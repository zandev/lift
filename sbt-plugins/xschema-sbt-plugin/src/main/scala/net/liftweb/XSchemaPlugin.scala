import sbt._
import java.net.URLClassLoader
import _root_.net.liftweb.json.xschema.codegen.BaseScalaCodeGenerator

trait XSchemaProject { this: DefaultProject =>
  val DefaultXSchemaFileName = "xschema.json"
  val DefaultGeneratedSourcesDirectoryName = "generated-sources"
  val DefaultGeneratedTestSourcesDirectoryName = "generated-test-sources"

  var xSchemaFiles = List(mainResourcesPath / DefaultXSchemaFileName)
  var xSchemaGenerationBasePath = outputPath
  var xSchemaGeneratedSourcesPath = xSchemaGenerationBasePath / DefaultGeneratedSourcesDirectoryName
  var xSchemaGeneratedTestSourcesPath = xSchemaGenerationBasePath / DefaultGeneratedTestSourcesDirectoryName

  lazy val generateSources = generateSourcesTask 
  def generateSourcesTask = task {
    (new BaseScalaCodeGenerator).generateFromFiles(
      xSchemaFiles.map(_.absolutePath).toArray,
      xSchemaGeneratedSourcesPath.absolutePath, 
      xSchemaGeneratedTestSourcesPath.absolutePath
    )     

    None
  }
}

// vim: set ts=2 sw=2 et:
