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

/**
 * The first name of a user
 */
final case class FirstName(name: String)

/**
 * The last name of a user
 */
final case class LastName(name: String)

/**
 * The display name of a user
 */
final case class DisplayName(name: String)

/**
 * An email address
 */
final case class Email(email: String)

/**
 * Permissions for a user
 */
final case class UserPermissions(superUser: Boolean)

/**
 * A path to a page
 */
final case class Path(path: List[String])

object Path {
  implicit def iterToPath(it: Iterable[String]): Path =
    new Path(it.toList)
}

/**
 * The name of a domain
 */
final case class Domain(domain: String)

object Domain {
  implicit def strToDomain(str: String): Domain = Domain(str)
}

/**
 */
final case class PageKey(domain: Domain, path: Path)

object PageKey {
  implicit def basePairToPageKey[A <% Domain, B <% Path](in: (A, B)): PageKey =
    new PageKey(in._1, in._2)
}

/**
 * Nobody can agree on a date format, so
 * here's ours... with implicits to and from
 * the common ones
 */
final case class CMSDate(millis: Long)

object CMSDate {
  implicit def toLong(d: CMSDate): Long = d.millis
  implicit def fromLong(lng: Long): CMSDate = new CMSDate(lng)
  implicit def toJavaDate(d: CMSDate): java.util.Date = 
    new java.util.Date(d.millis)
  implicit def fromJavaDate(d: java.util.Date): CMSDate =
    new CMSDate(d.getTime)
  import org.joda.time._
  implicit def fromJodaDate(d: DateTime): CMSDate = 
    new CMSDate(d.getMillis)

  implicit def toJodaTate(d: CMSDate): DateTime = new DateTime(d.millis)
}
