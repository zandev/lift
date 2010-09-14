package net.liftweb.http {
package config {
  
  import net.liftweb.common.{Logger,Box,Full,Empty}
  import net.liftweb.util.{FatLazy,Helpers}
  import net.liftweb.util.Helpers._
  import net.liftweb.http._
  import net.liftweb.http.js.{JsCmd,JsCmds,JsExp,ScriptRenderer}
  import net.liftweb.http.js.JE.{Str,JsRaw} 
  
  trait Comet { _: HTTP with Factory => 
    
    object Comet {
      
      type CometCreationPF = PartialFunction[CometCreationInfo, LiftCometActor]
      
      /**
       * A function that takes appropriate action in breaking out of any
       * existing comet requests based on the request, browser type, etc.
       */
      @volatile var makeCometBreakoutDecision: (LiftSession, Req) => Unit =
      (session, req) => {
        // get the open sessions to the host (this means that any DNS wildcarded
        // Comet requests will not be counted
        val which = session.cometForHost(req.hostAndPath)

        // get the maximum requests given the browser type
        val max = HTTP.maxConcurrentRequests.vend(req) - 2 // this request and any open comet requests

        // dump the oldest requests
        which.drop(max).foreach {
          case (actor, req) => actor ! BreakOut
        }
      }
      
      private val _cometLogger: FatLazy[Logger] = FatLazy({
        val ret = Logger("comet_trace")
        ret
      })

      /**
       * Tells Lift if the Comet JavaScript shoukd be included. By default it is set to true.
       */
      @volatile var autoIncludeComet: LiftSession => Boolean = session => true
      
      
      /**
       * Calculate the Comet Server (by default, the server that
       * the request was made on, but can do the multi-server thing
       * as well)
       */
      @volatile var cometServer: () => String = () => S.contextPath
      
      /**
       * Holds the CometLogger that will be used to log comet activity
       */
      def cometLogger: Logger = _cometLogger.get

      /**
       * Holds the CometLogger that will be used to log comet activity
       */
      def cometLogger_=(newLogger: Logger): Unit = _cometLogger.set(newLogger)
      
      
      /**
       * Where to send the user if there's no comet session
       */
      @volatile var noCometSessionPage = "/"
      
      /**
       * If you want the AJAX request timeout to be something other than 120 seconds, put the value here
       */
      @volatile var cometRequestTimeout: Box[Int] = Empty

      /**
       * If a Comet request fails timeout for this period of time. Default value is 10 seconds
       */
      @volatile var cometFailureRetryTimeout: Long = 10 seconds
      
      /**
      * Partial function to allow you to build a CometActor from code rather than via reflection
      */
      val cometCreation = RulesSeq[CometCreationPF]

      private def noComet(ignore: CometCreationInfo): Box[LiftCometActor] = Empty

      /**
      * A factory that will vend comet creators
      */
      val cometCreationFactory: FactoryMaker[CometCreationInfo => Box[LiftCometActor]] =
        new FactoryMaker(() => noComet _) {}
      
      /**
       * How many times do we retry an Ajax command before calling it a failure?
       */
      @volatile var ajaxRetryCount: Box[Int] = Empty

      /**
       * The JavaScript to execute at the begining of an
       * Ajax request (for example, showing the spinning working thingy)
       */
      @volatile var ajaxStart: Box[() => JsCmd] = Empty
    
    
      /**
       * The JavaScript to execute at the end of an
       * Ajax request (for example, removing the spinning working thingy)
       */
      @volatile var ajaxEnd: Box[() => JsCmd] = Empty
    
      /**
       * The default action to take when the JavaScript action fails
       */
      @volatile var ajaxDefaultFailure: Box[() => JsCmd] =
        Full(() => JsCmds.Alert(S.??("ajax.error")))
      
      
      @volatile var cometGetTimeout = 140000
      
      /**
       * Returns the JavaScript that manages Comet requests.
       */
      @volatile var renderCometScript: LiftSession => JsCmd = session => ScriptRenderer.cometScript

      /**
       * Renders that JavaScript that holds Comet identification information
       */
      @volatile var renderCometPageContents: (LiftSession, Seq[CometVersionPair]) => JsCmd =
      (session, vp) => JsCmds.Run(
        "var lift_toWatch = " + vp.map(p => p.guid.encJs + ": " + p.version).mkString("{", " , ", "}") + ";"
        )
      
      /**
       * Holds the last update time of the Comet request. Based on this server mayreturn HTTP 304 status
       * indicating the client to used the cached information.
       */
      @volatile var cometScriptUpdateTime: LiftSession => Long = session => {
        object when extends SessionVar[Long](millis)
        when.is
      }
      
      /**
       * The name of the Comet script that manages Comet rewuests.
       */
      @volatile var cometScriptName: () => String = () => "cometAjax.js"
      
      /**
       * Returns the Comet script as a JavaScript response
       */
      @volatile var serveCometScript: (LiftSession, Req) => Box[LiftResponse] =
      (liftSession, requestState) => {
        val modTime = cometScriptUpdateTime(liftSession)

        requestState.testFor304(modTime) or
                Full(JavaScriptResponse(renderCometScript(liftSession),
                  List("Last-Modified" -> toInternetDate(modTime),
                       "Expires" -> toInternetDate(modTime + 10.minutes),
                       "Date" -> Helpers.nowAsInternetDate,
                       "Pragma" -> "",
                       "Cache-Control" -> ""),
                  Nil, 200))
      }
      
      /**
       * Contains the Comet URI path used by Lift to process Comet requests.
       */
      @volatile var cometPath = "comet_request"

      /**
       * Computes the Comet path by adding additional tokens on top of cometPath
       */
      @volatile var calcCometPath: String => JsExp = prefix => {
        Str(prefix + "/" + cometPath + "/") +
                JsRaw("Math.floor(Math.random() * 100000000000)") +
                Str(S.session.map(s => S.encodeURL("/" + s.uniqueId)) openOr "")
      }
      
      
      
    }
  }
  
}}