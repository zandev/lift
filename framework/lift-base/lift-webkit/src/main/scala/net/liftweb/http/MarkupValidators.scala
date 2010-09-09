package net.liftweb {
package http {
  
  import scala.xml.Node
  import net.liftweb.common.{Loggable,Full,Empty,Failure}
  import net.liftweb.util.Helpers._
  
  case class XHTMLValidationError(msg: String, line: Int, col: Int)
  
  trait XHtmlValidator extends Function1[Node, List[XHTMLValidationError]]
  
  object StrictXHTML1_0Validator extends GenericValidtor {
    val ngurl = "http://www.w3.org/2002/08/xhtml/xhtml1-strict.xsd"
  }
  
  abstract class GenericValidtor extends XHtmlValidator with Loggable {
    import javax.xml.validation._
    import javax.xml._
    import XMLConstants._
    import java.net.URL
    import javax.xml.transform.dom._
    import javax.xml.transform.stream._
    import java.io.ByteArrayInputStream
    
    private lazy val sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI)
    
    protected def ngurl: String
    
    private lazy val schema = tryo(sf.newSchema(new URL(ngurl)))
    
    def apply(in: Node): List[XHTMLValidationError] = {
      (for{
        sc <- schema
        v <- tryo(sc.newValidator)
        source = new StreamSource(new ByteArrayInputStream(in.toString.getBytes("UTF-8")))
      } yield try {
          v.validate(source)
          Nil
        } catch {
          case e: org.xml.sax.SAXParseException =>
            List(XHTMLValidationError(e.getMessage, e.getLineNumber, e.getColumnNumber))
        }) match {
        case Full(x) => x
        case Failure(msg, _, _) =>
          logger.info("XHTML Validation Failure: " + msg)
          Nil
        case _ => Nil
      }
    }
  }
  
  object TransitionalXHTML1_0Validator extends GenericValidtor {
    val ngurl = "http://www.w3.org/2002/08/xhtml/xhtml1-transitional.xsd"
  }
  
}}
