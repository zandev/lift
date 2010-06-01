package net.liftweb.builtin.snippet

import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.builtin.user._
import xml._

object LiftUser extends DispatchSnippet with UserSnippet {
  var snippet: Box[UserSnippet] = Empty
  def dispatch: DispatchIt = {
    case "login" => login _
    case "signup" => signup _
    case "changePassword" => changePassword _
    case "passwordReset" => passwordReset _
    case "lostPassword" => lostPassword _
    case "edit" => edit _
  }
  def login(xhtml: NodeSeq) = snippet.map(_.login(xhtml)) openOr NodeSeq.Empty
  def signup(xhtml: NodeSeq) = snippet.map(_.signup(xhtml)) openOr NodeSeq.Empty
  def changePassword(xhtml: NodeSeq): NodeSeq = snippet.map(_.changePassword(xhtml)) openOr NodeSeq.Empty
  def passwordReset(xhtml: NodeSeq) = snippet.map(_.passwordReset(xhtml)) openOr NodeSeq.Empty
  def lostPassword(xhtml: NodeSeq) = snippet.map(_.lostPassword(xhtml)) openOr NodeSeq.Empty
  def edit(xhtml: NodeSeq) = snippet.map(_.edit(xhtml)) openOr NodeSeq.Empty
}