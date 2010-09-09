package net.liftweb {
package http {
  
  import net.liftweb.common.{Box,Empty,Full}
  import net.liftweb.util.Helpers._
  import net.liftweb.http.js.{JsCmd,ScriptRenderer}
  
  trait AJAXComponent {
    object AJAX {
      
      /**
       * Contains the Ajax URI path used by Lift to process Ajax requests.
       */
      @volatile var ajaxPath = "ajax_request"
      
      /**
       * The name of the Ajax script that manages Ajax rewuests.
       */
      @volatile var ajaxScriptName: () => String = () => "liftAjax.js"
      
      @volatile var ajaxPostTimeout = 5000
      
      /**
       * Tells Lift if the Ajax JavaScript shoukd be included. By default it is set to true.
       */
      @volatile var autoIncludeAjax: LiftSession => Boolean = session => true
      
      /**
       * Returns the JavaScript that manages Ajax requests.
       */
      @volatile var renderAjaxScript: LiftSession => JsCmd = session => ScriptRenderer.ajaxScript
      
      /**
       * Holds the last update time of the Ajax request. Based on this server mayreturn HTTP 304 status
       * indicating the client to used the cached information.
       */
      @volatile var ajaxScriptUpdateTime: LiftSession => Long = session => {
        object when extends SessionVar[Long](millis)
        when.is
      }
      
      /**
       * Returns the Ajax script as a JavaScript response
       */
      @volatile var serveAjaxScript: (LiftSession, Req) => Box[LiftResponse] =
      (liftSession, requestState) => {
        val modTime = ajaxScriptUpdateTime(liftSession)
        requestState.testFor304(modTime) or
          Full(JavaScriptResponse(renderAjaxScript(liftSession),
            List(
              "Last-Modified" -> toInternetDate(modTime),
              "Expires" -> toInternetDate(modTime + 10.minutes)),
              Nil, 200))
      }
      
      
    }
  }
  
}}