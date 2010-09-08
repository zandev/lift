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
import util._
import Helpers._

import java.io.File
import java.util.Locale
import scala.xml.NodeSeq

class FileCMS(baseDir: File) extends CoreCMS {
  type UserType = FileUser
  type UserKey = Email
  type Record = FileRecord
  type Key = FileKey

  @volatile
  private var pages: Map[Key, List[Record]] = Map()

  @volatile
  private var changeDates: Map[String, (Long, Key)] = Map()
  
  /**
   * Scan the files at the basedir
   */
  private def scanFiles() {
    val (unchanged, changed)= changeDates.partition
    {
      case (path, (changeTime, key)) =>
        val f = new File(path)
        f.exists && f.lastModified() == changeTime
    }

    val unchangedPages = changed.foldLeft(pages){
      case (p, (path, (changeTime, key))) => {
        p.getOrElse(key, Nil).filter(_.fromFile != path) match {
          case Nil => p - key
          case xs => p + (key -> xs)
        }
      }
    }

    def filesFor(root: File): Stream[File] = 
      if (root.isDirectory) 
        Stream(root.listFiles :_*).flatMap(filesFor)
    else if (root.isFile && root.getName.endsWith(".cms")) Stream(root)
    else Stream.Empty

    val filesToCheck = filesFor(baseDir).
    filter(f => unchanged.contains(f.getCanonicalPath)).
    flatMap(parseFile)

    filesToCheck.foldLeft(unchanged -> unchangedPages) {
      case ((cd, pa), (f, key, record)) =>
        (cd + (f.getCanonicalPath -> (f.lastModified() -> key))) -> 
      (pa + (key -> (record :: pa.getOrElse(key, Nil))))
    } match {
      case (cd, pa) => 
        pages = pa
        changeDates = cd
    }

    ActorPing.schedule(() => scanFiles(), 5 seconds)
  }

  private def parseFile(f: File): Box[(File, Key, Record)] = Empty
  
  /**
   * Get the user's first name
   */
  implicit def userToFirstName(user: UserType): FirstName = 
    FirstName(user.firstName)

  /**
   * Get the user's first name
   */
  implicit def userToLastName(user: UserType): LastName =
    LastName(user.lastName)

  /**
   * Get a record from backing store
   */
  def getRecord(key: Key): Box[Record] = Empty

  /**
   * Save the record in backing store
   */
  def putRecord(key: Key, record: Record): Unit = {}

  /**
   * What's the default locale
   */
  lazy val defaultLocale = Locale.getDefault

  def localeFor(in: String): Box[Locale] = 
    Locale.getAvailableLocales().filter(_.toString == in).headOption

}

final case class FileUser(email: String, firstName: String, lastName: String)

final case class FileKey(path: List[String], locale: Locale)

final case class FileRecord(path: List[String], locale: Locale,
                            fromFile: String,
                            tags: List[String] = Nil, 
                            validFrom: Box[CMSDate] = Empty,
                            validTo: Box[CMSDate] = Empty,
                            changeDate: CMSDate,
                            content: NodeSeq)
