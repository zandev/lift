package net.liftweb {
package http {
  
  import net.liftweb.common.{Full,Box,Empty,Failure}
  import net.liftweb.util.Helpers._
  import net.liftweb.http.provider.HTTPSession
  
  trait SessionComponent { _: Factory with CometComponent =>
    object Session {
      
      /**
       * A function that takes the HTTPSession and the contextPath as parameters
       * and returns a LiftSession reference. This can be used in cases subclassing
       * LiftSession is necessary.
       */
      @volatile var sessionCreator: (HTTPSession, String) => LiftSession = {
        case (httpSession, contextPath) => new LiftSession(contextPath, httpSession.sessionId, Full(httpSession))
      }

      //val sessionCreator = new FactoryMaker[(HTTPSession, String) => LiftSession](){}

      val enableContainerSessions = new FactoryMaker[Boolean](() => true){}

      @volatile var getLiftSession: (Req) => LiftSession = (req) => _getLiftSession(req)
      
      
      /**
       * Returns a LiftSession instance.
       */
      private def _getLiftSession(req: Req): LiftSession = {
        val wp = req.path.wholePath
        val cometSessionId =
        if (wp.length >= 3 && wp.head == Comet.cometPath)
          Full(wp(2))
        else
          Empty

        val ret = SessionMaster.getSession(req.request, cometSessionId) match {
          case Full(ret) =>
            ret.fixSessionTime()
            ret

          case _ =>
            val ret = LiftSession(req.request.session, req.request.contextPath)
            ret.fixSessionTime()
            SessionMaster.addSession(ret, req.request.userAgent, SessionMaster.getIpFromReq(req))
            ret
        }

        Comet.makeCometBreakoutDecision(ret, req)
        ret
      }
      
      
      /**
       * Implementation for snippetNamesToSearch that looks first in a package named by taking the current template path.
       * For example, suppose the following is configured in Boot:
       *   Application.snippetNamesToSearch.default.set(() => Application.searchSnippetsWithRequestPath)
       *   Application.addToPackages("com.mycompany.myapp")
       *   Application.addToPackages("com.mycompany.mylib")
       * The tag <lift:MySnippet> in template foo/bar/baz.html would search for the snippet in the following locations:
       *   - com.mycompany.myapp.snippet.foo.bar.MySnippet
       *   - com.mycompany.myapp.snippet.MySnippet
       *   - com.mycompany.mylib.snippet.foo.bar.MySnippet
       *   - com.mycompany.mylib.snippet.MySnippet
       *   - and then the Lift builtin snippet packages
       */
      def searchSnippetsWithRequestPath(name: String): List[String] =
        S.request.map(_.path.partPath.dropRight(1)) match {
          case Full(xs) if !xs.isEmpty => (xs.mkString(".") + "." + name) :: name :: Nil
          case _ => name :: Nil
        }
      
      
      
          /**
           * This variable controls whether RequestVars that have been set but not subsequently
           * read will be logged in Dev mode. Logging can be disabled at the per-RequestVar level
           * via RequestVar.logUnreadVal
           *
           * @see RequestVar#logUnreadVal
           */
          @volatile var logUnreadRequestVars = true


          /**
           * The polling interval for background Ajax requests to keep functions to not be garbage collected.
           * This will be applied if the Ajax request will fail. Default value is set to 15 seconds.
           */
          @volatile var liftGCFailureRetryTimeout: Long = 15 seconds

          /**
           * By default lift uses a garbage-collection mechanism of removing unused bound functions from LiftSesssion.
           * Setting this to false will disable this mechanims and there will be no Ajax polling requests attempted.
           */
          @volatile var enableLiftGC = true;

          /**
           * If Lift garbage collection is enabled, functions that are not seen in the page for this period of time
           * (given in milliseonds) will be discarded, hence eligibe for garbage collection.
           * The default value is 10 minutes.
           */
          @volatile var unusedFunctionsLifeTime: Long = 10 minutes

          /**
           * The polling interval for background Ajax requests to prevent functions of being garbage collected.
           * Default value is set to 75 seconds.
           */
          @volatile var liftGCPollingInterval: Long = 75 seconds
      
          /**
           * Set to false if you do not want Ajax/Comet requests that are not associated with a session
           * to cause a page reload
           */
          @volatile var redirectAjaxOnSessionLoss = true


    }
  }
  
}}

