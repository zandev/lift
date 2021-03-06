/*
 * Copyright 2009-2010 WorldWide Conferencing, LLC
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
package json {

object JsonAST {
  import scala.text.{Document, DocText}
  import scala.text.Document._

  /** Concatenates a sequence of <code>JValue</code>s.
   * <p>
   * Example:<pre>
   * concat(JInt(1), JInt(2)) == JArray(List(JInt(1), JInt(2)))
   * </pre>
   */
  def concat(xs: JValue*) = xs.foldLeft(JNothing: JValue)(_ ++ _)

  /**
   * Data type for Json AST.
   */
  sealed abstract class JValue extends Merge.Mergeable with Diff.Diffable with Product {
    type Values

    /** XPath-like expression to query JSON fields by name. Matches only fields on
     * next level.
     * <p>
     * Example:<pre>
     * json \ "name"
     * </pre>
     */
    def \(nameToFind: String): JValue = {
      val p = (json: JValue) => json match {
        case JField(name, value) if name == nameToFind => true
        case _ => false
      }
      findDirect(children, p) match {
        case Nil => JNothing
        case x :: Nil => x
        case x => JArray(x)
      }
    }
    
    /**
     * Returns the element as a JValue of the specified class.
     * <p>
     * Example:<pre>
     * (json \ "foo" --> classOf[JField]).value
     * </pre>
     */
    def --> [A <: JValue](clazz: Class[A]): A = (this -->? clazz).getOrElse(error("Expected class " + clazz + ", but found: " + this.getClass))
    
    /**
     * Returns the element as an option of a JValue of the specified class.
      * <p>
      * Example:<pre>
      * (json \ "foo" -->? classOf[JField]).map(_.value).getOrElse(defaultFieldValue)
      * </pre>
     */
    def -->? [A <: JValue](clazz: Class[A]): Option[A] = {
      def extractTyped(value: JValue) = if (value.getClass == clazz) Some(value.asInstanceOf[A]) else None
      
      this match {
        case JField(name, value) if (clazz != classOf[JField]) => extractTyped(value)
        case _ => extractTyped(this)
      }
    }

    private def findDirect(xs: List[JValue], p: JValue => Boolean): List[JValue] = xs.flatMap {
      case JObject(l) => l.filter {
        case x if p(x) => true
        case _ => false
      }
      case JArray(l) => findDirect(l, p)
      case x if p(x) => x :: Nil
      case _ => Nil
    }

    /** XPath-like expression to query JSON fields by name. Returns all matching fields.
     * <p>
     * Example:<pre>
     * json \\ "name"
     * </pre>
     */
    def \\(nameToFind: String): JValue = {
      def find(json: JValue): List[JField] = json match {
        case JObject(l) => l.foldLeft(List[JField]())((a, e) => a ::: find(e))
        case JArray(l) => l.foldLeft(List[JField]())((a, e) => a ::: find(e))
        case field @ JField(name, value) if name == nameToFind => field :: find(value)
        case JField(_, value) => find(value)
        case _ => Nil
      }
      find(this) match {
        case x :: Nil => x
        case x => JObject(x)
      }
    }

    /** XPath-like expression to query JSON fields by type. Matches only fields on
     * next level.
     * <p>
     * Example:<pre>
     * json \ classOf[JInt]
     * </pre>
     */
    def \[A <: JValue](clazz: Class[A]): List[A#Values] =
      findDirect(children, typePredicate(clazz) _).asInstanceOf[List[A]] map { _.values }

    /** XPath-like expression to query JSON fields by type. Returns all matching fields.
     * <p>
     * Example:<pre>
     * json \\ classOf[JInt]
     * </pre>
     */
    def \\[A <: JValue](clazz: Class[A]): List[A#Values] =
      (this filter typePredicate(clazz) _).asInstanceOf[List[A]] map { _.values }

    private def typePredicate[A <: JValue](clazz: Class[A])(json: JValue) = json match {
      case x if x.getClass == clazz => true
      case _ => false
    }

    /** Return nth element from JSON.
     * Meaningful only to JArray, JObject and JField. Returns JNothing for other types.
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil)(1) == JInt(2)
     * </pre>
     */
    def apply(i: Int): JValue = JNothing

    /** Return unboxed values from JSON
     * <p>
     * Example:<pre>
     * JObject(JField("name", JString("joe")) :: Nil).values == Map("name" -> "joe")
     * </pre>
     */
    def values: Values

    /** Return direct child elements.
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil).children == List(JInt(1), JInt(2))
     * </pre>
     */
    def children = this match {
      case JObject(l) => l
      case JArray(l) => l
      case JField(n, v) => List(v)
      case _ => Nil
    }

    /** Return a combined value by folding over JSON by applying a function <code>f</code>
     * for each element. The initial value is <code>z</code>.
     */
    def fold[A](z: A)(f: (A, JValue) => A): A = {
      def rec(acc: A, v: JValue) = {
        val newAcc = f(acc, v)
        v match {
          case JObject(l) => l.foldLeft(newAcc)((a, e) => e.fold(a)(f))
          case JArray(l) => l.foldLeft(newAcc)((a, e) => e.fold(a)(f))
          case JField(_, value) => value.fold(newAcc)(f)
          case _ => newAcc
        }
      }
      rec(z, this)
    }

    /** Return a new JValue resulting from applying the given function <code>f</code>
     * to each element in JSON.
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil) map { case JInt(x) => JInt(x+1); case x => x }
     * </pre>
     */
    def map(f: JValue => JValue): JValue = {
      def rec(v: JValue): JValue = v match {
        case JObject(l) => f(JObject(l.map(f => rec(f) match {
          case x: JField => x
          case x => JField(f.name, x)
        })))
        case JArray(l) => f(JArray(l.map(rec)))
        case JField(name, value) => f(JField(name, rec(value)))
        case x => f(x)
      }
      rec(this)
    }

    /** Return a new JValue resulting from applying the given partial function <code>f</code>
     * to each element in JSON.
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil) transform { case JInt(x) => JInt(x+1) }
     * </pre>
     */
    def transform(f: PartialFunction[JValue, JValue]): JValue = map { x =>
      if (f.isDefinedAt(x)) f(x) else x
    }

    /** Return a new JValue resulting from replacing the value at the specified field
     * path with the replacement value provided. This has no effect if the path is empty
     * or if the value is not a JObject instance.
     * <p>
     * Example:<pre>
     * JObject(List(JField("foo", JObject(List(JField("bar", JInt(1))))))).replace("foo" :: "bar" :: Nil, JString("baz"))
     * // returns JObject(List(JField("foo", JObject(List(JField("bar", JString("baz")))))))
     * </pre>
     */
    def replace(l: List[String], replacement: JValue): JValue = {
      def rep(l: List[String], in: JValue): JValue = {
        l match {
          case x :: xs => in match {
            case JObject(fields) => JObject(
                fields.map {
                  case JField(`x`, value) => JField(x, if (xs == Nil) replacement else rep(xs, value))
                  case field => field
                }
              )
            case other => other
          }

          case Nil => in
        }
      }

      rep(l, this)
    }

    /** Return the first element from JSON which matches the given predicate.
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil) find { _ == JInt(2) } == Some(JInt(2))
     * </pre>
     */
    def find(p: JValue => Boolean): Option[JValue] = {
      def find(json: JValue): Option[JValue] = {
        if (p(json)) return Some(json)
        json match {
          case JObject(l) => l.flatMap(find _).firstOption
          case JArray(l) => l.flatMap(find _).firstOption
          case JField(_, value) => find(value)
          case _ => None
        }
      }
      find(this)
    }

    /** Return a List of all elements which matches the given predicate.
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil) filter { case JInt(x) => x > 1; case _ => false }
     * </pre>
     */
    def filter(p: JValue => Boolean): List[JValue] =
      fold(List[JValue]())((acc, e) => if (p(e)) e :: acc else acc).reverse

    /** Concatenate with another JSON.
     * This is a concatenation monoid: (JValue, ++, JNothing)
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil) ++ JArray(JInt(3) :: Nil) ==
     * JArray(List(JInt(1), JInt(2), JInt(3)))
     * </pre>
     */
    def ++(other: JValue) = {
      def append(value1: JValue, value2: JValue): JValue = (value1, value2) match {
        case (JNothing, x) => x
        case (x, JNothing) => x
        case (JObject(xs), x: JField) => JObject(xs ::: List(x))
        case (x: JField, JObject(xs)) => JObject(x :: xs)
        case (JArray(xs), JArray(ys)) => JArray(xs ::: ys)
        case (JArray(xs), v: JValue) => JArray(xs ::: List(v))
        case (v: JValue, JArray(xs)) => JArray(v :: xs)
        case (f1: JField, f2: JField) => JObject(f1 :: f2 :: Nil)
        case (JField(n, v1), v2: JValue) => JField(n, append(v1, v2))
        case (x, y) => JArray(x :: y :: Nil)
      }
      append(this, other)
    }

    /** Return a JSON where all elements matching the given predicate are removed.
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: JNull :: Nil) remove { _ == JNull }
     * </pre>
     */
    def remove(p: JValue => Boolean): JValue = this map {
      case x if p(x) => JNothing
      case x => x
    }

    /** Extract a case class from a JSON.
     * <p>
     * Example:<pre>
     * case class Person(name: String)
     * JObject(JField("name", JString("joe")) :: Nil).extract[Foo] == Person("joe")
     * </pre>
     */
    def extract[A](implicit formats: Formats, mf: scala.reflect.Manifest[A]): A = 
      Extraction.extract(this)(formats, mf)

    /** Extract a case class from a JSON.
     * <p>
     * Example:<pre>
     * case class Person(name: String)
     * JObject(JField("name", JString("joe")) :: Nil).extractOpt[Foo] == Some(Person("joe"))
     * </pre>
     */
    def extractOpt[A](implicit formats: Formats, mf: scala.reflect.Manifest[A]): Option[A] = 
      Extraction.extractOpt(this)(formats, mf)
  }

  case object JNothing extends JValue {
    type Values = None.type
    def values = None
  }
  case object JNull extends JValue {
    type Values = Null
    def values = null
  }
  case class JBool(value: Boolean) extends JValue {
    type Values = Boolean
    def values = value
  }
  case class JInt(num: BigInt) extends JValue {
    type Values = BigInt
    def values = num
  }
  case class JDouble(num: Double) extends JValue {
    type Values = Double
    def values = num
  }
  case class JString(s: String) extends JValue {
    type Values = String
    def values = s
  }
  case class JField(name: String, value: JValue) extends JValue {
    type Values = (String, value.Values)
    def values = (name, value.values)
    override def apply(i: Int): JValue = value(i)
  }
  case class JObject(obj: List[JField]) extends JValue {
    type Values = Map[String, Any]
    def values = Map() ++ obj.map(_.values : (String, Any))
    
    override def equals(that: Any): Boolean = that match {
      case o: JObject => Set(obj.toArray: _*) == Set(o.obj.toArray: _*)
      case _ => false
    }
  }
  case class JArray(arr: List[JValue]) extends JValue {
    type Values = List[Any]
    def values = arr.map(_.values)
    override def apply(i: Int): JValue = arr(i)
    
    override def equals(that: Any): Boolean = that match {
      case a: JArray => Set(arr.toArray: _*) == Set(a.arr.toArray: _*)
      case _ => false
    }
  }

  /** Renders JSON.
   * @see Printer#compact
   * @see Printer#pretty
   */
  def render(value: JValue): Document = value match {
    case null          => text("null")
    case JBool(true)   => text("true")
    case JBool(false)  => text("false")
    case JDouble(n)    => text(n.toString)
    case JInt(n)       => text(n.toString)
    case JNull         => text("null")
    case JNothing      => error("can't render 'nothing'")
    case JString(null) => text("null")
    case JString(s)    => text("\"" + quote(s) + "\"")
    case JArray(arr)   => text("[") :: series(trimArr(arr).map(render)) :: text("]")
    case JField(n, v)  => text("\"" + n + "\":") :: render(v)
    case JObject(obj)  =>
      val nested = break :: fields(trimObj(obj).map(f => text("\"" + f.name + "\":") :: render(f.value)))
      text("{") :: nest(2, nested) :: break :: text("}")
  }
  
  /** Renders as Scala code, which can be copy/pasted into a lift-json scala 
   * application.
   */
  def renderScala(value: JValue): Document = {  
    val Quote = "\""
    
    def scalaQuote(s: String) = Quote + List("\\t" -> "\\t", "\\f" -> "\\f", "\\r" -> "\\r", "\\n" -> "\\n", "\\\\" -> "\\\\").foldLeft(s) { (str, pair) =>
      str.replaceAll(pair._1, pair._2)
    } + Quote
    
    def intersperse(l: List[Document], i: Document) = l.zip(List.make(l.length - 1, i) ::: List(text(""))).map(t => t._1 :: t._2)
    
    value match {
      case null => text("null")
      
      case JNothing => text("JNothing")
      case JNull => text("JNull")
      
      case _ => text(value.productPrefix + "(") :: (value match {
        case JNull | JNothing => error("impossible")
      
        case JBool(value)  => text(value.toString)
        case JDouble(n)    => text(n.toString)
        case JInt(n)       => text(n.toString)
        case JString(null) => text("null")
        case JString(s)    => text(scalaQuote(s))
        case JArray(arr)   => fold(intersperse(arr.map(renderScala) ::: List(text("Nil")), text("::")))
        case JField(n, v)  => text(scalaQuote(n) + ",") :: renderScala(v)
        case JObject(obj)  =>
          val nested = break :: fold(intersperse(intersperse(obj.map(renderScala) ::: List(text("Nil")), text("::")), break))
        
          nest(2, nested)
      }) :: text(")")
    }
  }

  private def trimArr(xs: List[JValue]) = xs.filter(_ != JNothing)
  private def trimObj(xs: List[JField]) = xs.filter(_.value != JNothing)
  private def series(docs: List[Document]) = fold(punctuate(text(","), docs))
  private def fields(docs: List[Document]) = fold(punctuate(text(",") :: break, docs))
  private def fold(docs: List[Document]) = docs.foldLeft[Document](empty)(_ :: _)

  private def punctuate(p: => Document, docs: List[Document]): List[Document] = {
    def prepend(d: DocText, ds: List[Document]) = ds match {
      case DocText(h) :: t => DocText(h + d.txt) :: t
      case _ => d :: ds
    }

    def punctuate0(docs: List[Document], acc: List[Document]): List[Document] = docs match {
      case Nil => acc.reverse
      case List(d) => punctuate0(Nil, d :: acc)
      case DocText(d) :: ds => p match {
        case DocText(punct) => punctuate0(ds, prepend(DocText(d + punct), acc))
        case _ => punctuate0(ds, (d :: p) :: acc)
      }
      case d :: ds => punctuate0(ds, (d :: p) :: acc)
    }
    punctuate0(docs, Nil)
  }

  private[json] def quote(s: String): String = {
    val buf = new StringBuilder
    for (i <- 0 until s.length) {
      val c = s.charAt(i)
      buf.append(c match {
        case '"'  => "\\\""
        case '\\' => "\\\\"
        case '\b' => "\\b"
        case '\f' => "\\f"
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case c if ((c >= '\u0000' && c < '\u001f') || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) => "\\u%04x".format(c: Int)
        case c => c
      })
    }
    buf.toString
  }
}

/** Basic implicit conversions from primitive types into JSON.
 * Example:<pre>
 * import net.liftweb.json.Implicits._
 * JObject(JField("name", "joe") :: Nil) == JObject(JField("name", JString("joe")) :: Nil)
 * </pre>
 */
object Implicits extends Implicits
trait Implicits {
  import JsonAST._

  implicit def int2jvalue(x: Int) = JInt(x)
  implicit def long2jvalue(x: Long) = JInt(x)
  implicit def bigint2jvalue(x: BigInt) = JInt(x)
  implicit def double2jvalue(x: Double) = JDouble(x)
  implicit def float2jvalue(x: Float) = JDouble(x)
  implicit def bigdecimal2jvalue(x: BigDecimal) = JDouble(x.doubleValue)
  implicit def boolean2jvalue(x: Boolean) = JBool(x)
  implicit def string2jvalue(x: String) = JString(x)
}

/** A DSL to produce valid JSON.
 * Example:<pre>
 * import net.liftweb.json.JsonDSL._
 * ("name", "joe") ~ ("age", 15) == JObject(JField("name",JString("joe")) :: JField("age",JInt(15)) :: Nil)
 * </pre>
 */
object JsonDSL extends JsonDSL with Printer
trait JsonDSL extends Implicits {
  import JsonAST._

  implicit def seq2jvalue[A <% JValue](s: Seq[A]) = JArray(s.toList.map { a => val v: JValue = a; v })
  implicit def option2jvalue[A <% JValue](opt: Option[A]): JValue = opt match {
    case Some(x) => x
    case None => JNothing
  }

  implicit def symbol2jvalue(x: Symbol) = JString(x.name)
  implicit def pair2jvalue[A <% JValue](t: (String, A)) = JObject(List(JField(t._1, t._2)))
  implicit def list2jvalue(l: List[JField]) = JObject(l)
  implicit def jobject2assoc(o: JObject) = new JsonListAssoc(o.obj)
  implicit def pair2Assoc[A <% JValue](t: (String, A)) = new JsonAssoc(t)

  class JsonAssoc[A <% JValue](left: (String, A)) {
    def ~[B <% JValue](right: (String, B)) = {
      val l: JValue = left._2
      val r: JValue = right._2
      JObject(JField(left._1, l) :: JField(right._1, r) :: Nil)
    }

    def ~(right: JObject) = {
      val l: JValue = left._2
      JObject(JField(left._1, l) :: right.obj)
    }
  }

  class JsonListAssoc(left: List[JField]) {
    def ~(right: (String, JValue)) = JObject(left ::: List(JField(right._1, right._2)))
    def ~(right: JObject) = JObject(left ::: right.obj)
  }
}

/** Printer converts JSON to String.
 * Before printing a <code>JValue</code> needs to be rendered into scala.text.Document.
 * <p>
 * Example:<pre>
 * pretty(render(json))
 * </pre>
 *
 * @see net.liftweb.json.JsonAST#render
 */
object Printer extends Printer
trait Printer {
  import java.io._
  import java.util.IdentityHashMap
  import scala.text._
  import scala.collection.immutable.Stack

  /** Compact printing (no whitespace etc.)
   */
  def compact(d: Document): String = compact(d, new StringWriter).toString

  /** Compact printing (no whitespace etc.)
   */
  def compact[A <: Writer](d: Document, out: A): A = {
    // Non-recursive implementation to support serialization of big structures.
    var nodes = Stack.Empty.push(d)
    val visited = new IdentityHashMap[Document, Unit]()
    while (!nodes.isEmpty) {
      val cur = nodes.top
      nodes = nodes.pop
      cur match {
        case DocText(s)      => out.write(s)
        case DocCons(d1, d2) => 
          if (!visited.containsKey(cur)) {
            visited.put(cur, ())
            nodes = nodes.push(cur)
            nodes = nodes.push(d1)
          } else {
            nodes = nodes.push(d2)
          }
        case DocBreak        =>
        case DocNest(_, d)   => nodes = nodes.push(d)
        case DocGroup(d)     => nodes = nodes.push(d)
        case DocNil          =>
      }
    }
    out.flush
    out
  }

  /** Pretty printing.
   */
  def pretty(d: Document): String = pretty(d, new StringWriter).toString

  /** Pretty printing.
   */
  def pretty[A <: Writer](d: Document, out: A): A = {
    d.format(0, out)
    out
  }
}

}
}
