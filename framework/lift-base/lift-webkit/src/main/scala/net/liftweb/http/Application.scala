/*
 * Copyright 2007-2010 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb {
package http {

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.util.Helpers._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.http.js.JSArtifacts
import _root_.net.liftweb.http.js.jquery._
import _root_.net.liftweb.http.provider._
import _root_.scala.xml._
import _root_.scala.collection.mutable.{ListBuffer}
import _root_.java.util.{Locale, TimeZone, ResourceBundle, Date}
import _root_.java.io.{InputStream, ByteArrayOutputStream, BufferedReader, StringReader}
import js._
import JE._
import JsCmds._
import auth._
import _root_.java.util.concurrent.{ConcurrentHashMap => CHash}
import _root_.scala.reflect.Manifest


trait Application 
  extends EnvironmentComponent 
  with HTTPComponent
  with SessionComponent
  with SitemapComponent
  with PresentationComponent
  with AJAXComponent
  with CometComponent
  with Factory
  with LazyLoggable
  with FormVendor {
  
  /**
   * Put a test for being logged in into this function
   */
  @volatile var loggedInTest: Box[() => Boolean] = Empty
  
  private def ctor() {
    appendGlobalFormBuilder(FormBuilderLocator[String]((value, setter) => SHtml.text(value, setter)))
    appendGlobalFormBuilder(FormBuilderLocator[Int]((value, setter) => SHtml.text(value.toString, s => Helpers.asInt(s).foreach((setter)))))
    appendGlobalFormBuilder(FormBuilderLocator[Boolean]((value, setter) => SHtml.checkbox(value, s => setter(s))))
  }
  ctor()
}


//object Application extends Factory with FormVendor with LazyLoggable {
object Application extends Application 


trait NotFound
case object DefaultNotFound extends NotFound
case class NotFoundAsResponse(response: LiftResponse) extends NotFound
case class NotFoundAsTemplate(path: ParsePath) extends NotFound
case class NotFoundAsNode(node: NodeSeq) extends NotFound
case object BreakOut
abstract class Bootable {
  def boot(): Unit;
}

/**
 * Factory object for RulesSeq instances
 */
object RulesSeq {
  def apply[T]: RulesSeq[T] = new RulesSeq[T] {}
}

/**
 * Generic container used mainly for adding functions
 *
 */
trait RulesSeq[T] {
  @volatile private var rules: List[T] = Nil

  private def safe_?(f: => Any) {
    LiftRules.doneBoot match {
      case false => f
      case _ => throw new IllegalStateException("Cannot modify after boot.");
    }
  }

  def toList = rules

  def prepend(r: T): RulesSeq[T] = {
    safe_? {
      rules = r :: rules
    }
    this
  }

  private[http] def remove(f: T => Boolean) {
    safe_? {
      rules = rules.remove(f)
    }
  }

  def append(r: T): RulesSeq[T] = {
    safe_? {
      rules = rules ::: List(r)
    }
    this
  }
}

trait FirstBox[F, T] {
  self: RulesSeq[F => Box[T]] =>

  def firstFull(param: F): Box[T] = {
    def finder(in: List[F => Box[T]]): Box[T] = in match {
      case Nil => Empty
      case x :: xs => x(param) match {
        case Full(r) => Full(r)
        case _ => finder(xs)
      }
    }

    finder(toList)
  }
}

private[http] case object DefaultBootstrap extends Bootable {
  def boot(): Unit = {
    val f = createInvoker("boot", Class.forName("bootstrap.liftweb.Boot").newInstance.asInstanceOf[AnyRef])
    f.map {f => f()}
  }
}

/**
 * Holds the Comet identification information
 */
trait CometVersionPair {
  def guid: String

  def version: Long
}

case class CVP(guid: String, version: Long) extends CometVersionPair

}
}
