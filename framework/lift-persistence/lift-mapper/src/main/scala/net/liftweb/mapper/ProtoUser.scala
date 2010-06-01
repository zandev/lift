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
package mapper {

import _root_.net.liftweb.http._
import js._
import JsCmds._
import _root_.scala.xml.{NodeSeq, Node, Text, Elem}
import _root_.scala.xml.transform._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import _root_.net.liftweb.util.Helpers._
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.util.Mailer._
import S._
import builtin.user._

trait ProtoUser[T <: ProtoUser[T]] extends KeyedMapper[Long, T] with UserIdAsString {
  self: T =>

  override def primaryKeyField = id

  // the primary key for the database
  object id extends MappedLongIndex(this)

  def userIdAsString: String = id.is.toString

  // First Name
  object firstName extends MappedString(this, 32) {
    override def displayName = fieldOwner.firstNameDisplayName
    override val fieldId = Some(Text("txtFirstName"))
  }

  def firstNameDisplayName = ??("first.name")

  // Last Name
  object lastName extends MappedString(this, 32) {
    override def displayName = fieldOwner.lastNameDisplayName
    override val fieldId = Some(Text("txtLastName"))
  }

  def lastNameDisplayName = ??("last.name")

  // Email
  object email extends MappedEmail(this, 48) {
    override def dbIndexed_? = true
    override def validations = valUnique(S.??("unique.email.address")) _ :: super.validations
    override def displayName = fieldOwner.emailDisplayName
    override val fieldId = Some(Text("txtEmail"))
  }

  def emailDisplayName = ??("email.address")
  // Password
  object password extends MappedPassword[T](this) {
    override def displayName = fieldOwner.passwordDisplayName
  }

  def passwordDisplayName = ??("password")

  object superUser extends MappedBoolean(this) {
    override def defaultValue = false
  }

  def niceName: String = (firstName.is, lastName.is, email.is) match {
    case (f, l, e) if f.length > 1 && l.length > 1 => f+" "+l+" ("+e+")"
    case (f, _, e) if f.length > 1 => f+" ("+e+")"
    case (_, l, e) if l.length > 1 => l+" ("+e+")"
    case (_, _, e) => e
  }

  def shortName: String = (firstName.is, lastName.is) match {
    case (f, l) if f.length > 1 && l.length > 1 => f+" "+l
    case (f, _) if f.length > 1 => f
    case (_, l) if l.length > 1 => l
    case _ => email.is
  }

  def niceNameWEmailLink = <a href={"mailto:"+email.is}>{niceName}</a>
}

trait MetaMegaProtoUser[ModelType <: MegaProtoUser[ModelType]] extends KeyedMetaMapper[Long, ModelType]
        with UserService[ModelType] with UserOperations[ModelType] with MapperUserFinders[ModelType]
        with MapperUserSnippet[ModelType] {
  self: ModelType => 

  case class MenuItem(name: String, path: List[String],
                      loggedIn: Boolean) {
    lazy val endOfPath = path.last
    lazy val pathStr: String = path.mkString("/", "/", "")
    lazy val display = name match {
      case null | "" => false
      case _ => true
    }
  }

  /**
   * Return the URL of the "login" page
   */
  def loginPageURL = loginPath.mkString("/","/", "")

  def loginFirst = If(
    loggedIn_? _,
    () => {
      import net.liftweb.http.{RedirectWithState, RedirectState}
      val uri = S.uriAndQueryString
      RedirectWithState(
        loginPageURL,
        RedirectState( ()=>{loginRedirect.set(uri)})
      )
    }
  )

  def userMenu: List[Node] = {
    val li = loggedIn_?
    ItemList.
    filter(i => i.display && i.loggedIn == li).
    map(i => (<a href={i.pathStr}>{i.name}</a>))
  }

  lazy val ItemList: List[MenuItem] =
  List(MenuItem(S.??("sign.up"), signUpPath, false),
       MenuItem(S.??("log.in"), loginPath, false),
       MenuItem(S.??("lost.password"), lostPasswordPath, false),
       MenuItem("", passwordResetPath, false),
       MenuItem(S.??("change.password"), changePasswordPath, true),
       MenuItem(S.??("log.out"), logoutPath, true),
       MenuItem(S.??("edit.profile"), editPath, true),
       MenuItem("", validateUserPath, false))

  protected object curUserId extends SessionVar[Box[String]](Empty)

  def testLoggedIn(page: String): Boolean =
  ItemList.filter(_.endOfPath == page) match {
    case x :: xs if x.loggedIn == loggedIn_? => true
    case _ => false
  }

  def validateUser(user: ModelType) = user.validate

  def setUserAccountValidated(user: ModelType, validated: Boolean) = {
    user.validated(validated)
    resetUniqueId(user).saveUser(user)
    this
  }

  def setPassword(user: ModelType, passwords: List[String]) = {
    user.password.setList(passwords)
    this
  }

  def setPassword(user: ModelType, password: String) = {
    user.password.setFromAny(password)
    this
  }

  def matchPassword(user: ModelType, password: String) = user.password.match_?(password)

  def resetUniqueId(user: ModelType) = {
    user.uniqueId.reset()
    this
  }

  def saveUser(user: ModelType) = user.save

  def authenticate(user: ModelType) = user.password.match_?(S.param("password").openOr("*"))

  def userService = this

  def userOperations = this

  def createUser = create
}

trait MegaProtoUser[T <: MegaProtoUser[T]] extends ProtoUser[T] with UserDetails {
  self: T =>
  object uniqueId extends MappedUniqueId(this, 32) {
    override def dbIndexed_? = true
    override def writePermission_?  = true
  }

  object validated extends MappedBoolean[T](this) {
    override def defaultValue = false
    override val fieldId = Some(Text("txtValidated"))
  }

  object locale extends MappedLocale[T](this) {
    override def displayName = fieldOwner.localeDisplayName
    override val fieldId = Some(Text("txtLocale"))
  }

  object timezone extends MappedTimeZone[T](this) {
    override def displayName = fieldOwner.timezoneDisplayName
    override val fieldId = Some(Text("txtTimeZone"))
  }

  def timezoneDisplayName = ??("time.zone")

  def localeDisplayName = ??("locale")

  def userEmail = email.is

  def userName = shortName

  def userLastName = lastName.is

  def userFirstName = firstName.is

  def userUniqueId = uniqueId.is

  override def userSuperUser_? = superUser.is
}

trait MapperUserFinders[ModelType <: MegaProtoUser[ModelType]] extends UserFinders[ModelType] {
  self: ModelType =>
  def findByUsername(username: String) = findByEmail(username)

  def findByEmail(email: String) = getSingleton.find(By(self.email, email))

  def findByUniqueId(uniqueId: String) = getSingleton.find(By(self.uniqueId, uniqueId))

  def findById(id: String) = getSingleton.find(id)
}



trait MapperUserSnippet[ModelType <: MegaProtoUser[ModelType]] extends CommonUserSnippet[ModelType] with MetaMapper[ModelType]{
  self: ModelType =>

  def signupFields: List[BaseOwnedMappedField[ModelType]] = firstName :: lastName :: email :: locale :: timezone :: password :: Nil

  override def fieldOrder: List[BaseOwnedMappedField[ModelType]] = firstName :: lastName :: email :: locale :: timezone :: password :: Nil

  def validateSignup(user: ModelType): List[FieldError] = user.validate

  def signupForm(user: ModelType): NodeSeq = localForm(user, false)

  def editForm(user: ModelType) = localForm(user, true)

  protected def localForm(user: ModelType, ignorePassword: Boolean): NodeSeq = {
    signupFields.
    map(fi => getSingleton.getActualBaseField(user, fi)).
    filter(f => !ignorePassword || (f match {
          case f: MappedPassword[ModelType] => false
          case _ => true
        })).
    flatMap(f =>
      f.toForm.toList.map(form =>
        (<tr><td>{f.displayName}</td><td>{form}</td></tr>) ) )
  }
}  

}
}
