package net.liftweb.json.xschema {

import _root_.net.liftweb.json.JsonAST._
import _root_.net.liftweb.json.JsonParser.{parse => j}

class BootstrapXSchema {
  def apply: XRoot = XRoot(
    List(
      XProduct(
        "XRoot", "net.liftweb.json.xschema",
        Map(),
        List(
          XRealField("definitions", Map(), XList(XDefinitionRef("XDefinition", "net.liftweb.json.xschema")), j("""[]"""), XOrderAscending),
          XRealField("constants",   Map(), XList(XDefinitionRef("XConstant", "net.liftweb.json.xschema")), j("""[]"""), XOrderAscending),
          XRealField("properties",  Map(), XMap(XString, XString), j("""[]"""), XOrderAscending)
        )
      ),
      XCoproduct(
        "XSchema", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XDefinition", "net.liftweb.json.xschema"),
          XDefinitionRef("XReference",  "net.liftweb.json.xschema"),
          XDefinitionRef("XField",      "net.liftweb.json.xschema"),
          XDefinitionRef("XConstant",   "net.liftweb.json.xschema")
        ),
        j("""{ "XString": {} } """)
      ),
      XCoproduct(
        "XReference", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XPrimitiveRef",  "net.liftweb.json.xschema"),
          XDefinitionRef("XContainerRef",  "net.liftweb.json.xschema"),
          XDefinitionRef("XDefinitionRef", "net.liftweb.json.xschema")
        ),
        j("""{ "XString": {} } """)
      ),
      XCoproduct(
        "XPrimitiveRef", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XBoolean", "net.liftweb.json.xschema"),
          XDefinitionRef("XInt",     "net.liftweb.json.xschema"),
          XDefinitionRef("XLong",    "net.liftweb.json.xschema"),
          XDefinitionRef("XFloat",   "net.liftweb.json.xschema"),
          XDefinitionRef("XDouble",  "net.liftweb.json.xschema"),
          XDefinitionRef("XString",  "net.liftweb.json.xschema"),
          XDefinitionRef("XJSON",    "net.liftweb.json.xschema"),
          XDefinitionRef("XDate",    "net.liftweb.json.xschema")
        ),
        j("""{ "XString": {} } """)
      ),
      XCoproduct(
        "XContainerRef", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XCollection", "net.liftweb.json.xschema"),
          XDefinitionRef("XMap",        "net.liftweb.json.xschema"),
          XDefinitionRef("XOptional",   "net.liftweb.json.xschema"),
          XDefinitionRef("XTuple",      "net.liftweb.json.xschema")
        ),
        j(""" { "XList": { "elementType": { "XString": {} } } } """)
      ),
      XProduct(
        "XDefinitionRef", "net.liftweb.json.xschema",
        Map(),
        List(
          XRealField("name", Map(), XString, JString(""), XOrderAscending),
          XRealField("namespace", Map(), XString, JString(""), XOrderAscending)
        )
      ),
      XProduct("XBoolean", "net.liftweb.json.xschema", Map(), List()),
      XProduct("XInt",     "net.liftweb.json.xschema", Map(), List()),
      XProduct("XLong",    "net.liftweb.json.xschema", Map(), List()),
      XProduct("XFloat",   "net.liftweb.json.xschema", Map(), List()),
      XProduct("XDouble",  "net.liftweb.json.xschema", Map(), List()),
      XProduct("XString",  "net.liftweb.json.xschema", Map(), List()),
      XProduct("XJSON",    "net.liftweb.json.xschema", Map(), List()),
      XProduct("XDate",    "net.liftweb.json.xschema", Map(), List()),
      XCoproduct(
        "XCollection", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XList", "net.liftweb.json.xschema"),
          XDefinitionRef("XSet", "net.liftweb.json.xschema"),
          XDefinitionRef("XArray", "net.liftweb.json.xschema")
        ),
        j(""" { "XList": { "elementType": { "XString": {} } } } """)
      ),
      XProduct("XList", "net.liftweb.json.xschema", Map(), 
        List(XRealField("elementType", Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XDefinitionRef": { "name": "", "namespace": "" } }  """), XOrderAscending))
      ),
      XProduct("XSet", "net.liftweb.json.xschema", Map(), 
        List(XRealField("elementType", Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XDefinitionRef": { "name": "", "namespace": "" } }  """), XOrderAscending))
      ),
      XProduct("XArray", "net.liftweb.json.xschema", Map(), 
        List(XRealField("elementType", Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XDefinitionRef": { "name": "", "namespace": "" } }  """), XOrderAscending))
      ),
      XProduct("XMap", "net.liftweb.json.xschema", Map(), 
        List(
          XRealField("keyType", Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XDefinitionRef": { "name": "", "namespace": "" } }  """), XOrderAscending),
          XRealField("valueType", Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XDefinitionRef": { "name": "", "namespace": "" } }  """), XOrderAscending)
        )
      ),
      XProduct("XOptional", "net.liftweb.json.xschema", Map(), 
        List(XRealField("optionalType", Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XDefinitionRef": { "name": "", "namespace": "" } }  """), XOrderAscending))
      ),
      XProduct("XTuple", "net.liftweb.json.xschema", Map(), 
        List(XRealField("types", Map(), XList(XDefinitionRef("XReference", "net.liftweb.json.xschema")), j("""[]"""), XOrderAscending))
      ),
      XCoproduct(
        "XDefinition", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XProduct", "net.liftweb.json.xschema"),
          XDefinitionRef("XMultitype", "net.liftweb.json.xschema")
        ),
        j(""" { "XProduct": {} } """)
      ),
      XCoproduct(
        "XMultitype", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XCoproduct", "net.liftweb.json.xschema"),
          XDefinitionRef("XUnion", "net.liftweb.json.xschema")
        ),
        j(""" { "XCoproduct": {} } """)
      ),
      XCoproduct(
        "XField", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XRealField", "net.liftweb.json.xschema"),
          XDefinitionRef("XViewField", "net.liftweb.json.xschema"),
          XDefinitionRef("XConstantField", "net.liftweb.json.xschema")
        ),
        j(""" { "XRealField": {} } """)
      ),
      XProduct(
        "XProduct", "net.liftweb.json.xschema",
        Map(
          "scala.class.traits" -> "net.liftweb.json.xschema.XProductBehavior",
          "xschema.doc" -> """A product is analogous to a record: it contains fields, which may be
                              any type, have default values, and have a user-defined ordering.
                              Products are the fundamental building blocks used to construct most 
                              data structures."""
        ),
        List(
          XRealField("name",        Map(), XString, JString(""), XOrderAscending),
          XRealField("namespace",   Map(), XString, JString(""), XOrderAscending),
          XRealField("properties",  Map(), XMap(XString, XString), j("""[]"""), XOrderAscending),
          XRealField("terms",       Map(), XList(XDefinitionRef("XField", "net.liftweb.json.xschema")), j("""[]"""), XOrderAscending),
          
          XViewField("referenceTo", Map(), XDefinitionRef("XDefinitionRef", "net.liftweb.json.xschema"))
        )
      ),
      XProduct(
        "XCoproduct", "net.liftweb.json.xschema",
        Map(
          "xschema.doc" -> """A coproduct is a data structure that can assume one of N other types. 
                              These types must be either products, or other coproducts -- primitives
                              are not allowed because they cannot be mapped cleanly to most languages
                              (see unions for a disjoint structure that allows primitives). <p>
                              Note that most languages cannot handle coproducts of unions.
                              """
        ),
        List(
          XRealField("name",        Map(), XString, JString(""), XOrderAscending),
          XRealField("namespace",   Map(), XString, JString(""), XOrderAscending),
          XRealField("properties",  Map(), XMap(XString, XString), j("""[]"""), XOrderAscending),
          XRealField("terms",       Map(), XList(XDefinitionRef("XDefinitionRef", "net.liftweb.json.xschema")), j("""[]"""), XOrderAscending),
          XRealField("default",     Map(), XJSON, JNothing, XOrderAscending),
          
          XViewField("referenceTo", Map(), XDefinitionRef("XDefinitionRef", "net.liftweb.json.xschema"))
        )
      ),
      XProduct(
        "XUnion", "net.liftweb.json.xschema",
        Map(
          "xschema.doc" -> """A union is a C-style union of N types -- referred to as terms. Unlike 
                              coproducts, unions have no effect on the type hierarchy of the specified 
                              terms, and the terms may include primitive types, in addition to references
                              to products, coproducts, and other unions. Although unions have names and 
                              namespaces, most languages do not have explicit support for union types, 
                              and in such cases, no entity will be generated for them; they will be 
                              translated into the supertype of all the terms. <p>Some code generators 
                              may not be able to handle unions or coproducts that contain unions."""
        ),
        List(
          XRealField("name",        Map(), XString, JString(""), XOrderAscending),
          XRealField("namespace",   Map(), XString, JString(""), XOrderAscending),
          XRealField("properties",  Map(), XMap(XString, XString), j("""[]"""), XOrderAscending),
          XRealField("terms",       Map(), XList(XDefinitionRef("XReference", "net.liftweb.json.xschema")), j("""[]"""), XOrderAscending),
          XRealField("default",     Map(), XJSON, JNothing, XOrderAscending),
          
          XViewField("referenceTo", Map(), XDefinitionRef("XDefinitionRef", "net.liftweb.json.xschema"))
        )
      ),
      XProduct(
        "XConstant", "net.liftweb.json.xschema",
        Map(),
        List(
          XRealField("name",         Map(), XString, JString(""), XOrderAscending),
          XRealField("namespace",    Map(), XString, JString(""), XOrderAscending),
          XRealField("properties",   Map(), XMap(XString, XString), j("""[]"""), XOrderAscending),
          XRealField("constantType", Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XString": {} }  """), XOrderAscending),
          XRealField("default",      Map(), XJSON, JString(""), XOrderAscending),
          
          XViewField("referenceTo",  Map(), XDefinitionRef("XDefinitionRef", "net.liftweb.json.xschema"))
        )
      ),
      XProduct(
        "XRealField", "net.liftweb.json.xschema",
        Map(),
        List(
          XRealField("name",       Map(), XString, JString(""), XOrderAscending),
          XRealField("properties", Map(), XMap(XString, XString), JArray(Nil), XOrderAscending),
          XRealField("fieldType",  Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XString": {} }  """), XOrderAscending),
          XRealField("default",    Map(), XJSON, JString(""), XOrderAscending),
          XRealField("order",      Map(), XDefinitionRef("XOrder", "net.liftweb.json.xschema"), j(""" { "XOrderAscending": {} } """), XOrderAscending)
        )
      ),
      XProduct(
        "XViewField", "net.liftweb.json.xschema",
        Map(),
        List(
          XRealField("name",       Map(), XString, JString(""), XOrderAscending),
          XRealField("properties", Map(), XMap(XString, XString), j("""[]"""), XOrderAscending),
          XRealField("fieldType",  Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XDefinitionRef": { "name": "", "namespace": "" } }  """), XOrderAscending)
        )
      ),
      XProduct(
        "XConstantField", "net.liftweb.json.xschema",
        Map(),
        List(
          XRealField("name",       Map(), XString, JString(""), XOrderAscending),
          XRealField("properties", Map(), XMap(XString, XString), j("""[]"""), XOrderAscending),
          XRealField("fieldType",  Map(), XDefinitionRef("XReference", "net.liftweb.json.xschema"), j(""" { "XString": {} }  """), XOrderAscending),
          XRealField("default",    Map(), XJSON, JString(""), XOrderAscending)
        )
      ),
      XCoproduct(
        "XOrder", "net.liftweb.json.xschema",
        Map(),
        List(
          XDefinitionRef("XOrderAscending",  "net.liftweb.json.xschema"),
          XDefinitionRef("XOrderDescending", "net.liftweb.json.xschema"),
          XDefinitionRef("XOrderIgnore",     "net.liftweb.json.xschema")
        ),
        j(""" { "XOrderAscending": {} } """)
      ),
      XProduct("XOrderAscending",  "net.liftweb.json.xschema", Map(), List()),
      XProduct("XOrderDescending", "net.liftweb.json.xschema", Map(), List()),
      XProduct("XOrderIgnore",     "net.liftweb.json.xschema", Map(), List())
    ),
    Nil,
    Map(
    )
  )
}

object SampleSchemas {
  val DataSocialGenderSchema = XRoot(
    List(
      XCoproduct(
        "Gender", "data.social",
        Map(
          "xschema.doc" -> "This is the coproduct that includes male and female. The normal way to translate this into OOP is as a superclass/superinterface.",
          "scala.class.traits" -> "java.io.Serializable, java.lang.Cloneable",
          "scala.object.traits" -> "java.io.Serializable, java.lang.Cloneable"
        ),
        List(
          XDefinitionRef("Male", "data.social"),
          XDefinitionRef("Female", "data.social")
        ),
        j(""" { "Male": { "text": "foo" } } """)
      ),
      XProduct(
        "Male", "data.social",
        Map("scala.class.traits" -> "java.io.Serializable, java.lang.Cloneable"),
        List(
          XRealField("text", Map(), XString, JString("male"), XOrderDescending),
          XViewField("asFemale", Map(), XDefinitionRef("Female", "data.social"))
        )
      ),
      XProduct(
        "Female", "data.social",
        Map("scala.class.traits" -> "java.io.Serializable, java.lang.Cloneable"),
        List(
          XRealField("text", Map(), XString, JString("female"), XOrderAscending),
          XViewField("asMale", Map(), XDefinitionRef("Male", "data.social"))
        )
      ),
      XProduct(
        "Morning", "data.social",
        Map(),
        List()
      ),
      XProduct(
        "Noon", "data.social",
        Map(),
        List()
      ),
      XProduct(
        "Night", "data.social",
        Map(),
        List()
      ),
      XCoproduct(
        "Time", "data.social",
        Map(),
        List(
          XDefinitionRef("Morning", "data.social"),
          XDefinitionRef("Noon", "data.social"),
          XDefinitionRef("Night", "data.social")
        ),
        j(""" { "Morning": {} } """)
      )
    ),
    List(
      XConstant(
        "DefaultFemale", "data.social",
        Map(),
        XDefinitionRef("Gender", "data.social"),
        JObject(
          JField("Female",
            JObject(
              JField("text", JString("female")) :: Nil
            )
          ) :: Nil
        )
      ),
      XConstant(
        "DefaultMale", "data.social",
        Map(),
        XDefinitionRef("Gender", "data.social"),
        JObject(
          JField("Male",
            JObject(
              JField("text", JString("male")) :: Nil
            )
          ) :: Nil
        )
      )
    ),
    Map(
      "scala.imports" -> "net.liftweb.json.xschema.{SerializationImplicits => XSerializationImplicits, DefaultExtractors => XDefaultExtractors}, java.lang.reflect._"
    )
  )
  
  val EmployeeSchema = XRoot(
    List(
      XCoproduct(
        "Employee", "data.employee",
        Map(),
        List(
          XDefinitionRef("Manager", "data.employee"),
          XDefinitionRef("Secretary", "data.employee"),
          XDefinitionRef("Coach", "data.employee")
        ),
        j(""" { "Manager": {} } """)
      ),
      XCoproduct(
        "ID", "data.employee",
        Map(),
        List(
          XDefinitionRef("EmployeeID", "data.employee"),
          XDefinitionRef("NoID", "data.employee")
        ),
        j(""" { "NoID": {} } """)
      ),
      XCoproduct(
        "EmployeeID", "data.employee",
        Map(),
        List(
          XDefinitionRef("SSN", "data.employee"),
          XDefinitionRef("Passport", "data.employee"),
          XDefinitionRef("DL", "data.employee")
        ),
        j(""" { "SSN": {} } """)
      ),
      XProduct(
        "NoID", "data.employee",
        Map(),
        List()
      ),
      XProduct(
        "SSN", "data.employee",
        Map(),
        List(
          XRealField("value", Map(), XString, JString(""), XOrderAscending)
        )
      ),
      XProduct(
        "Passport", "data.employee",
        Map(),
        List(
          XRealField("value", Map(), XLong, JInt(-1), XOrderAscending)
        )
      ),
      XProduct(
        "DL", "data.employee",
        Map(),
        List(
          XRealField("value", Map(), XString, JString(""), XOrderAscending)
        )
      ),
      XProduct(
        "Manager", "data.employee",
        Map(),
        List(
          XRealField("id", Map(), XDefinitionRef("SSN", "data.employee"), j(""" { "SSN": {} } """), XOrderAscending)
        )
      ),
      XProduct(
        "Secretary", "data.employee",
        Map(),
        List(
          XRealField("id", Map(), XDefinitionRef("SSN", "data.employee"), j(""" { "SSN": {} } """), XOrderAscending)
        )
      ),
      XProduct(
        "Coach", "data.employee",
        Map(),
        List(
          XRealField("id", Map(), XDefinitionRef("SSN", "data.employee"), j(""" { "SSN": {} } """), XOrderAscending)
        )
      )
    ),
    Nil,
    Map(
      "scala.imports" -> "net.liftweb.json.xschema.{SerializationImplicits => XSerializationImplicits, DefaultExtractors => XDefaultExtractors}, java.lang.reflect._"
    )
  )
  
  val FringeFeaturesSchema = XRoot(
    XProduct(
      "ConstantSingleton", "data.fringe",
      Map(),
      List(
        XConstantField("constantNumber", Map(), XInt,     JInt(123)),
        XConstantField("constantString", Map(), XString,  JString("foo")),
        XConstantField("constantBool",   Map(), XBoolean, JString("foo")),
        XConstantField("constantDate",   Map(), XDate,    JInt(275296173))
      )
    ) :: 
    XProduct(
      "ConstantWithRealField", "data.fringe",
      Map(),
      List(
        XRealField("value", Map(), XLong, JInt(-1), XOrderAscending),
        XConstantField("constantNumber", Map(), XInt,     JInt(123)),
        XConstantField("constantString", Map(), XString,  JString("foo")),
        XConstantField("constantBool",   Map(), XBoolean, JString("foo")),
        XConstantField("constantDate",   Map(), XDate,    JInt(275296173))
      )
    ) ::
    XUnion(
      "UnionOfStringAndProduct", "data.fringe",
      Map(),
      List(
        XString,
        XDefinitionRef("ConstantWithRealField", "data.fringe")
      ),
      j(""" { "String": {} } """)
    ) ::
    XProduct(
      "ProductWithUnionField", "data.fringe",
      Map(),
      List(
        XRealField("value", Map(), XDefinitionRef("UnionOfStringAndProduct", "data.fringe"), j(""" { "String": "" } """), XOrderAscending)
      )
    ) :: Nil,
    XConstant(
      "ConstantBool", "data.fringe",
      Map(),
      XBoolean,
      JBool(true)
    ) :: Nil,
    Map()
  )
  
  val XSchemaSchema = (new BootstrapXSchema).apply
}
}












// This code was auto-generated by Lift Json XSchema - do not edit
package data.fringe {
  import net.liftweb.json.JsonParser._
  import net.liftweb.json.JsonAST._
  import net.liftweb.json.xschema.{SerializationImplicits, Extractor, Decomposer}
  import net.liftweb.json.xschema.{XRoot, XProduct, XCoproduct}
  import net.liftweb.json.xschema.DefaultOrderings._
  
  
  trait Orderings {
    implicit val ConstantSingletonOrdering: Ordering[data.fringe.ConstantSingleton.type] = new Ordering[data.fringe.ConstantSingleton.type] {
      def compare(v1: data.fringe.ConstantSingleton.type, v2: data.fringe.ConstantSingleton.type): Int = {
        import Stream.{cons, empty}
        
        return if (v1 == v2) 0 else {      
          val comparisons = empty
          
          comparisons.dropWhile(_ == 0).append(0 :: Nil).first
        }
      }
    }
    
    implicit val ConstantWithRealFieldOrdering: Ordering[data.fringe.ConstantWithRealField] = new Ordering[data.fringe.ConstantWithRealField] {
      def compare(v1: data.fringe.ConstantWithRealField, v2: data.fringe.ConstantWithRealField): Int = {
        import Stream.{cons, empty}
        
        return if (v1 == v2) 0 else {      
          val comparisons = cons(net.liftweb.json.xschema.DefaultOrderings.LongOrdering.compare(v1.value, v2.value) * 1, empty)
          
          comparisons.dropWhile(_ == 0).append(0 :: Nil).first
        }
      }
    }
    
    val UnionOfStringAndProductOrdering: Ordering[Any] = new Ordering[Any] {
      def compare(v1: Any, v2: Any): Int = {
        if (v1 == v2) 0
        else v1 match {
          case x: String => v2 match {
            case y: String => net.liftweb.json.xschema.DefaultOrderings.StringOrdering.compare(x, y)
            case y: data.fringe.ConstantWithRealField => -1
          }
          case x: data.fringe.ConstantWithRealField => v2 match {
            case y: String => 1
            case y: data.fringe.ConstantWithRealField => data.fringe.Orderings.ConstantWithRealFieldOrdering.compare(x, y)
          }
        }
      }
    }
    
    implicit val ProductWithUnionFieldOrdering: Ordering[data.fringe.ProductWithUnionField] = new Ordering[data.fringe.ProductWithUnionField] {
      def compare(v1: data.fringe.ProductWithUnionField, v2: data.fringe.ProductWithUnionField): Int = {
        import Stream.{cons, empty}
        
        return if (v1 == v2) 0 else {      
          val comparisons = cons(data.fringe.Orderings.UnionOfStringAndProductOrdering.compare(v1.value, v2.value) * 1, empty)
          
          comparisons.dropWhile(_ == 0).append(0 :: Nil).first
        }
      }
    }
  }
  object Orderings extends Orderings
  
  case object ConstantSingleton {
    lazy val xschema: XProduct = net.liftweb.json.xschema.Extractors.XDefinitionExtractor.extract(JObject(JField("XProduct",JObject(JField("name",JString("ConstantSingleton"))::JField("namespace",JString("data.fringe"))::JField("properties",JArray(Nil))::JField("terms",JArray(JObject(JField("XConstantField",JObject(JField("name",JString("constantNumber"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XInt",JObject(Nil))::Nil))::JField("default",JInt(123))::Nil))::Nil)::JObject(JField("XConstantField",JObject(JField("name",JString("constantString"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XString",JObject(Nil))::Nil))::JField("default",JString("foo"))::Nil))::Nil)::JObject(JField("XConstantField",JObject(JField("name",JString("constantBool"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XBoolean",JObject(Nil))::Nil))::JField("default",JString("foo"))::Nil))::Nil)::JObject(JField("XConstantField",JObject(JField("name",JString("constantDate"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XDate",JObject(Nil))::Nil))::JField("default",JInt(275296173))::Nil))::Nil)::Nil))::Nil))::Nil)).asInstanceOf[XProduct]
    lazy val constantNumber: Int = net.liftweb.json.xschema.DefaultExtractors.IntExtractor.extract(JInt(123))
    lazy val constantString: String = net.liftweb.json.xschema.DefaultExtractors.StringExtractor.extract(JString("foo"))
    lazy val constantBool: Boolean = net.liftweb.json.xschema.DefaultExtractors.BooleanExtractor.extract(JString("foo"))
    lazy val constantDate: java.util.Date = net.liftweb.json.xschema.DefaultExtractors.DateExtractor.extract(JInt(275296173))
  }
  
  case class ConstantWithRealField(value: Long) extends Ordered[data.fringe.ConstantWithRealField] {
    def compare(that: data.fringe.ConstantWithRealField): Int = data.fringe.Orderings.ConstantWithRealFieldOrdering.compare(this, that)
    
  }
  object ConstantWithRealField {
    lazy val xschema: XProduct = net.liftweb.json.xschema.Extractors.XDefinitionExtractor.extract(JObject(JField("XProduct",JObject(JField("name",JString("ConstantWithRealField"))::JField("namespace",JString("data.fringe"))::JField("properties",JArray(Nil))::JField("terms",JArray(JObject(JField("XRealField",JObject(JField("name",JString("value"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XLong",JObject(Nil))::Nil))::JField("default",JInt(-1))::JField("order",JObject(JField("XOrderAscending",JObject(Nil))::Nil))::Nil))::Nil)::JObject(JField("XConstantField",JObject(JField("name",JString("constantNumber"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XInt",JObject(Nil))::Nil))::JField("default",JInt(123))::Nil))::Nil)::JObject(JField("XConstantField",JObject(JField("name",JString("constantString"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XString",JObject(Nil))::Nil))::JField("default",JString("foo"))::Nil))::Nil)::JObject(JField("XConstantField",JObject(JField("name",JString("constantBool"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XBoolean",JObject(Nil))::Nil))::JField("default",JString("foo"))::Nil))::Nil)::JObject(JField("XConstantField",JObject(JField("name",JString("constantDate"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XDate",JObject(Nil))::Nil))::JField("default",JInt(275296173))::Nil))::Nil)::Nil))::Nil))::Nil)).asInstanceOf[XProduct]
  }
  
  case class ProductWithUnionField(value: Any) extends Ordered[data.fringe.ProductWithUnionField] {
    def compare(that: data.fringe.ProductWithUnionField): Int = data.fringe.Orderings.ProductWithUnionFieldOrdering.compare(this, that)
    
  }
  object ProductWithUnionField {
    lazy val xschema: XProduct = net.liftweb.json.xschema.Extractors.XDefinitionExtractor.extract(JObject(JField("XProduct",JObject(JField("name",JString("ProductWithUnionField"))::JField("namespace",JString("data.fringe"))::JField("properties",JArray(Nil))::JField("terms",JArray(JObject(JField("XRealField",JObject(JField("name",JString("value"))::JField("properties",JArray(Nil))::JField("fieldType",JObject(JField("XDefinitionRef",JObject(JField("name",JString("UnionOfStringAndProduct"))::JField("namespace",JString("data.fringe"))::Nil))::Nil))::JField("default",JObject(JField("String",JString(""))::Nil))::JField("order",JObject(JField("XOrderAscending",JObject(Nil))::Nil))::Nil))::Nil)::Nil))::Nil))::Nil)).asInstanceOf[XProduct]
  }
  
  trait Extractors {
    protected def extractField[T](jvalue: JValue, name: String, default: JValue, e: Extractor[T]): T = {
      try {
        e.extract((jvalue \ name -->? classOf[JField]).map(_.value).getOrElse(default))
      }
      catch {
        case _ => e.extract(default)
      }
    }
    
    implicit val ConstantSingletonExtractor: Extractor[data.fringe.ConstantSingleton.type] = new Extractor[data.fringe.ConstantSingleton.type] {
      def extract(jvalue: JValue): data.fringe.ConstantSingleton.type = {
        ConstantSingleton
      }
    }
    
    implicit val ConstantWithRealFieldExtractor: Extractor[data.fringe.ConstantWithRealField] = new Extractor[data.fringe.ConstantWithRealField] {
      def extract(jvalue: JValue): data.fringe.ConstantWithRealField = {
        ConstantWithRealField(
          extractField[Long](jvalue, "value", JInt(-1), net.liftweb.json.xschema.DefaultExtractors.LongExtractor)
        )
      }
    }
    
    lazy val UnionOfStringAndProductExtractorFunction: PartialFunction[JField, Any] = ({
      case JField("String", value) => net.liftweb.json.xschema.DefaultExtractors.StringExtractor.extract(value)
      case JField("ConstantWithRealField", value) => data.fringe.Extractors.ConstantWithRealFieldExtractor.extract(value)
    }: PartialFunction[JField, Any])
    val UnionOfStringAndProductExtractor: Extractor[Any] = new Extractor[Any] {
      def extract(jvalue: JValue): Any = {
        def extract0(jvalue: JValue): Option[Any] = {
          (jvalue --> classOf[JObject]).obj.filter(UnionOfStringAndProductExtractorFunction.isDefinedAt _) match {
            case field :: fields => Some(UnionOfStringAndProductExtractorFunction(field))
            case Nil => None
          }
        }
        
        extract0(jvalue) match {
          case Some(v) => v
          case None => extract0(JObject(JField("String",JObject(Nil))::Nil)) match {
            case Some(v) => v
            case None => error("Expected to find Any, but found " + jvalue + ", and default value was invalid")
          }
        }
      }
    }
    
    implicit val ProductWithUnionFieldExtractor: Extractor[data.fringe.ProductWithUnionField] = new Extractor[data.fringe.ProductWithUnionField] {
      def extract(jvalue: JValue): data.fringe.ProductWithUnionField = {
        ProductWithUnionField(
          extractField[Any](jvalue, "value", JObject(JField("String",JString(""))::Nil), data.fringe.Extractors.UnionOfStringAndProductExtractor)
        )
      }
    }
  }
  object Extractors extends Extractors
  
  trait Decomposers {
    implicit val ConstantSingletonDecomposer: Decomposer[data.fringe.ConstantSingleton.type] = new Decomposer[data.fringe.ConstantSingleton.type] {
      def decompose(tvalue: data.fringe.ConstantSingleton.type): JValue = {
        JObject(
           Nil
        )
      }
    }
    
    implicit val ConstantWithRealFieldDecomposer: Decomposer[data.fringe.ConstantWithRealField] = new Decomposer[data.fringe.ConstantWithRealField] {
      def decompose(tvalue: data.fringe.ConstantWithRealField): JValue = {
        JObject(
          JField("value", net.liftweb.json.xschema.DefaultDecomposers.LongDecomposer.decompose(tvalue.value)) :: Nil
        )
      }
    }
    
    val UnionOfStringAndProductDecomposer: Decomposer[Any] = new Decomposer[Any] {
      def decompose(tvalue: Any): JValue = {
        tvalue match {
          case x: String => JObject(JField("String", net.liftweb.json.xschema.DefaultDecomposers.StringDecomposer.decompose(x)) :: Nil)
          case x: data.fringe.ConstantWithRealField => JObject(JField("ConstantWithRealField", data.fringe.Decomposers.ConstantWithRealFieldDecomposer.decompose(x)) :: Nil)
        }
      }
    }
    
    implicit val ProductWithUnionFieldDecomposer: Decomposer[data.fringe.ProductWithUnionField] = new Decomposer[data.fringe.ProductWithUnionField] {
      def decompose(tvalue: data.fringe.ProductWithUnionField): JValue = {
        JObject(
          JField("value", data.fringe.Decomposers.UnionOfStringAndProductDecomposer.decompose(tvalue.value)) :: Nil
        )
      }
    }
  }
  object Decomposers extends Decomposers
  
  object Serialization extends Decomposers with Extractors with SerializationImplicits {
    lazy val xschema: XRoot = net.liftweb.json.xschema.Extractors.XRootExtractor.extract(parse("""{"definitions":[{"XProduct":{"name":"ConstantSingleton","namespace":"data.fringe","properties":[],"terms":[{"XConstantField":{"name":"constantNumber","properties":[],"fieldType":{"XInt":{}},"default":123}},{"XConstantField":{"name":"constantString","properties":[],"fieldType":{"XString":{}},"default":"foo"}},{"XConstantField":{"name":"constantBool","properties":[],"fieldType":{"XBoolean":{}},"default":"foo"}},{"XConstantField":{"name":"constantDate","properties":[],"fieldType":{"XDate":{}},"default":275296173}}]}},{"XProduct":{"name":"ConstantWithRealField","namespace":"data.fringe","properties":[],"terms":[{"XRealField":{"name":"value","properties":[],"fieldType":{"XLong":{}},"default":-1,"order":{"XOrderAscending":{}}}},{"XConstantField":{"name":"constantNumber","properties":[],"fieldType":{"XInt":{}},"default":123}},{"XConstantField":{"name":"constantString","properties":[],"fieldType":{"XString":{}},"default":"foo"}},{"XConstantField":{"name":"constantBool","properties":[],"fieldType":{"XBoolean":{}},"default":"foo"}},{"XConstantField":{"name":"constantDate","properties":[],"fieldType":{"XDate":{}},"default":275296173}}]}},{"XUnion":{"name":"UnionOfStringAndProduct","namespace":"data.fringe","properties":[],"terms":[{"XString":{}},{"XDefinitionRef":{"name":"ConstantWithRealField","namespace":"data.fringe"}}],"default":{"String":{}}}},{"XProduct":{"name":"ProductWithUnionField","namespace":"data.fringe","properties":[],"terms":[{"XRealField":{"name":"value","properties":[],"fieldType":{"XDefinitionRef":{"name":"UnionOfStringAndProduct","namespace":"data.fringe"}},"default":{"String":""},"order":{"XOrderAscending":{}}}}]}}],"constants":[{"name":"ConstantBool","namespace":"data.fringe","properties":[],"constantType":{"XBoolean":{}},"default":true}],"properties":[]} """))
  }
  
  object Constants {
    import Serialization._
    
    lazy val ConstantBool: Boolean = net.liftweb.json.xschema.DefaultExtractors.BooleanExtractor.extract(JBool(true))
  }
}// These tests were auto-generated by Lift Json XSchema - do not edit
package data.fringe {
  import _root_.org.specs.Specification
  import _root_.org.specs.runner.{Runner, JUnit}
  
  import net.liftweb.json.JsonParser._
  import net.liftweb.json.JsonAST._
  
  import net.liftweb.json.xschema.DefaultSerialization._
  
  import data.fringe.Serialization._
  import data.fringe.Constants._

  import data.fringe.{ConstantSingleton, ConstantWithRealField, ProductWithUnionField}
  
  object ExampleProductData {
    lazy val ExampleConstantSingleton: data.fringe.ConstantSingleton.type = data.fringe.Extractors.ConstantSingletonExtractor.extract(JObject(Nil))
    
    lazy val ExampleConstantWithRealField: data.fringe.ConstantWithRealField = JObject(Nil).deserialize[data.fringe.ConstantWithRealField]
    
    lazy val ExampleProductWithUnionField: data.fringe.ProductWithUnionField = JObject(Nil).deserialize[data.fringe.ProductWithUnionField]
  }
  class DataProductSerializationTest extends Runner(DataProductSerializationExamples) with JUnit
  object DataProductSerializationExamples extends Specification {
    "Deserialization of ConstantSingleton succeeds even when information is missing" in {
      ExampleProductData.ExampleConstantSingleton.isInstanceOf[data.fringe.ConstantSingleton.type] must be (true)
    }
    "Serialization of ConstantSingleton has non-zero information content" in {
      Decomposers.ConstantSingletonDecomposer.decompose(ExampleProductData.ExampleConstantSingleton) mustNot be (JObject(Nil))
    }
    
    "Deserialization of ConstantWithRealField succeeds even when information is missing" in {
      ExampleProductData.ExampleConstantWithRealField.isInstanceOf[data.fringe.ConstantWithRealField] must be (true)
    }
    "Serialization of ConstantWithRealField has non-zero information content" in {
      ExampleProductData.ExampleConstantWithRealField.serialize mustNot be (JObject(Nil))
    }
  
    
    "Deserialization of ProductWithUnionField succeeds even when information is missing" in {
      ExampleProductData.ExampleProductWithUnionField.isInstanceOf[data.fringe.ProductWithUnionField] must be (true)
    }
    "Serialization of ProductWithUnionField has non-zero information content" in {
      ExampleProductData.ExampleProductWithUnionField.serialize mustNot be (JObject(Nil))
    }
  
  }
  object ExampleMultitypeData {
    
  }
  class DataCoproductSerializationTest extends Runner(DataCoproductSerializationExamples) with JUnit
  object DataCoproductSerializationExamples extends Specification {
    
  }
  class DataConstantsSerializationTest extends Runner(DataConstantsSerializationExamples) with JUnit
  object DataConstantsSerializationExamples extends Specification {
    "Deserialization of constant ConstantBool succeeds" in {
      Constants.ConstantBool.serialize.deserialize[Boolean] must be (Constants.ConstantBool)
    }
    "Serialization of constant ConstantBool has non-zero information content" in {
      Constants.ConstantBool.serialize mustNot be (JObject(Nil))
    }
  
  }
}
