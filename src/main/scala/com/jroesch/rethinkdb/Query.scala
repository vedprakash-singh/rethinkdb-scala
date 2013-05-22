package com.jroesch.rethinkdb

import com.rethinkdb.{ QL2 => Protocol }
import scala.collection.JavaConversions._

/* im pretty sure there is a better place to put these implicits, look at 
 * scalaz */

trait JSON[A] // { def toJson(x: A): String; def fromJson(x: String): A }

object Query {
  implicit object IntJSON extends JSON[Int]

  implicit object DoubleJSON extends JSON[Double]

  implicit object StringJSON extends JSON[String]

  implicit object ArrayJSON extends JSON[Array[_]] //I'm fucked here

  implicit object MapJSON extends JSON[Map[_, _]]
}

trait Query[T] {
  val queryObject: Protocol.Query

  def run(implicit conn: Connection) = {
    //conn.writeQuery(this)
    "dummy result" /* read result from here and output back */
  }

  //def insert(implicit ev: T =:= Table)
  //

  def buildGlobalOptArgs(conn: Connection) = {
    /* import Protocol.{ Datum => _, _ }
    val pair = Query.AssocPair.newBuilder()
    pair.setKey("db")
    val term = Term.newBuilder()
    term.setType(Term.TermType.DB)
    val termArgs = Term.newBuilder()
    termArgs.setType(Term.TermType.DATUM)
    termArgs.setDatum(Datum(conn.db))
    term.setArgs(termArgs.build)
    pair.setValue(term.build)
    pair.build */
  }  

  type AssocPairs = Map[String, Protocol.Term]

  def db(name: String)(implicit evidence: JSON[String]): (String, Protocol.Term) = {
    import Protocol.Term.TermType
    ("db", Term(TermType.DB, None, Term(TermType.DATUM, Some(Datum(name))) :: Nil))
  }

  /* Most Queries will have the type Start, good default */
  def Query(query: Protocol.Term, token: Long, globalOptArgs: AssocPairs): Protocol.Query =
    Query(Protocol.Query.QueryType.START, query, token, globalOptArgs)

  def Query(tpe: Protocol.Query.QueryType, query: Protocol.Term, token: Long, globalOptArgs: AssocPairs): Protocol.Query = {
    val _query = Protocol.Query.newBuilder()
    _query.setType(tpe)
    _query.setQuery(query)
    _query.setToken(token)
    val optargs = globalOptArgs map { case (k, v) =>
      val pair = Protocol.Query.AssocPair.newBuilder()
      pair.setVal(v)
      pair.setKey(k)
      pair.build
    }
    _query.addAllGlobalOptargs(asJavaIterable(optargs))
    _query.build
  }

  def Term(tpe: Protocol.Term.TermType, datum: Option[Protocol.Datum], args: Seq[Protocol.Term] = Nil, optargs: AssocPairs = Map()) = {
    val term = Protocol.Term.newBuilder()
    term.setType(tpe)
    datum foreach { case d => term setDatum d }
    term.addAllArgs(args)
    val oargs = optargs map { case (k, v) =>
      val pair = Protocol.Term.AssocPair.newBuilder()
      pair.setVal(v)
      pair.setKey(k)
      pair.build
    }
    term.addAllOptargs(asJavaIterable(oargs))
    term.build()
  }

  def Datum[A: JSON](d: A) = {
    import Protocol.Datum.DatumType
    val datum = Protocol.Datum.newBuilder()
    d match {
      case null =>
        datum.setType(DatumType.R_NULL)
      case b: Boolean =>
        datum.setType(DatumType.R_BOOL)
      case i: Int =>
        datum.setType(DatumType.R_NUM)
        datum.setRNum(i)
      case d: Double =>
        datum.setType(DatumType.R_NUM)
        datum.setRNum(d)
      case s: String =>
        datum.setType(DatumType.R_STR)
        datum.setRStr(s)
      case a: Array[_] => ???
      case m: Map[_,_]   => ???
    }

    datum.build
  }
}
  
