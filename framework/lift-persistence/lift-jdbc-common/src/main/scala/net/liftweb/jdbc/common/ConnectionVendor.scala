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
  
  import _root_.java.sql.{Connection,DriverManager}
  import _root_.net.liftweb.common.{Box,Empty,Full,Logger}
  import _root_.net.liftweb.util.Helpers.tryo
  
  /**
   * The standard DB vendor.
   * @param driverName the name of the database driver
   * @param dbUrl the URL for the JDBC data connection
   * @param dbUser the optional username
   * @param dbPassword the optional db password
   */
  class StandardDBVendor(driverName: String, dbUrl: String, dbUser: Box[String],
          dbPassword: Box[String]) extends ProtoDBVendor {

    protected def createOne: Box[Connection] = try {
      Class.forName(driverName)

      val dm = (dbUser, dbPassword) match {
        case (Full(user), Full(pwd)) =>
          DriverManager.getConnection(dbUrl, user, pwd)

        case _ => DriverManager.getConnection(dbUrl)
      }
      Full(dm)
    } catch {
      case e: Exception => e.printStackTrace; Empty
    }
  }
  
  /** 
   * Prototypical database connection manager. Extend this to create 
   * your own pooling mechinihsm or just use the StandardDBVendor (advisable)
   */
  trait ProtoDBVendor extends ConnectionManager {
    private val logger = Logger(classOf[ProtoDBVendor])
    private var pool: List[Connection] = Nil
    private var poolSize = 0
    private var tempMaxSize = maxPoolSize

    /**
     * Override and set to false if the maximum pool size can temporarilly be expanded to avoid pool starvation
     */
    protected def allowTemporaryPoolExpansion = true

    /**
     *  Override this method if you want something other than
     * 4 connections in the pool
     */
    protected def maxPoolSize = 4

    /**
     * The absolute maximum that this pool can extend to
     * The default is 20.  Override this method to change.
     */
    protected def doNotExpandBeyond = 20

    /**
     * The logic for whether we can expand the pool beyond the current size.  By
     * default, the logic tests allowTemporaryPoolExpansion &amp;&amp; poolSize &lt;= doNotExpandBeyond
     */
    protected def canExpand_? : Boolean = allowTemporaryPoolExpansion && poolSize <= doNotExpandBeyond

    /**
     *   How is a connection created?
     */
    protected def createOne: Box[Connection]

    /**
     * Test the connection.  By default, setAutoCommit(false),
     * but you can do a real query on your RDBMS to see if the connection is alive
     */
    protected def testConnection(conn: Connection) {
      conn.setAutoCommit(false)
    }

    def newConnection(name: ConnectionIdentifier): Box[Connection] =
      synchronized {
        pool match {
          case Nil if poolSize < tempMaxSize =>
            val ret = createOne
            ret.foreach(_.setAutoCommit(false))
            poolSize = poolSize + 1
            logger.debug("Created new pool entry. name=%s, poolSize=%d".format(name, poolSize))
            ret

          case Nil =>
            val curSize = poolSize
            logger.trace("No connection left in pool, waiting...")
            wait(50L)
            // if we've waited 50 ms and the pool is still empty, temporarily expand it
            if (pool.isEmpty && poolSize == curSize && canExpand_?) {
              tempMaxSize += 1
              logger.debug("Temporarily expanding pool. name=%s, tempMaxSize=%d".format(name, tempMaxSize))
            }
            newConnection(name)

          case x :: xs =>
            logger.trace("Found connection in pool, name=%s".format(name))
            pool = xs
            try {
              this.testConnection(x)
              Full(x)
            } catch {
              case e => try {
                logger.debug("Test connection failed, removing connection from pool, name=%s".format(name))
                poolSize = poolSize - 1
                tryo(x.close)
                newConnection(name)
              } catch {
                case e => newConnection(name)
              }
            }
        }
      }

    def releaseConnection(conn: Connection): Unit = synchronized {
      if (tempMaxSize > maxPoolSize) {
        tryo {conn.close()}
        tempMaxSize -= 1
        poolSize -= 1
      } else {
        pool = conn :: pool
      }
      logger.debug("Released connection. poolSize=%d".format(poolSize))
      notifyAll
    }

    def closeAllConnections_!(): Unit = synchronized {
      logger.info("Closing all connections")
      if (poolSize == 0) ()
      else {
        pool.foreach {c => tryo(c.close); poolSize -= 1}
        pool = Nil

        if (poolSize > 0) wait(250)

        closeAllConnections_!()
      }
    }
    
  }
  
}}}