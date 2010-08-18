import sbt._
import java.net.URLClassLoader
import _root_.net.liftweb.json.xschema.XRoot
import _root_.net.liftweb.json.xschema.codegen.BaseScalaCodeGenerator

trait XSchemaProject { this: DefaultProject =>
  val DefaultXSchemaFileName = "xschema.json"
  val DefaultGeneratedSourcesDirectoryName = "generated-sources"
  val DefaultGeneratedTestSourcesDirectoryName = "generated-test-sources"

  var xSchemaFiles = List(mainResourcesPath / DefaultXSchemaFileName)
  var xSchemaGenerationBasePath = outputPath
  var xSchemaGeneratedSourcesPath = xSchemaGenerationBasePath / DefaultGeneratedSourcesDirectoryName
  var xSchemaGeneratedTestSourcesPath = xSchemaGenerationBasePath / DefaultGeneratedTestSourcesDirectoryName
  var xSchemaNamespaces: List[String] = List()

  lazy val xschemaGenerateSources = xschemaGenerateSourcesTask 
  def xschemaGenerateSourcesTask = task {
    (new BaseScalaCodeGenerator).generateFromFiles(
      xSchemaFiles.map(_.absolutePath).toArray,
      xSchemaGeneratedSourcesPath.absolutePath, 
      xSchemaGeneratedTestSourcesPath.absolutePath,
      xSchemaNamespaces.toArray
    )     

    None
  }

  var xSchemaXRootProvider: String
  var xSchemaOutputFile = mainResourcesPath / DefaultXSchemaFileName

  lazy val xschemaGenerateXSchema = xschemaGenerateXSchemaTask dependsOn(testCompile)
  def xschemaGenerateXSchemaTask = task {
    val xroot = Class.forName(xSchemaXRootProvider).newInstance.asInstanceOf[XRoot]
    (new BaseScalaCodeGenerator).generateXSchema(xroot, xSchemaOutputFile.absolutePath)
    
    None
  }
}

// vim: set ts=2 sw=2 et:
