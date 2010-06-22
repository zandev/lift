/*
 * Copyright 2006-2010 WorldWide Conferencing, LLC
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
package jdbc {
package common {
  
  import _root_.java.sql.Connection
  import _root_.net.liftweb.common.{Box,Empty}
  
  class SuperConnection(val connection: Connection, val releaseFunc: () => Unit, val schemaName: Box[String]) {
    def this(c: Connection, rf: () => Unit) = this (c, rf, Empty)
    
    // def createTablePostpend: String = driverType.createTablePostpend
    // def supportsForeignKeys_? : Boolean = driverType.supportsForeignKeys_?
    // lazy val brokenLimit_? = driverType.brokenLimit_?
    //lazy val driverType: DriverType = DriverType.calcDriver(connection)
    lazy val metaData = connection.getMetaData
  }

  object SuperConnection {
    implicit def superToConn(in: SuperConnection): Connection = in.connection
  }
  
  trait ConnectionIdentifier {
    def jndiName: String
    override def toString() = "ConnectionIdentifier(" + jndiName + ")"
    override def hashCode() = jndiName.hashCode()
    override def equals(other: Any): Boolean = other match {
      case ci: ConnectionIdentifier => ci.jndiName == this.jndiName
      case _ => false
    }
  }
  
  case object DefaultConnectionIdentifier extends ConnectionIdentifier {
    var jndiName = "lift"
  }
  
}}}
