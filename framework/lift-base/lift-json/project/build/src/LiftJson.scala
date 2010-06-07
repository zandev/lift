import sbt._
import java.net.URLClassLoader

class LiftJson(info: ProjectInfo) extends DefaultProject(info) {
  override def compileOptions = super.compileOptions ++ Seq(Unchecked)
  
  val paranamer  = "com.thoughtworks.paranamer" % "paranamer" % "2.0" % "compile->default"
  val junit      = "junit" % "junit" % "4.5" % "test"
  val specs      = "org.scala-tools.testing" % "specs" % "1.6.1" % "test"
  val scalacheck = "org.scala-tools.testing" % "scalacheck" % "1.6" % "test"

  override def crossScalaVersions = List("2.7.7", "2.8.0.Beta1-RC1")

  val DefaultXSchemaFileName = "xschema.json"
  val DefaultGeneratedSourcesDirectoryName = "main"
  val DefaultGeneratedTestSourcesDirectoryName = "test"

  var xSchemaFiles = List(mainResourcesPath / DefaultXSchemaFileName)
  var xSchemaGenerationBasePath = sourcePath
  var xSchemaGeneratedSourcesPath = xSchemaGenerationBasePath / DefaultGeneratedSourcesDirectoryName / "scala" 
  var xSchemaGeneratedTestSourcesPath = xSchemaGenerationBasePath / DefaultGeneratedTestSourcesDirectoryName / "scala"

  lazy val regenerateSources = regenerateSourcesTask dependsOn(`package`)
  def regenerateSourcesTask = task {
    val classLoader = new URLClassLoader(testClasspath.getURLs, this.getClass.getClassLoader)

    val generatorClass = Class.forName("net.liftweb.json.xschema.codegen.BaseScalaCodeGenerator", true, classLoader)
    val generator = generatorClass.newInstance
    val genCall = generatorClass.getMethod("generateFromFiles", classOf[Array[String]], classOf[String], classOf[String])
    genCall.invoke(
      generator, 
      xSchemaFiles.map(_.absolutePath).toArray.asInstanceOf[runtime.BoxedAnyArray].unbox(classOf[String]), 
      xSchemaGeneratedSourcesPath.absolutePath, 
      xSchemaGeneratedTestSourcesPath.absolutePath
    )

    None
  }

  override def ivyXML =
    <publications>
      <artifact name="lift-json" type="jar" ext="jar"/>
    </publications>
}
