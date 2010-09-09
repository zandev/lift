package net.liftweb {
package http {
  
  import java.util.concurrent.{ConcurrentHashMap => CHash}
  import scala.reflect.Manifest
  import scala.xml.NodeSeq
  import net.liftweb.common.{Box,Full,Empty}
  import net.liftweb.util.FormBuilderLocator
  
  trait FormVendor {
    /**
     * Given a type manifest, vend a form
     */
    def vendForm[T](implicit man: Manifest[T]): Box[(T, T => Unit) => NodeSeq] = {
      val name = man.toString
      val first: Option[List[FormBuilderLocator[_]]] = requestForms.is.get(name) orElse sessionForms.is.get(name)

      first match {
        case Some(x :: _) => Full(x.func.asInstanceOf[(T, T => Unit) => NodeSeq])
        case _ => if (globalForms.containsKey(name)) {
          globalForms.get(name).headOption.map(_.func.asInstanceOf[(T, T => Unit) => NodeSeq])
        } else Empty
      }
    }

    private val globalForms: CHash[String, List[FormBuilderLocator[_]]] = new CHash

    def prependGlobalFormBuilder[T](builder: FormBuilderLocator[T]) {
      globalForms.synchronized {
        val name = builder.manifest.toString
        if (globalForms.containsKey(name)) {
          globalForms.put(name, builder :: globalForms.get(name))
        } else {
          globalForms.put(name, List(builder))
        }
      }
    }

    def appendGlobalFormBuilder[T](builder: FormBuilderLocator[T]) {
      globalForms.synchronized {
        val name = builder.manifest.toString
        if (globalForms.containsKey(name)) {
          globalForms.put(name, builder :: globalForms.get(name))
        } else {
          globalForms.put(name, List(builder))
        }
      }
    }

    def prependSessionFormBuilder[T](builder: FormBuilderLocator[T]) {
      sessionForms.set(prependBuilder(builder, sessionForms))
    }

    def appendSessionFormBuilder[T](builder: FormBuilderLocator[T]) {
      sessionForms.set(appendBuilder(builder, sessionForms))
    }

    def prependRequestFormBuilder[T](builder: FormBuilderLocator[T]) {
      requestForms.set(prependBuilder(builder, requestForms))
    }

    def appendRequestFormBuilder[T](builder: FormBuilderLocator[T]) {
      requestForms.set(appendBuilder(builder, requestForms))
    }

    def doWith[F, T](builder: FormBuilderLocator[T])(f: => F): F =
      requestForms.doWith(prependBuilder(builder, requestForms))(f)


    private def prependBuilder(builder: FormBuilderLocator[_], to: Map[String, List[FormBuilderLocator[_]]]):
    Map[String, List[FormBuilderLocator[_]]] = {
      val name = builder.manifest.toString
      to + (name -> (builder :: to.getOrElse(name, Nil)))
    }

    private def appendBuilder(builder: FormBuilderLocator[_], to: Map[String, List[FormBuilderLocator[_]]]):
    Map[String, List[FormBuilderLocator[_]]] = {
      val name = builder.manifest.toString
      to + (name -> (builder :: to.getOrElse(name, Nil)))
    }


    private object sessionForms extends SessionVar[Map[String, List[FormBuilderLocator[_]]]](Map())
    private object requestForms extends SessionVar[Map[String, List[FormBuilderLocator[_]]]](Map())
  }
  
}}
