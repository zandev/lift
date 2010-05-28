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

package net.liftweb.mapper

import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import _root_.net.liftweb.util._
import _root_.net.liftweb.util.Helpers._
import _root_.net.liftweb.http.js.JsCmds._
import xml._
import transform._

trait UserDetails extends UserIdAsString {
  def userFirstName: String
  def userLastName: String
  def userName: String
  def userEmail: String
  def userSuperUser_? : Boolean = false
  def userEnabled_? : Boolean = true
  def userLocked_? : Boolean = false
  def userValidated_? : Boolean = true
}

trait UserFinders[ModelType <: UserDetails] {
  def findById(id: String): Box[ModelType]
  def findByUsername(username: String): Box[ModelType]
  def findByUniqueId(uniqueId: String): Box[ModelType]
}

trait MapperUserFinders[ModelType <: MegaProtoUser[ModelType]] extends UserFinders[ModelType] {
  self: ModelType => 
  def findByUsername(username: String) = getSingleton.find(By(email, username))

  def findByUniqueId(uniqueId: String) = getSingleton.find(By(self.uniqueId, uniqueId))

  def findById(id: String) = getSingleton.find(id)
}

trait UserService[ModelType <: UserDetails] extends UserFinders[ModelType]{
  /**
   * This function is given a chance to log in a user
   * programmatically when needed
   */
  var autologinFunc: Box[()=>Unit] = Empty

  var onLogIn: List[ModelType => Unit] = Nil

  var onLogOut: List[Box[ModelType] => Unit] = Nil

  lazy val testLogginIn = If(loggedIn_? _, S.??("must.be.logged.in"))

  lazy val testSuperUser = If(superUser_? _, S.??("must.be.super.user"))

//  var authenticationProvider: AuthenticationProvider

  protected val curUserId: AnyVarTrait[Box[String], _]
  protected object curUser extends RequestVar[Box[ModelType]](currentUserId.flatMap(id => findById(id))) with CleanRequestVarOnSessionTransition

  def createUser: ModelType

  def currentUserId: Box[String] = curUserId.is

  def currentUser: Box[ModelType] = curUser.is

  def loggedIn_? = {
    if(!currentUserId.isDefined)
      for(f <- autologinFunc) f()
    currentUserId.isDefined
  }

  def notLoggedIn_? = !loggedIn_?

  def superUser_? : Boolean = currentUser.map(_.userSuperUser_?) openOr false

  def logUserIdIn(id: String) {
    curUser.remove()
    curUserId(Full(id))
  }
  
  def logUserIn(who: ModelType) {
    logUserIdIn(who.userIdAsString)
    onLogIn.foreach(_(who))
  }

  def logoutCurrentUser() = logUserOut()

  def logUserOut() {
    onLogOut.foreach(_(curUser))
    curUserId.remove()
    curUser.remove()
    S.request.foreach(_.request.session.terminate)
  }

  //def authenticate(authentication: Authentication) = authenticationProvider.authenticate(authentication)
  def authenticate(user: ModelType): Boolean
}
/*
trait AuthenticationProvider {
  type Authentication
  def authenticate(authentication: Authentication)
}

class UserServiceAuthenticationProvider[ModelType <: UserDetails](userService: UserService[ModelType]) extends AuthenticationProvider {
  type
  def authenticate(authentication: Authentication[ModelType]) =
}

abstract class Authentication[ModelType] {
  def authenticate(userService: UserService[ModelType])
}

case class EmailPassword[ModelType](email: String, password: String) extends Authentication {
  def authenticate(userService: UserService[ModelType]) = null
}*/

trait UserOperations[ModelType <: UserDetails] {

  def userService: UserService[ModelType] 
  /**
   * If the
   */
  def screenWrap: Box[Node] = Empty

  // Menu related

  val basePath: List[String] = "user_mgt" :: Nil
  def signUpSuffix = "sign_up"
  lazy val signUpPath = thePath(signUpSuffix)
  def loginSuffix = "login"
  lazy val loginPath = thePath(loginSuffix)
  def lostPasswordSuffix = "lost_password"
  lazy val lostPasswordPath = thePath(lostPasswordSuffix)
  def passwordResetSuffix = "reset_password"
  lazy val passwordResetPath = thePath(passwordResetSuffix)
  def changePasswordSuffix = "change_password"
  lazy val changePasswordPath = thePath(changePasswordSuffix)
  def logoutSuffix = "logout"
  lazy val logoutPath = thePath(logoutSuffix)
  def editSuffix = "edit"
  lazy val editPath = thePath(editSuffix)
  def validateUserSuffix = "validate_user"
  lazy val validateUserPath = thePath(validateUserSuffix)

  def homePage = "/"
  def thePath(end: String): List[String] = basePath ::: List(end)

  /**
   * The menu item for login (make this "Empty" to disable)
   */
  def loginMenuLoc: Box[Menu] =
    Full(Menu(Loc("Login", loginPath, S.??("login"), loginMenuLocParams)))

  /**
   * The LocParams for the menu item for login.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def loginMenuLocParams: List[LocParam[Unit]] =
    If(userService.notLoggedIn_? _, S.??("already.logged.in")) ::
    Template(() => wrapIt(login)) ::
    Nil

  /**
   * The menu item for logout (make this "Empty" to disable)
   */
  def logoutMenuLoc: Box[Menu] =
    Full(Menu(Loc("Logout", logoutPath, S.??("logout"), logoutMenuLocParams)))

  /**
   * The LocParams for the menu item for logout.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def logoutMenuLocParams: List[LocParam[Unit]] =
    Template(() => wrapIt(logout)) ::
    userService.testLogginIn ::
    Nil

  /**
   * The menu item for creating the user/sign up (make this "Empty" to disable)
   */
  def createUserMenuLoc: Box[Menu] =
    Full(Menu(Loc("CreateUser", signUpPath, S.??("sign.up"), createUserMenuLocParams)))

  /**
   * The LocParams for the menu item for creating the user/sign up.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def createUserMenuLocParams: List[LocParam[Unit]] =
    Template(() => wrapIt(signupFunc.map(_()) openOr signup)) ::
    If(userService.notLoggedIn_? _, S.??("logout.first")) ::
    Nil

  /**
   * The menu item for lost password (make this "Empty" to disable)
   */
  def lostPasswordMenuLoc: Box[Menu] =
    Full(Menu(Loc("LostPassword", lostPasswordPath, S.??("lost.password"), lostPasswordMenuLocParams))) // not logged in

  /**
   * The LocParams for the menu item for lost password.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def lostPasswordMenuLocParams: List[LocParam[Unit]] =
    Template(() => wrapIt(lostPassword)) ::
    If(userService.notLoggedIn_? _, S.??("logout.first")) ::
    Nil

  /**
   * The menu item for resetting the password (make this "Empty" to disable)
   */
  def resetPasswordMenuLoc: Box[Menu] =
    Full(Menu(Loc("ResetPassword", (passwordResetPath, true), S.??("reset.password"), resetPasswordMenuLocParams))) //not Logged in

  /**
   * The LocParams for the menu item for resetting the password.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def resetPasswordMenuLocParams: List[LocParam[Unit]] =
    Hidden ::
    Template(() => wrapIt(passwordReset(snarfLastItem))) ::
    If(userService.notLoggedIn_? _, S.??("logout.first")) ::
    Nil

  /**
   * The menu item for editing the user (make this "Empty" to disable)
   */
  def editUserMenuLoc: Box[Menu] =
    Full(Menu(Loc("EditUser", editPath, S.??("edit.user"), editUserMenuLocParams)))

  /**
   * The LocParams for the menu item for editing the user.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def editUserMenuLocParams: List[LocParam[Unit]] =
    Template(() => wrapIt(editFunc.map(_()) openOr edit)) ::
    userService.testLogginIn ::
    Nil

  /**
   * The menu item for changing password (make this "Empty" to disable)
   */
  def changePasswordMenuLoc: Box[Menu] =
    Full(Menu(Loc("ChangePassword", changePasswordPath, S.??("change.password"), changePasswordMenuLocParams)))

  /**
   * The LocParams for the menu item for changing password.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def changePasswordMenuLocParams: List[LocParam[Unit]] =
    Template(() => wrapIt(changePassword)) ::
    userService.testLogginIn ::
    Nil

  /**
   * The menu item for validating a user (make this "Empty" to disable)
   */
  def validateUserMenuLoc: Box[Menu] =
    Full(Menu(Loc("ValidateUser", (validateUserPath, true), S.??("validate.user"), validateUserMenuLocParams)))

  /**
   * The LocParams for the menu item for validating a user.
   * Overwrite in order to add custom LocParams. Attention: Not calling super will change the default behavior!
   */
  protected def validateUserMenuLocParams: List[LocParam[Unit]] =
    Hidden ::
    Template(() => wrapIt(validateUser(snarfLastItem))) ::
    If(userService.notLoggedIn_? _, S.??("logout.first")) ::
    Nil

  /**
  * An alias for the sitemap property
  */
  def menus: List[Menu] = sitemap // issue 182

  lazy val sitemap: List[Menu] =
  List(loginMenuLoc, logoutMenuLoc, createUserMenuLoc,
       lostPasswordMenuLoc, resetPasswordMenuLoc,
       editUserMenuLoc, changePasswordMenuLoc,
       validateUserMenuLoc).flatten(a => a)

  protected def wrapIt(in: NodeSeq): NodeSeq =
  screenWrap.map(new RuleTransformer(new RewriteRule {
        override def transform(n: Node) = n match {
          case e: Elem if "bind" == e.label && "lift" == e.prefix => in
          case _ => n
        }
      })) openOr in

  // Default XHTML implementations

  def login: NodeSeq

  def logout = {
    userService.logoutCurrentUser
    S.redirectTo(homePage)
  }

  def lostPassword: NodeSeq

  def passwordReset(id: String): NodeSeq

  def changePassword: NodeSeq

  def validateUser(id: String): NodeSeq

  def signup(): NodeSeq

  def edit(): NodeSeq

  object signupFunc extends RequestVar[Box[() => NodeSeq]](Empty)

  object editFunc extends RequestVar[Box[() => NodeSeq]](Empty)

  protected def snarfLastItem: String =
  (for (r <- S.request) yield r.path.wholePath.last) openOr ""

  def emailFrom = "noreply@"+S.hostName

  def bccEmail: Box[String] = Empty
}

trait DefaultUserSnippet[ModelType <: UserDetails] extends UserSnippet {
  def userService: UserService[ModelType]
  def userOperations: UserOperations[ModelType]

  object loginRedirect extends SessionVar[Box[String]](Empty)

  def login(xhtml: NodeSeq): NodeSeq = {
    if (S.post_?) {
      S.param("username").
      flatMap(username => userService.findByUsername(username)) match {
        case Full(user) if user.userValidated_? &&
          userService.authenticate(user) =>
          S.notice(S.??("logged.in"))
          userService.logUserIn(user)
          val redir = loginRedirect.is match {
            case Full(url) =>
              loginRedirect(Empty)
              url
            case _ =>
              userOperations.homePage
          }
          S.redirectTo(redir)

        case Full(user) if !user.userValidated_? =>
          S.error(S.??("account.validation.error"))

        case _ => S.error(S.??("invalid.credentials"))
      }
    }
    bind("user", xhtml,
      "email" -> (FocusOnLoad(<input type="text" name="username"/>)),
      "password" -> (<input type="password" name="password"/>),
      "submit" -> (<input type="submit" value={S.??("log.in")}/>))
  }
  
  def login: NodeSeq = login(loginXhtml)

  def loginXhtml = {
    (<form method="post" action={S.uri}><table><tr><td
              colspan="2">{S.??("log.in")}</td></tr>
          <tr><td>{S.??("email.address")}</td><td><user:email /></td></tr>
          <tr><td>{S.??("password")}</td><td><user:password /></td></tr>
          <tr><td><a href={userOperations.lostPasswordPath.mkString("/", "/", "")}
                >{S.??("recover.password")}</a></td><td><user:submit /></td></tr></table>
     </form>)
  }

  def changePassword(xhtml: NodeSeq): NodeSeq = {
    val user = userSerivce.currentUser.open_! // we can do this because the logged in test has happened
    var oldPassword = ""
    var newPassword: List[String] = Nil
    bind("user", xhtml,
         "old_pwd" -> SHtml.password("", s => oldPassword = s),
         "new_pwd" -> SHtml.password_*("", LFuncHolder(s => newPassword = s)),
         "submit" -> SHtml.submit(S.??("change"), changePasswordCallback(user, oldPassword, newPassword) _))
  }

  def changePasswordCallback(user: ModelType, oldPassword: String, newPassword: List[String]): Unit

  def changePassword: NodeSeq = changePassword(changePasswordXhtml)

  def changePasswordXhtml = {
    (<form method="post" action={S.uri}>
        <table><tr><td colspan="2">{S.??("change.password")}</td></tr>
          <tr><td>{S.??("old.password")}</td><td><user:old_pwd /></td></tr>
          <tr><td>{S.??("new.password")}</td><td><user:new_pwd /></td></tr>
          <tr><td>{S.??("repeat.password")}</td><td><user:new_pwd /></td></tr>
          <tr><td>&nbsp;</td><td><user:submit /></td></tr>
        </table>
     </form>)
  }

  def signup(xhtml: NodeSeq) = {
    val theUser: ModelType = userService.createUser

    def testSignup() {
      validateSignup(theUser) match {
        case Nil =>
          actionsAfterSignup(theUser)
          S.redirectTo(userOperations.homePage)

        case xs => S.error(xs) ; userOperations.signupFunc(Full(innerSignup _))
      }
    }

    def innerSignup = bind("user", signupXhtml(theUser),
                           "submit" -> SHtml.submit(S.??("sign.up"), testSignup _))

    innerSignup
  }

  def signup = signup(NodeSeq.Empty)

  /**
   * Override this method to validate the user signup (eg by adding captcha verification)
   */
  def validateSignup(user: ModelType): List[FieldError]

  def signupXhtml(user: ModelType) = {
    (<form method="post" action={S.uri}><table><tr><td
              colspan="2">{ S.??("sign.up") }</td></tr>
          {signupForm(user)}
          <tr><td>&nbsp;</td><td><user:submit/></td></tr>
                                        </table></form>)
  }

  def signupForm(user: ModelType): NodeSeq

  def signupMailBody(user: ModelType, validationLink: String) = {
    (<html>
        <head>
          <title>{S.??("sign.up.confirmation")}</title>
        </head>
        <body>
          <p>{S.??("dear")} {user.userFirstName},
            <br/>
            <br/>
            {S.??("sign.up.validation.link")}
            <br/><a href={validationLink}>{validationLink}</a>
            <br/>
            <br/>
            {S.??("thank.you")}
          </p>
        </body>
     </html>)
  }

  def signupMailSubject = S.??("sign.up.confirmation")

  def sendValidationEmail(user: ModelType) {
    val resetLink = S.hostAndPath+"/"+validateUserPath.mkString("/")+
    "/"+user.uniqueId

    val email: String = user.userEmail

    val msgXml = signupMailBody(user, resetLink)

    Mailer.sendMail(From(emailFrom),Subject(signupMailSubject),
                    (To(user.email) :: xmlToMailBodyType(msgXml) ::
                     (bccEmail.toList.map(BCC(_)))) :_* )
  }

  /**
   * Override this method to do something else after the user signs up
   */
  protected def actionsAfterSignup(theUser: ModelType) {
    if (!skipEmailValidation) {
      sendValidationEmail(theUser)
      S.notice(S.??("sign.up.message"))
    } else {
      S.notice(S.??("welcome"))
      logUserIn(theUser)
    }
  }

  def edit(xhtml: NodeSeq) = {
    val theUser: ModelType = userService.currentUser.open_! // we know we're logged in

    def testEdit() {
      theUser.validate match {
        case Nil =>
          theUser.save
          S.notice(S.??("profile.updated"))
          S.redirectTo(homePage)

        case xs => S.error(xs) ; editFunc(Full(innerEdit _))
      }
    }

    def innerEdit = bind("user", editXhtml(theUser),
                         "submit" -> SHtml.submit(S.??("edit"), testEdit _))

    innerEdit
  }

  def edit(): NodeSeq = edit(editXhtml())

  def editXhtml(user: ModelType) = {
    (<form method="post" action={S.uri}>
        <table><tr><td colspan="2">{S.??("edit")}</td></tr>
          {localForm(user, true)}
          <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
     </form>)
  }
}

trait MapperUserSnippet[ModelType <: MegaProtoUser] extends DefaultUserSnippet[ModelType] {
  self: ModelType =>

  def signupFields: List[BaseOwnedMappedField[ModelType]] = firstName :: lastName :: email :: locale :: timezone :: password :: Nil

  override def fieldOrder: List[BaseOwnedMappedField[ModelType]] = firstName :: lastName :: email :: locale :: timezone :: password :: Nil
  
  def signupForm(user: ModelType) = null
  
  def changePasswordCallback(user: ModelType, oldPassword: String, newPassword: List[String]): Unit = {
    if (!user.password.match_?(oldPassword)) {
      S.error(S.??("wrong.old.password"))
    } else {
        user.password.setFromAny(newPassword)
        user.validate match {
          case Nil => user.save; S.notice(S.??("password.changed")); S.redirectTo(homePage)
          case xs => S.error(xs)
        }
      }
  }

  def validateSignup(user: ModelType): List[FieldError] = user.validate

  override protected def actionsAfterSignup(theUser: ModelType) = {
    theUser.validated(skipEmailValidation).uniqueId.reset()
    theUser.save
    super.actionsAfterSignup(theUser)
  }

  def signupForm(user: ModelType): NodeSeq = localForm(user, false)

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