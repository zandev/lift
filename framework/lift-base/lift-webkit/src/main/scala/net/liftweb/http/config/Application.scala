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

package net.liftweb.http {
package config {

import net.liftweb.common.{LazyLoggable,Full,Box,Empty}
import net.liftweb.util.Helpers._
import net.liftweb.util.{Helpers,FormBuilderLocator}
import net.liftweb.http._

trait Application 
  extends Environment 
  with HTTP
  with Session
  with Sitemap
  with Presentation
  with AJAX
  with Comet
  with Factory
  with LazyLoggable
  with FormVendor {
  
  /**
   * Put a test for being logged in into this function
   */
  @volatile var loggedInTest: Box[() => Boolean] = Empty
  
  // all lift applications will touch Application, so 
  // calling it directly is a sort of constructor for the singleton
  private def initilizer() {
    appendGlobalFormBuilder(FormBuilderLocator[String]((value, setter) => SHtml.text(value, setter)))
    appendGlobalFormBuilder(FormBuilderLocator[Int]((value, setter) => SHtml.text(value.toString, s => Helpers.asInt(s).foreach((setter)))))
    appendGlobalFormBuilder(FormBuilderLocator[Boolean]((value, setter) => SHtml.checkbox(value, s => setter(s))))
  }
  initilizer()
}

/** 
 * Concrete implementation of Application component traits
 */
object Application extends Application 


}
}
