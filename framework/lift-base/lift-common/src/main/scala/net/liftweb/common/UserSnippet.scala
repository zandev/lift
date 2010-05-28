package net.liftweb.common

import xml.NodeSeq

trait UserSnippet {
  def login(xhtml: NodeSeq): NodeSeq
  def signup(xhtml: NodeSeq): NodeSeq
  def changePassword(xhtml: NodeSeq): NodeSeq
}