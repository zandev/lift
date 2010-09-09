package net.liftweb {
package http {
  
  import java.io.{BufferedReader,StringReader}
  import scala.xml.{NodeSeq,Elem,MetaData,Null,Node,Unparsed,UnprefixedAttribute}
  import net.liftweb.common.{Box,Full,Empty,Failure,LazyLoggable}
  import net.liftweb.util.{NamedPF,CSSHelpers,Props,Helpers}
  import net.liftweb.util.Helpers._
  import net.liftweb.http.js.{JsCmd,JSArtifacts}
  import net.liftweb.http.js.JsCmds
  import net.liftweb.http.js.jquery.{JQuery13Artifacts,JQuery14Artifacts}
  
  trait PresentationComponent { 
    _: EnvironmentComponent with HTTPComponent with LazyLoggable with Factory with FormVendor =>
    
    object Presentation {
      
      type SnippetPF = PartialFunction[List[String], NodeSeq => NodeSeq]
      type LiftTagPF = PartialFunction[(String, Elem, MetaData, NodeSeq, String), NodeSeq]
      
      /**
       * Default notice for notices / errors / warnings created by S.notice etc
       */
      val noticesContainerId = "lift__noticesContainer__"
      
      /**
       * Holds the user's snippet functions that will be executed by lift given a certain path.
       */
      val snippets = RulesSeq[SnippetPF]
      
      /**
       * Set a snippet failure handler here.  The class and method for the snippet are passed in
       */
      val snippetFailedFunc = RulesSeq[SnippetFailure => Unit].prepend(logSnippetFailure _)
      
      /**
       * Allows user adding additional Lift tags (the tags must be prefixed by lift namespace such as <lift:xxxx/>).
       * Each LiftTagPF function will be called with the folowing parameters:
       * <pre>
       *  - Element label,
       *  - The Element itselft,
       *  - The attrbutes
       *  - The child nodes
       *  - The page name
       * </pre>
       */
      val liftTagProcessing = RulesSeq[LiftTagPF]
      
      /**
       * Define a XHTML validator if you want your template markup to be automatically validated
       * using one of the W3C validators. If you have markup errors, you will be alerted in the 
       * the browser upon loading the offending page.
       */
      @volatile var xhtmlValidator: Box[XHtmlValidator] = Empty // Full(TransitionalXHTML1_0Validator)
      
      @volatile var calcIE6ForResponse: () => Boolean = () => S.request.map(_.isIE6) openOr false
      
      @volatile var flipDocTypeForIE6 = true
      
      private[http] def snippet(name: String): Box[DispatchSnippet] = NamedPF.applyBox(name, HTTP.snippetDispatch.toList)
      
      
      /**
       * Modifies the root relative paths from the css url-s
       *
       * @param path - the path of the css resource
       * @prefix - the prefix to be added on the root relative paths. If this is Empty
       * 	       the prefix will be the application context path.
       */
      def fixCSS(path: List[String], prefix: Box[String]) {

        val liftReq: HTTP.LiftRequestPF = new HTTP.LiftRequestPF {
          def functionName = "Default CSS Fixer"

          def isDefinedAt(r: Req): Boolean = {
            r.path.partPath == path
          }

          def apply(r: Req): Boolean = {
            r.path.partPath == path
          }
        }

        val cssFixer: HTTP.DispatchPF = new HTTP.DispatchPF {
          def functionName = "default css fixer"
          def isDefinedAt(r: Req): Boolean = {
            r.path.partPath == path
          }
          def apply(r: Req): () => Box[LiftResponse] = {
            val cssPath = path.mkString("/", "/", ".css")
            val css = Environment.loadResourceAsString(cssPath);

            () => {
              css.map(str => CSSHelpers.fixCSS(new BufferedReader(
                new StringReader(str)), prefix openOr (S.contextPath)) match {
                case (Full(c), _) => CSSResponse(c)
                case (_, input) => {
                  logger.info("Fixing " + cssPath + " failed");
                  CSSResponse(input)
                }
              })
            }
          }
        }
        HTTP.dispatch.prepend(cssFixer)
        HTTP.liftRequest.append(liftReq)
      }
      
      
      /**
       * Holds the falure information when a snippet can not be executed.
       */
      case class SnippetFailure(page: String, typeName: Box[String], failure: SnippetFailures.Value)

      object SnippetFailures extends Enumeration {
        val NoTypeDefined = Value(1, "No Type Defined")
        val ClassNotFound = Value(2, "Class Not Found")
        val StatefulDispatchNotMatched = Value(3, "Stateful Snippet: Dispatch Not Matched")
        val MethodNotFound = Value(4, "Method Not Found")
        val NoNameSpecified = Value(5, "No Snippet Name Specified")
        val InstantiationException = Value(6, "Exception During Snippet Instantiation")
        val DispatchSnippetNotMatched = Value(7, "Dispatch Snippet: Dispatch Not Matched")
      }
      
      /**
       * Set the default fadeout mechanism for Lift notices. Thus you provide a function that take a NoticeType.Value
       * and decide the duration after which the fade out will start and the actual fadeout time. This is applicable
       * for general notices (not associated with id-s) regardless if they are set for the page rendering, ajax
       * response or Comet response.
       */
      val noticesAutoFadeOut = new FactoryMaker[(NoticeType.Value) => Box[(TimeSpan, TimeSpan)]]((notice : NoticeType.Value) => Empty){}

      /**
       * Use this to apply various effects to the notices. The user function receives the NoticeType
       * and the id of the element containing the specific notice. Thus it is the function's responsability to form
       * the javascript code for the visual effects. This is applicable for both ajax and non ajax contexts.
       * For notices associated with ID's the user type will receive an Empty notice type. That's because the effect
       * is applied on the real estate holding the notices for this ID. Typically this contains a single message.
       */
      val noticesEffects = new FactoryMaker[(Box[NoticeType.Value], String) => Box[JsCmd]]((notice: Box[NoticeType.Value], id: String) => Empty){}
      
      /**
       * Holds the JS library specific UI artifacts. By efault it uses JQuery's artifacts
       */
      @volatile var jsArtifacts: JSArtifacts = JQuery13Artifacts
      
      protected val pageResourceId = Helpers.nextFuncName

      /**
       * Attached an ID entity for resource URI specified in
       * link or script tags. This allows controlling browser
       * resource caching. By default this just adds a query string
       * parameter unique per application lifetime. More complex
       * implementation could user per resource MD5 sequences thus
       * "forcing" browsers to refresh the resource only when the resource
       * file changes. Users can define other rules as well. Inside user's
       * function it is safe to use S context as attachResourceId is called
       * from inside the &lt;lift:with-resource-id> snippet
       *
       */
      val attachResourceId = new FactoryMaker[String => String]((name: String) => {
        name + (if (name contains ("?")) "&" else "?") + pageResourceId + "=_"
      }){}

      /**
       * The path to handle served resources
       */
      @volatile var resourceServerPath = "classpath"

      /**
       * The base name of the resource bundle
       */
      @volatile var resourceNames: List[String] = List("lift")
      
      /**
       * Should codes that represent entities be converted to XML
       * entities when rendered?
       */
      val convertToEntity: FactoryMaker[Boolean] = new FactoryMaker(false) {}
      
      @volatile var noticesToJsCmd: () => JsCmd = () => {
        import builtin.snippet._

        def noticesFadeOut(noticeType: NoticeType.Value, id: String): JsCmd =
          (noticesAutoFadeOut()(noticeType) map {
            case (duration, fadeTime) => jsArtifacts.fadeOut(id, duration, fadeTime)
          }) openOr JsCmds.Noop

        def effects(noticeType: Box[NoticeType.Value], id: String): JsCmd =
          noticesEffects()(noticeType, id) match {
            case Full(jsCmd) => jsCmd
            case _ => JsCmds.Noop
          }

        val func: (() => List[NodeSeq], String, MetaData) => NodeSeq = (f, title, attr) => f() map (e => <li>{e}</li>) match {
          case Nil => Nil
          case list => <div>{title}<ul>{list}</ul> </div> % attr
        }

        val f = if (ShowAll.get)
          S.messages _
        else
          S.noIdMessages _

        def makeList(meta: Box[AjaxMessageMeta], notices: List[NodeSeq], title: String, id: String):
          List[(Box[AjaxMessageMeta], List[NodeSeq], String, String)] =
            if (notices.isEmpty) Nil else List((meta, notices, title, id))

        val xml =
          ((makeList(MsgsErrorMeta.get, f(S.errors), S.??("msg.error"), noticesContainerId + "_error")) ++
           (makeList(MsgsWarningMeta.get, f(S.warnings), S.??("msg.warning"), noticesContainerId + "_warn")) ++
           (makeList(MsgsNoticeMeta.get, f(S.notices), S.??("msg.notice"), noticesContainerId + "_notice"))) flatMap {
             msg => msg._1 match {
               case Full(meta) => <div id={msg._4}>{func(msg._2 _, meta.title openOr "",
                 meta.cssClass.map(new UnprefixedAttribute("class",_, Null)) openOr Null)}</div>
               case _ => <div id={msg._4}>{func(msg._2 _, msg._3, Null)}</div>
            }
          }

        val groupMessages = xml match {
          case Nil => JsCmds.Noop
          case _ => jsArtifacts.setHtml(noticesContainerId, xml) &
            noticesFadeOut(NoticeType.Notice, noticesContainerId + "_notice") &
            noticesFadeOut(NoticeType.Warning, noticesContainerId + "_warn") &
            noticesFadeOut(NoticeType.Error, noticesContainerId + "_error") &
            effects(Full(NoticeType.Notice), noticesContainerId + "_notice") &
            effects(Full(NoticeType.Warning), noticesContainerId + "_warn") &
            effects(Full(NoticeType.Error), noticesContainerId + "_error")
        }

        val g = S.idMessages _
        List((MsgErrorMeta.get, g(S.errors)),
          (MsgWarningMeta.get, g(S.warnings)),
          (MsgNoticeMeta.get, g(S.notices))).foldLeft(groupMessages)((car, cdr) => cdr match {
          case (meta, m) => m.foldLeft(car)((left, r) =>
                  left & jsArtifacts.setHtml(r._1, <span>{r._2 flatMap (node => node)}</span> %
                          (Box(meta.get(r._1)).map(new UnprefixedAttribute("class", _, Null)) openOr Null)) & effects(Empty, r._1))
        })
      }
      
      /**
       * Function that generates variants on snippet names to search for, given the name from the template.
       * The default implementation just returns name :: Nil (e.g. no change).
       * The names are searched in order.
       * See also searchSnippetsWithRequestPath for an implementation.
       */
      val snippetNamesToSearch: FactoryMaker[String => List[String]] = 
        new FactoryMaker(() => (name: String) => name :: Nil) {}
      
      /**
       * The function that calculates if the response should be rendered in
       * IE6/7 compatibility mode
       */
      @volatile var calcIEMode: () => Boolean = {
      () => (for (r <- S.request) yield r.isIE6 || r.isIE7 ||
              r.isIE8) openOr true
      }
      
      /**
       * An XML header is inserted at the very beginning of returned XHTML pages.
       * This function defines the cases in which such a header is inserted.  The
       * function takes a NodeResponse (the LiftResponse that is converting the
       * XML to a stream of bytes), the Node (root node of the XML), and
       * a Box containing the content type.
       */
      @volatile var calculateXmlHeader: (NodeResponse, Node, Box[String]) => String = {
        case _ if S.skipXmlHeader => ""
        case (_, up: Unparsed, _) => ""

        case (_, _, Empty) | (_, _, Failure(_, _, _)) =>
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"

        case (_, _, Full(s)) if (s.toLowerCase.startsWith("text/html")) =>
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"

        case (_, _, Full(s)) if (s.toLowerCase.startsWith("text/xml") ||
            s.toLowerCase.startsWith("text/xhtml") ||
            s.toLowerCase.startsWith("application/xml") ||
            s.toLowerCase.startsWith("application/xhtml+xml")) =>
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"

        case _ => ""
      }
      
      /**
       * Should comments be stripped from the served XHTML
       */
      val stripComments: FactoryMaker[Boolean] =
      new FactoryMaker(() => {
            if (Props.devMode)
            false
            else true
          }) {}
      
      
          /**
           * How long should we wait for all the lazy snippets to render
           */
          val lazySnippetTimeout: FactoryMaker[TimeSpan] = new FactoryMaker(() => 30 seconds) {}

          /**
           * Does the current context support parallel snippet execution
           */
          val allowParallelSnippets: FactoryMaker[Boolean] = new FactoryMaker(() => false) {}
      
          /**
           * If a deferred snippet has a failure during render,
           * what should we display?
           */
          val deferredSnippetFailure: FactoryMaker[Failure => NodeSeq] =
          new FactoryMaker(() => {
            failure: Failure => {
              if (Props.devMode)
                <div style="border: red solid 2px">A lift:parallel snippet failed to render.Message:{failure.msg}{failure.exception match {
                  case Full(e) =>
                    <pre>{e.getStackTrace.map(_.toString).mkString("\n")}</pre>
                  case _ => NodeSeq.Empty
                }}<i>note: this error is displayed in the browser because
                your application is running in "development" mode.If you
                set the system property run.mode=production, this error will not
                be displayed, but there will be errors in the output logs.
                </i>
                </div>
              else NodeSeq.Empty
            }
          }){}
      
          /**
           * If a deferred snippet has a failure during render,
           * what should we display?
           */
          val deferredSnippetTimeout: FactoryMaker[NodeSeq] =
          new FactoryMaker(() => {
                if (Props.devMode)
                <div style="border: red solid 2px">
                  A deferred snippet timed out during render.

                  <i>note: this error is displayed in the browser because
                    your application is running in "development" mode.  If you
                    set the system property run.mode=production, this error will not
                    be displayed, but there will be errors in the output logs.
                  </i>
                </div>
                else NodeSeq.Empty
              }) {}
      
      
              private var otherPackages: List[String] = Nil

              /**
               * Used by Lift to construct full pacakge names fromthe packages provided to addToPackages function
               */
              def buildPackage(end: String) = synchronized(otherPackages.map(_ + "." + end))

              /**
               * Tells Lift where to find Snippets,Views, Comet Actors and Lift ORM Model object
               */
              def addToPackages(what: String) {
                synchronized {otherPackages = what :: otherPackages}
              }

              /**
               * Tells Lift where to find Snippets,Views, Comet Actors and Lift ORM Model object
               */
              def addToPackages(what: Package) {
                synchronized {otherPackages = what.getName :: otherPackages}
              }
      
              /**
               * If you use the form attribute in a snippet invocation, what attributes should
               * be copied from the snippet invocation tag to the form tag.  The
               * default list is "class", "id", "target", "style", "onsubmit"
               */
              val formAttrs: FactoryMaker[List[String]] = new FactoryMaker(() => List("class", "id", "target", "style", "onsubmit")) {}
      
      
              private def logSnippetFailure(sf: SnippetFailure) = logger.info("Snippet Failure: " + sf)

      
      
      
    }
  }
  
}}

