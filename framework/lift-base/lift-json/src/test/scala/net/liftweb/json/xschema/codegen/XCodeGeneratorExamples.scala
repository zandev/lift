package net.liftweb.json.xschema.codegen {
  import _root_.org.specs.Specification
  import _root_.org.specs.runner.{Runner, JUnit}

  import _root_.net.liftweb.json.JsonAST._
  import _root_.net.liftweb.json.JsonParser._
  import java.io._

  class XCodeGeneratorExamplesTest extends Runner(XCodeGeneratorExamples) with JUnit

  object XCodeGeneratorExamples extends Specification {
    import _root_.java.io._
    import _root_.net.liftweb.json.xschema.SampleSchemas._
    import _root_.net.liftweb.json.xschema.DefaultSerialization._
    import CodeGenerator._
    
    trait Uncloseable extends Closeable {
      override def close() = { }
    }
  
    "the xschema code generator" should {
      "generate the schema for XSchema without exceptions" >> {
        val out = using(new StringWriter) {
          sw => using(new PrintWriter(sw)) { out => 
            ScalaCodeGenerator.generator.generate(XSchemaSchema, "src/main/scala", "src/test/scala", Nil, _ => out)
            sw.toString
          }
        }

        out must not be equalTo("")
      }
    }
    
    "the xschema code generator" should {
      "generate the schema for FringeSchema without exceptions" >> {
        val sw = new StringWriter()
        
        val out = using(new PrintWriter(sw) with Uncloseable) { out => 
          ScalaCodeGenerator.generator.generate(FringeFeaturesSchema, "src/main/scala", "src/test/scala", Nil, _ => out)
          sw.toString
        }
        
        println(sw)

        out must not be equalTo("")
      }
    }
  }
}