package net.liftweb.http {
package config {
  
  import java.io.{InputStream,ByteArrayOutputStream}
  import java.net.URL
  import java.util.{Date,Locale}
  import scala.xml.{NodeSeq,Text}
  import net.liftweb.common.{Box,Full,Empty,Logger,LazyLoggable}
  import net.liftweb.util.{Props,TemplateCache,PCDataXmlParser}
  import net.liftweb.util.Helpers._
  import net.liftweb.http._
  
  trait Environment { _: HTTP with Factory with LazyLoggable => 
    
    object Environment {
      
      type ExceptionHandlerPF = PartialFunction[(Props.RunModes.Value, Req, Throwable), LiftResponse]
      
      lazy val liftVersion: String = {
        val cn = """\.""".r.replaceAllIn(Application.getClass.getName, "/")
        val ret: Box[String] =
        for{
          url <- Box !! Application.getClass.getResource("/" + cn + ".class")
          val newUrl = new URL(url.toExternalForm.split("!")(0) + "!" + "/META-INF/MANIFEST.MF")
          str <- tryo(new String(readWholeStream(newUrl.openConnection.getInputStream), "UTF-8"))
          ma <- """lift_version: (.*)""".r.findFirstMatchIn(str)
        } yield ma.group(1)
        ret openOr "Unknown Lift Version"
      }
      
      lazy val liftBuildDate: Date = {
        val cn = """\.""".r.replaceAllIn(Application.getClass.getName, "/")
        val ret: Box[Date] =
        for{
          url <- Box !! Application.getClass.getResource("/" + cn + ".class")
          val newUrl = new URL(url.toExternalForm.split("!")(0) + "!" + "/META-INF/MANIFEST.MF")
          str <- tryo(new String(readWholeStream(newUrl.openConnection.getInputStream), "UTF-8"))
          ma <- """Bnd-LastModified: (.*)""".r.findFirstMatchIn(str)
          asLong <- asLong(ma.group(1))
        } yield new Date(asLong)
        ret openOr new Date(0L)
      }
      
      @volatile var templateCache: Box[TemplateCache[(Locale, List[String]), NodeSeq]] = Empty
      
      /**
       * A utility method to convert an exception to a string of stack traces
       * @param le the exception
       *
       * @return the stack trace
       */
      private def showException(le: Throwable): String = {
        val ret = "Message: " + le.toString + "\n\t" +
                le.getStackTrace.map(_.toString).mkString("\n\t") + "\n"
        val also = le.getCause match {
          case null => ""
          case sub: Throwable => "\nCaught and thrown by:\n" + showException(sub)
        }
        ret + also
      }
      
      private[config] def runAsSafe[T](f: => T): T = synchronized {
         val old = doneBoot
         try {
            doneBoot = false
            f
         } finally {
            doneBoot = old
         }
      }
      
      
      @volatile private[http] var ending = false
      
      @volatile private[http] var doneBoot = false;
      
      private var _configureLogging: () => Unit = _
      
      /**
       * Holds the function that configures logging. Must be set before any loggers are created
       */
      def configureLogging: () => Unit = _configureLogging
      
      /**
       * Holds the function that configures logging. Must be set before any loggers are created
       */
      def configureLogging_=(newConfigurer: () => Unit): Unit = {
        _configureLogging = newConfigurer
        Logger.setup = Full(newConfigurer)
      }
      //TODO: Move to somewhere else, this is not good.
      configureLogging = net.liftweb.util.LoggingAutoConfigurer()
      
      
      private val defaultFinder = getClass.getResource _
      
      private[http] def resourceFinder(name: String): URL = HTTP._context.resource(name)
      
      /**
       * Obtain the resource URL by name
       */
      var getResource: String => Box[URL] = defaultGetResource _
      
      /**
       * Obtain the resource URL by name
       */
      def defaultGetResource(name: String): Box[URL] =
        for{
          rf <- (Box !! resourceFinder(name)) or (Box !! defaultFinder(name))
        } yield rf
      
      /**
       * Open a resource by name and process its contents using the supplied function.
       */
      def doWithResource[T](name: String)(f: InputStream => T): Box[T] =
        getResource(name) map { _.openStream } map { is => try { f(is) } finally { is.close } }
      
      /**
       * Obtain the resource as an array of bytes by name
       */
      def loadResource(name: String): Box[Array[Byte]] = doWithResource(name) { stream =>
        val buffer = new Array[Byte](2048)
        val out = new ByteArrayOutputStream
        def reader {
          val len = stream.read(buffer)
          if (len < 0) return
          else if (len > 0) out.write(buffer, 0, len)
          reader
        }
        reader
        out.toByteArray
      }
      
      /**
       * Obtain the resource as an XML by name. If you're using this to load a template, consider using
       * the TemplateFinder object instead.
       *
       * @see TemplateFinder
       */
      def loadResourceAsXml(name: String): Box[NodeSeq] = loadResourceAsString(name).flatMap(s => PCDataXmlParser(s))
      
      /**
       * Obtain the resource as a String by name
       */
      def loadResourceAsString(name: String): Box[String] = loadResource(name).map(s => new String(s, "UTF-8"))
      
      /**
       * The sequence of partial functions (pattern matching) for handling converting an exception to something to
       * be sent to the browser depending on the current RunMode (development, etc.)
       *
       * By default it returns an XhtmlResponse containing a predefined markup. You can overwrite this by calling
       * LiftRules.exceptionHandler.prepend(...). If you are calling append then your code will not be calle since
       * a default implementation is already appended.
       *
       */
      @volatile var exceptionHandler = RulesSeq[ExceptionHandlerPF].append {
        case (Props.RunModes.Development, r, e) =>
          logger.error("Exception being returned to browser when processing " + r.uri.toString + ": " + showException(e))
          XhtmlResponse((<html> <body>Exception occured while processing {r.uri}<pre>{showException(e)}</pre> </body> </html>), HTTP.docType.vend(r), List("Content-Type" -> "text/html; charset=utf-8"), Nil, 500, S.ieMode)
        case (_, r, e) =>
          logger.error("Exception being returned to browser when processing " + r, e)
          XhtmlResponse((<html> <body>Something unexpected happened while serving the page at {r.uri}</body> </html>), HTTP.docType.vend(r), List("Content-Type" -> "text/html; charset=utf-8"), Nil, 500, S.ieMode)
      }
      
      
    }
  }
  
}}

