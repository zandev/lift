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

import util._
import Helpers._

import java.util.Locale

class Dispatch[CMS <: CoreCMS](val cms: CMS) extends LiftRules.DispatchPF {
  type UserType = cms.UserType
  type UserKey = cms.UserKey
  type Record = cms.Record
  type Key = cms.Key
  
  import cms._

  /**
   * Sets the current Req and clears out the key, etc. vars
   */
  private def setReq(req: Req) {
    if (currentReq.is != Full(req)) {
      currentReq.set(Full(req))
      currentLocale.remove()
      currentHost.remove()
      currentKey.remove()
      currentRecord.remove()
    }
  }
  
  /**
   * Set up current request state... calculate the host
   */
  private def calcHost() = currentReq.map(cms.requestToHost)

  private def calcLocale() = LiftRules.localeCalculator(
    currentReq.is.map(_.request))

  /**
   * Set up current request state... calculate the Key
   */
  private def calcKey(): Box[Key] = 
    for {
      host <- currentHost.is
      req <- currentReq
      path = Path(req.path.wholePath)
      locale <- currentLocale.is
    } yield cms.infoToKey(host, path, locale)
                          

  /**
   * Set up current request state... calculate the Key
   */
  private def calcRecord(): Box[Record] = 
    for {
      key <- currentKey.is
      record <- cms.getRecord(key)
    } yield record

  /**
   * calculate a response based on the request
   */
  private def calcResponse(): Box[LiftResponse] = 
    for {
      req <- currentReq.is
      record <- currentRecord.is
      HtmlContent(elem) <- Full(cms.recordToContent(record))
    } yield null // FIXME

  def isDefinedAt(req: Req): Boolean = {
    setReq(req)
    currentRecord.is.isDefined
  }

  def apply(req: Req):() => Box[LiftResponse] = {
    setReq(req)
    calcResponse _
  }

  private object currentReq extends TransientRequestVar[Box[Req]](Empty) {
    override val __nameSalt = randomString(10)
  }

  private object currentLocale extends 
  TransientRequestVar[Box[Locale]](Full(calcLocale())) {
    override val __nameSalt = randomString(10)
  }

  private object currentHost extends TransientRequestVar[Box[Host]](calcHost()) {
    override val __nameSalt = randomString(10)
  }

  private object currentKey extends TransientRequestVar[Box[Key]](calcKey()) {
    override val __nameSalt = randomString(10)
  }

  private object currentRecord extends TransientRequestVar[Box[Record]](calcRecord()) {
    override val __nameSalt = randomString(10)
  }

  
}
