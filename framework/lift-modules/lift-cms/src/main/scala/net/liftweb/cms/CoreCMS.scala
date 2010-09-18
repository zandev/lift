/*
 * Copyright 2010 WorldWide Conferencing, LLC
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

package net.liftweb
package cms

import common._
import http._

import java.util.Locale

/**
 * The core definition of the CMS system
 */
trait CoreCMS {
  type UserType
  type UserKey
  type Record
  type Key

  /**
   * Get a record from backing store
   */
  def getRecord(key: Key): Box[Record]
  /**
   * Save the record in backing store
   */
  def putRecord(key: Key, record: Record): Unit
  /**
   * Get the user's first name
   */
  implicit def userToFirstName(user: UserType): FirstName

  /**
   * Get the user's first name
   */
  implicit def userToLastName(user: UserType): LastName

  implicit def requestToHost(in: Req): Host =
    Host(in.hostName)

  implicit def recordToContent(in: Record): Content

  /**
   * Convert the core information into a key
   */
  implicit def infoToKey(host: Host,
                         path: Path,
                         locale: Locale): Key
}
