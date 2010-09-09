package net.liftweb {
package http {
  
  import java.io.InputStream
  import java.util.{ResourceBundle,Locale}
  import java.util.concurrent.atomic.AtomicInteger
  import scala.xml.{NodeSeq,Node,Group}
  import net.liftweb.common.{Box,Full,Empty,Failure,LazyLoggable}
  import net.liftweb.util.{NamedPF,SafeNodeSeq}
  import net.liftweb.util.Helpers._
  import net.liftweb.http.auth.{Role,HttpAuthentication,NoAuthentication}
  import net.liftweb.http.provider.{HTTPRequest,HTTPResponse,HTTPContext,HTTPParam,HTTPCookie}
  
  trait HTTPComponent { _: EnvironmentComponent with Factory with LazyLoggable =>
    
    type DispatchPF = PartialFunction[Req, () => Box[LiftResponse]];
    type RewritePF = PartialFunction[RewriteRequest, RewriteResponse]
    type URINotFoundPF = PartialFunction[(Req, Box[Failure]), NotFound]
    type URLDecoratorPF = PartialFunction[String, String]
    type ViewDispatchPF = PartialFunction[List[String], Either[() => Box[NodeSeq], LiftView]]
    type HttpAuthProtectedResourcePF = PartialFunction[Req, Box[Role]]
    type SplitSuffixPF = PartialFunction[List[String], (List[String], String)]
    type SnippetDispatchPF = PartialFunction[String, DispatchSnippet]
    
    object HTTP {
      
      /**
       * A partial function that allows the application to define requests that should be
       * handled by lift rather than the default handler
       */
      type LiftRequestPF = PartialFunction[Req, Boolean]

      /**
       * Holds user functions that willbe executed very early in the request processing. The functions'
       * result will be ignored.
       */
      val early = RulesSeq[(HTTPRequest) => Any]

      /**
       * Holds user functions that are executed before sending the response to client. The functions'
       * result will be ignored.
       */
      val beforeSend = RulesSeq[(BasicResponse, HTTPResponse, List[(String, String)], Box[Req]) => Any]

      /**
       * The HTTP authentication mechanism that ift will perform. See <i>LiftRules.protectedResource</i>
       */
      val authentication = new FactoryMaker[HttpAuthentication](() => NoAuthentication){}

      /**
       * Defines the resources that are protected by authentication and authorization. If this function
       * is not defined for the input data, the resource is considered unprotected ergo no authentication
       * is performed. If this function is defined and returns a Full box, it means that this resource
       * is protected by authentication,and authenticated subjed must be assigned to the role returned by
       * this function or to a role that is child-of this role. If this function returns Empty it means that
       * this resource is protected by authentication but no authorization is performed meaning that roles are
       * not verified.
       */
      val protectedResources = RulesSeq[HttpAuthProtectedResourcePF]

      /**
       * Use this PartialFunction to to automatically add static URL parameters
       * to any URL reference from the markup of Ajax request.
       */
      val urlDecorate = RulesSeq[URLDecoratorPF]

      /**
       * Holds user functions that are executed after the response was sent to client. The functions' result
       * will be ignored.
       */
      val afterSend = RulesSeq[(BasicResponse, HTTPResponse, List[(String, String)], Box[Req]) => Any]
      
      /**
       * If you don't want lift to send the application/xhtml+xml mime type to those browsers
       * that understand it, then set this to  { @code false }
       */
      val useXhtmlMimeType = new FactoryMaker[Boolean](() => true){}
      
      /**
       * Runs responseTransformers
       */
      def performTransform(in: LiftResponse): LiftResponse = responseTransformers.toList.foldLeft(in) {
        case (in, pf: PartialFunction[_, _]) =>
          if (pf.isDefinedAt(in)) pf(in) else in
        case (in, f) => f(in)
      }
      
      /**
       * Holds the user's transformer functions allowing the user to modify a LiftResponse before sending it to client.
       */
      val responseTransformers = RulesSeq[LiftResponse => LiftResponse]
      
      /**
       * A partial function that determines content type based on an incoming
       * Req and Accept header
       */
      @volatile var determineContentType: PartialFunction[(Box[Req], Box[String]), String] = {
        case (_, Full(accept)) if useXhtmlMimeType.vend && accept.toLowerCase.contains("application/xhtml+xml") =>
          "application/xhtml+xml; charset=utf-8"
        case _ => "text/html; charset=utf-8"
      }
      
      /**
       * The list of partial function for defining the behavior of what happens when
       * URI is invalid and you're not using a site map
       *
       */
      val uriNotFound = RulesSeq[URINotFoundPF].prepend(NamedPF("default") {
        case (r, _) => DefaultNotFound
      })
      
      
      /**
       * Hooks to be run when LiftServlet.destroy is called.
       */
      val unloadHooks = RulesSeq[() => Unit]

      /**
       * For each unload hook registered, run them during destroy()
       */
      private[http] def runUnloadHooks() {
        unloadHooks.toList.foreach{f =>
          tryo{f()}
        }
      }

      /**
       * The maximum allowed size of a complete mime multi-part POST.  Default 8MB
       */
      @volatile var maxMimeSize: Long = 8 * 1024 * 1024

      /**
       * Should pages that are not found be passed along the request processing chain to the
       * next handler outside Lift?
       */
      @volatile var passNotFoundToChain = false

      /**
       * The maximum allowed size of a single file in a mime multi-part POST.
       * Default 7MB
       */
      @volatile var maxMimeFileSize: Long = 7 * 1024 * 1024
      
      private[http] def notFoundOrIgnore(requestState: Req, session: Box[LiftSession]): Box[LiftResponse] = {
        if (passNotFoundToChain) Empty
        else session match {
          case Full(session) => Full(session.checkRedirect(requestState.createNotFound))
          case _ => Full(requestState.createNotFound)
        }
      }
      
      /**
       * Put a function that will calculate the request timeout based on the
       * incoming request.
       */
      @volatile var calcRequestTimeout: Box[Req => Int] = Empty

      /**
       * If you want the standard (non-AJAX) request timeout to be something other than
       * 10 seconds, put the value here
       */
      @volatile var stdRequestTimeout: Box[Int] = Empty
      
      /**
       * The dispatcher that takes a Snippet and converts it to a
       * DispatchSnippet instance
       */
      val snippetDispatch = RulesSeq[SnippetDispatchPF]
      
      private def setupSnippetDispatch() {
        import net.liftweb.builtin.snippet._

        snippetDispatch.append(
          Map("CSS" -> CSS, "Msgs" -> Msgs, "Msg" -> Msg,
            "Menu" -> Menu, "css" -> CSS, "msgs" -> Msgs, "msg" -> Msg,
            "menu" -> Menu,
            "a" -> A, "children" -> Children,
            "comet" -> Comet, "form" -> Form, "ignore" -> Ignore, "loc" -> Loc,
            "surround" -> Surround,
            "test_cond" -> TestCond,
            "TestCond" -> TestCond,
            "embed" -> Embed,
            "tail" -> Tail,
            "with-param" -> WithParam,
            "bind-at" -> WithParam,
            "VersionInfo" -> VersionInfo,
            "version_info" -> VersionInfo,
            "SkipDocType" -> SkipDocType,
            "skip_doc_type" -> SkipDocType,
            "xml_group" -> XmlGroup,
            "XmlGroup" -> XmlGroup,
            "lazy-load" -> LazyLoad,
            "html5" -> HTML5,
            "HTML5" -> HTML5,
            "with-resource-id" -> WithResourceId
            ))
      }
      setupSnippetDispatch()
      
      /**
       * Change this variable to set view dispatching
       */
      val viewDispatch = RulesSeq[ViewDispatchPF]
      
      /**
       * If the request times out (or returns a non-Response) you can
       * intercept the response here and create your own response
       */
      @volatile  var requestTimedOut: Box[(Req, Any) => Box[LiftResponse]] = Empty
      
      /**
       * Update the function here that calculates particular paths to
       * exclused from context path rewriting
       */
      val excludePathFromContextPathRewriting: FactoryMaker[String => Boolean] =
        new FactoryMaker(() => ((s: String) => false)) {}
      
      import provider.servlet.containers.Jetty6AsyncProvider
      import provider.servlet.ServletAsyncProvider
      
      /**
       * Provides the async provider instance responsible for suspending/resuming requests
       */
      @volatile var servletAsyncProvider: (HTTPRequest) => ServletAsyncProvider = 
        req => new Jetty6AsyncProvider(req)
      
      /** 
       * Controls whether or not the service handling timing messages 
       * (Service request (GET) ... took ... Milliseconds) are logged. Defaults to true. 
       */
      @volatile var logServiceRequestTiming = true
      
      
      /**
       * Holds user's DispatchPF functions that will be executed in a stateless context. This means that
       * S object is not availble yet.
       */
      val statelessDispatchTable = RulesSeq[DispatchPF]
      
      
      private[http] def dispatchTable(req: HTTPRequest): List[DispatchPF] = {
        req match {
          case null => dispatch.toList
          case _ => SessionMaster.getSession(req, Empty) match {
            case Full(s) => S.initIfUninitted(s) {
              S.highLevelSessionDispatchList.map(_.dispatch) :::
              dispatch.toList
            }
            case _ => dispatch.toList
          }
        }
      }
      
      /**
       * If there is an alternative way of calculating the context path
       * (by default returning Empty)
       *
       * If this function returns an Empty, the contextPath provided by the container will be used.
       *
       */
      @volatile var calculateContextPath: () => Box[String] = () => Empty
      
      /**
       * The maximum concurrent requests.  If this number of
       * requests are being serviced for a given session, messages
       * will be sent to all Comet requests to terminate
       */
      val maxConcurrentRequests: FactoryMaker[Req => Int] = new FactoryMaker((x: Req) => x match {
        case r if r.isFirefox35_+ || r.isIE8 || r.isChrome3_+ || r.isOpera9 || r.isSafari3_+ => 6
        case _ => 2
      }){}
      
      @volatile private[http] var _context: HTTPContext = _
      
      /**
       * Returns the HTTPContext
       */
      def context: HTTPContext = synchronized {_context}
      
      /**
       * Sets the HTTPContext
       */
      def setContext(in: HTTPContext): Unit = synchronized {
        if (in ne _context) {
          _context = in
        }
      }
      
      
      /**
       * Get the partial function that defines if a request should be handled by
       * the application (rather than the default container handler)
       */
      val liftRequest = RulesSeq[LiftRequestPF]
      
      /**
       * Holds the user's DispatchPF functions that will be executed in stateful context
       */
      val dispatch = RulesSeq[DispatchPF]
      
      /**
       * Holds the user's rewrite functions that can alter the URI parts and query parameters.  This rewrite
       * is performed very early in the HTTP request cycle and may not include any state.  This rewrite is meant
       * to rewrite requests for statelessDispatch
       */
      val statelessRewrite = RulesSeq[RewritePF]
      
      /**
       * Use statelessRewrite or statefuleRewrite
       */
      @deprecated
      val rewrite = statelessRewrite
      
      /**
       *  Holds the user's rewrite functions that can alter the URI parts and query parameters.
       * This rewrite takes place within the scope of the S state so SessionVars and other session-related
       * information is available.
       */
      val statefulRewrite = RulesSeq[RewritePF]
      
      @volatile var defaultHeaders: PartialFunction[(NodeSeq, Req), List[(String, String)]] = {
        case _ =>
          val d = nowAsInternetDate
            List("Expires" -> d,
              "Date" -> d,
              "Cache-Control" ->
              "no-cache; private; no-store",
              "Pragma" -> "no-cache" /*,
              "Keep-Alive" -> "timeout=3, max=993" */ )
      }
      
      /**
        * By default, Http response headers are appended.  However, there are
        * some headers that should only appear once (for example "expires").  This
        * Vendor vends the list of header responses that can only appear once.
        */
      val overwrittenReponseHeaders: FactoryMaker[List[String]] = new FactoryMaker(() => List("expires")) {}
      
      /**
       * Holds user function hooks when the request is about to be processed
       */
      val onBeginServicing = RulesSeq[Req => Unit]
      
      val preAccessControlResponse_!! = new RulesSeq[Req => Box[LiftResponse]] with FirstBox[Req, LiftResponse]
      
      val earlyResponse = new RulesSeq[Req => Box[LiftResponse]] with FirstBox[Req, LiftResponse]
      
      /**
       * Holds user function hooks when the request was processed
       */
      val onEndServicing = RulesSeq[(Req, Box[LiftResponse]) => Unit]
      
      /**
       * When a request is parsed into a Req object, certain suffixes are explicitly split from
       * the last part of the request URI.  If the suffix is contained in this list, it is explicitly split.
       * The default list is: "html", "htm", "jpg", "png", "gif", "xml", "rss", "json" ...
       */
      @volatile var explicitlyParsedSuffixes: Set[String] = knownSuffixes
      
      /**
       * The global multipart progress listener:
       *    pBytesRead - The total number of bytes, which have been read so far.
       *    pContentLength - The total number of bytes, which are being read. May be -1, if this number is unknown.
       *    pItems - The number of the field, which is currently being read. (0 = no item so far, 1 = first item is being read, ...)
       */
      @volatile var progressListener: (Long, Long, Int) => Unit = (_, _, _) => ()
      
      @volatile var supplimentalHeaders: HTTPResponse => Unit = 
        s => s.addHeaders(List(HTTPParam("X-Lift-Version", Environment.liftVersion)))
      
      /**
       * convertResponse is a PartialFunction that reduces a given Tuple4 into a
       * LiftResponse that can then be sent to the browser.
       */
      var convertResponse: PartialFunction[(Any, List[(String, String)], List[HTTPCookie], Req), LiftResponse] = {
        case (r: LiftResponse, _, _, _) => r
        case (ns: Group, headers, cookies, req) => cvt(ns, headers, cookies, req, 200)
        case (ns: Node, headers, cookies, req) => cvt(ns, headers, cookies, req, 200)
        case (ns: NodeSeq, headers, cookies, req) => cvt(Group(ns), headers, cookies, req, 200)
        case ((ns: NodeSeq, code: Int), headers, cookies, req) => cvt(Group(ns), headers, cookies, req, code)
        case (SafeNodeSeq(n), headers, cookies, req) => cvt(Group(n), headers, cookies, req, 200)
        case (Full(o), headers, cookies, req) => convertResponse((o, headers, cookies, req))
        case (Some(o), headers, cookies, req) => convertResponse((o, headers, cookies, req))
        case (bad, _, _, req) => req.createNotFound
      }
      
      /**
       * The function that converts a fieldName, contentType, fileName and an InputStream into
       * a FileParamHolder.  By default, create an in-memory instance.  Use OnDiskFileParamHolder
       * to create an on-disk version
       */
      @volatile var handleMimeFile: (String, String, String, InputStream) => FileParamHolder =
        (fieldName, contentType, fileName, inputStream) =>
          new InMemFileParamHolder(fieldName, contentType, fileName, readWholeStream(inputStream))
          /**
           * Determins the path parts and suffix from given path parts
           */
          val suffixSplitters = RulesSeq[SplitSuffixPF].append {
            case parts =>
              val last = parts.last
              val idx: Int = {
                val firstDot = last.indexOf(".")
                val len = last.length
                if (firstDot + 1 == len) -1 // if the dot is the last character, don't split
                else {
                  if (last.indexOf(".", firstDot + 1) != -1) -1 // if there are multiple dots, don't split out
                  else {
                    val suffix = last.substring(firstDot + 1)
                    // if the suffix isn't in the list of suffixes we care about, don't split it
                    if (!LiftRules.explicitlyParsedSuffixes.contains(suffix.toLowerCase)) -1
                    else firstDot
                  }
                }
              }
            
            if (idx == -1) (parts, "")
            else (parts.dropRight(1) ::: List(last.substring(0, idx)), last.substring(idx + 1))
          }
      
      
      /**
       * Takes a Node, headers, cookies, and a session and turns it into an XhtmlResponse.
       */
      private def cvt(ns: Node, headers: List[(String, String)], cookies: List[HTTPCookie], req: Req, code:Int) =
        convertResponse({
          val ret = XhtmlResponse(ns,
          LiftRules.docType.vend(req),
          headers, cookies, code,
          S.ieMode)
          ret._includeXmlVersion = !S.skipDocType
          ret
        }, headers, cookies, req)
      
      private[http] val reqCnt = new AtomicInteger(0)
    
    }
  }
  
}}

