package net.liftweb.builtin.snippet

import net.liftweb.common._
import net.liftweb.http._
import xml._


object LiftUser extends DispatchSnippet with UserSnippet {
  var snippet: Box[UserSnippet] = Empty
  def login(xhtml: NodeSeq) = snippet.map(_.login(xhtml)) openOr NodeSeq.Empty
  def signup(xhtml: NodeSeq) = snippet.map(_.signup(xhtml)) openOr NodeSeq.Empty
}