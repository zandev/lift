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
package jdbc {
package common {
  
  import _root_.javax.sql.{DataSource}
  import _root_.javax.naming.{Context, InitialContext}
  import _root_.scala.collection.mutable.HashMap
  import _root_.net.liftweb.common.{Loggable,Box,Full,Empty}
  import _root_.net.liftweb.util.{DynoVar,ThreadGlobal,Helpers,LoanWrapper}
  import _root_.net.liftweb.util.Helpers._
  
  object ConnectionAccessor extends ConnectionAccessor
  
  trait ConnectionAccessor extends Loggable {
    
    private val connectionManagers = new HashMap[ConnectionIdentifier, ConnectionManager]
    private val threadLocalConnectionManagers = new ThreadGlobal[Map[ConnectionIdentifier, ConnectionManager]]
    private val threadStore = new ThreadLocal[HashMap[ConnectionIdentifier, ConnectionHolder]]
    private val _postCommitFuncs = new ThreadLocal[List[() => Unit]]
    
    private def postCommit: List[() => Unit] =
      _postCommitFuncs.get match {
        case null =>
          _postCommitFuncs.set(Nil)
          Nil

        case v => v
      }

    private def postCommit_=(lst: List[() => Unit]): Unit = _postCommitFuncs.set(lst)

    /**
     * perform this function post-commit.  THis is helpful for sending messages to Actors after we know
     * a transaction has committed
     */
    def performPostCommit(f: => Unit) {
      postCommit = (() => f) :: postCommit
    }
    
    /**
     *  Append a function to be invoked after the commit has taken place for the given connection identifier
     */
    def appendPostFunc(name: ConnectionIdentifier, func: () => Unit) {
      info.get(name) match {
        case Some(ConnectionHolder(c, n, post)) => info(name) = ConnectionHolder(c, n, func :: post)
        case _ =>
      }
    }
    
    case class ConnectionHolder(conn: SuperConnection, cnt: Int, postCommit: List[() => Unit])
    
    protected class ThreadBasedConnectionManager(connections: List[ConnectionIdentifier]) {
      private var used: Set[ConnectionIdentifier] = Set()
      def use(conn: ConnectionIdentifier): Int = if (connections.contains(conn)) {
        used += conn
        1
      } else 0
    }

    protected object CurrentConnectionSet extends DynoVar[ThreadBasedConnectionManager]
    
    def defineConnectionManager(name: ConnectionIdentifier, mgr: ConnectionManager) {
      connectionManagers(name) = mgr
    }

    /**
     * Allows you to override the connection manager associated with particular connection identifiers for the duration
     * of the call.
     */
    def doWithConnectionManagers[T](mgrs: (ConnectionIdentifier, ConnectionManager)*)(f: => T): T = {
      val newMap = mgrs.foldLeft(threadLocalConnectionManagers.box openOr Map())(_ + _)
      threadLocalConnectionManagers.doWith(newMap)(f)
    }
    
    def jndiJdbcConnAvailable_?(connectionIdentifier: ConnectionIdentifier) : Boolean = {
      try {
        ((new InitialContext).lookup("java:/comp/env").asInstanceOf[Context].lookup(connectionIdentifier.jndiName).asInstanceOf[DataSource].getConnection) != null
      } catch {
        case e => false
      }
    }
    
    def jndiJdbcConnAvailable_? : Boolean = jndiJdbcConnAvailable_?(DefaultConnectionIdentifier)
    
    protected def newConnection(name: ConnectionIdentifier): SuperConnection = {
      val ret = ((threadLocalConnectionManagers.box.flatMap(_.get(name)) or Box(connectionManagers.get(name))).flatMap(cm => cm.newSuperConnection(name) or cm.newConnection(name).map(c => new SuperConnection(c, () => cm.releaseConnection(c))))) openOr {
        Helpers.tryo {
          val uniqueId = if (logger.isDebugEnabled) Helpers.nextNum.toString else ""
          logger.debug("Connection ID " + uniqueId + " for JNDI connection " + name.jndiName + " opened")
          val conn = (new InitialContext).lookup("java:/comp/env").asInstanceOf[Context].lookup(name.jndiName).asInstanceOf[DataSource].getConnection
          new SuperConnection(conn, () => {logger.debug("Connection ID " + uniqueId + " for JNDI connection " + name.jndiName + " closed"); conn.close})
        } openOr {
          throw new NullPointerException("Looking for Connection Identifier " + name + " but failed to find either a JNDI data source " +
                                         "with the name " + name.jndiName + " or a lift connection manager with the correct name")
        }
      }
      ret.setAutoCommit(false)
      ret
    }
    
    
    protected def releaseConnection(conn: SuperConnection): Unit = conn.close
    
    protected def calcBaseCount(conn: ConnectionIdentifier): Int =
      CurrentConnectionSet.is.map(_.use(conn)) openOr 0
    
    protected def getConnection(name: ConnectionIdentifier, transformationFunc: Box[SuperConnection => SuperConnection]): SuperConnection = {
      logger.trace("Acquiring connection " + name + " on thread " + Thread.currentThread)
      var ret = info.get(name) match {
        case None => ConnectionHolder((transformationFunc.map(f => f(newConnection(name))) getOrElse newConnection(name)), calcBaseCount(name) + 1, Nil)
        case Some(ConnectionHolder(conn, cnt, post)) => ConnectionHolder(conn, cnt + 1, post)
      }
      info(name) = ret
      logger.trace("Acquired connection " + name + " on thread " + Thread.currentThread +
                " count " + ret.cnt)
      ret.conn
    }
    
    // for backward compatibility
    protected def getConnection(name: ConnectionIdentifier): SuperConnection = getConnection(name,Empty)
    
    private def info: HashMap[ConnectionIdentifier, ConnectionHolder] = {
      threadStore.get match {
        case null =>
          val tinfo = new HashMap[ConnectionIdentifier, ConnectionHolder]
          threadStore.set(tinfo)
          tinfo

        case v => v
      }
    }
    
    // remove thread-local association
    protected def clearThread(success: Boolean): Unit = {
      val ks = info.keySet
      if (ks.isEmpty) {
        postCommit.foreach(f => tryo(f.apply()))
    
        _postCommitFuncs.remove
        threadStore.remove
      } else {
        ks.foreach(n => releaseConnectionNamed(n, !success))
        clearThread(success)
      }
    }
    
    protected def releaseConnectionNamed(name: ConnectionIdentifier, rollback: Boolean) {
      logger.trace("Request to release connection: " + name + " on thread " + Thread.currentThread)
      (info.get(name): @unchecked) match {
        case Some(ConnectionHolder(c, 1, post)) =>
          if (rollback) tryo{c.rollback}
          else c.commit
          tryo(c.releaseFunc())
          info -= name
          post.reverse.foreach(f => tryo(f()))
          logger.trace("Released connection " + name + " on thread " + Thread.currentThread)
          
        case Some(ConnectionHolder(c, n, post)) =>
          logger.trace("Did not release connection: " + name + " on thread " + Thread.currentThread + " count " + (n - 1))
          info(name) = ConnectionHolder(c, n - 1, post)
          
        case _ => // ignore
      }
    }
    
    protected object currentConn extends DynoVar[SuperConnection]
    def currentConnection: Box[SuperConnection] = currentConn.is
    
    
  }
  
}}}