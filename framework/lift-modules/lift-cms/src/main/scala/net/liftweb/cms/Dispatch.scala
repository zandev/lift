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

abstract class Dispatch[CMS <: CoreCMS](val cms: CMS) extends 
LiftRules.DispatchPF {
  type UserType = CMS#UserType
  type UserKey = CMS#UserKey
  type Record = CMS#Record
  type Key = CMS#Key
  
  import cms._

  /**
   * This method canonilizes the domain.  By default, it just
   * returns the domain
   */
  def canonicalizeDomain(domain: Domain): Domain = domain

  /**
   * Convert the request to a PageKey
   */
  def requestToPageKey(r: Req): PageKey =
    canonicalizeDomain(r.hostName) -> r.path.wholePath
}
