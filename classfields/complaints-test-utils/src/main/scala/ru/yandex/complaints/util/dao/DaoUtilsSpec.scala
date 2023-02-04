package ru.yandex.complaints.util.dao

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.dao.DaoUtils

/**
  * Created by s-reznick on 15.03.17.
  */
@RunWith(classOf[JUnitRunner])
class DaoUtilsSpec extends WordSpec with Matchers {
  private def baseChecks(s: String): Unit = {
    val chars = s.toCharArray
    assert(s.length >= 2)
    assert(chars(0) == '(')
    assert(chars(s.length - 1) == ')')
  }
  private def dropParens(s: String) = s.trim.substring(1, s.length - 1)

  private val Elems = Seq("s", "s123", "id")
  private val WrongElems = Seq("", "?", "i d", "id1,id2", "id=5")

  val wrongSeqs: Seq[Seq[String]] = {
    val base = WrongElems.map(Seq(_))

    base ++ base.map(_ ++ Elems) ++ base.map(Elems ++ _)
  }

  "DaoUtils.names" should {
    "properly work on empty seq" in {
      val s = DaoUtils.names(Seq())
      baseChecks(s)
      assert(dropParens(s).forall(_.isSpaceChar))
    }

    "properly propely work on single elem" in {
      Elems.foreach(e ⇒ {
        val s = DaoUtils.names(Seq(e))
        baseChecks(s)
        assert(dropParens(s).trim == e)
      })
    }

    "properly work on many elems" in {
      val s = DaoUtils.names(Elems)
      baseChecks(s)
      val splitted = dropParens(s).split(",").map(_.trim)
      assert(splitted.length == Elems.size)
      assert(splitted.zip(Elems).forall(e => e._1 == e._2))
    }

    "properly throws exception on illegal parameter" in {
      for (wrong <- wrongSeqs) {
        assertThrows[IllegalArgumentException] {
          DaoUtils.names(wrong)
        }
      }
    }
  }

  "DaoUtils.placeholders" should {
    "properly work on empty seq" in {
      val s = DaoUtils.placeholders(Seq())
      baseChecks(s)
      assert(dropParens(s).forall(_.isSpaceChar))
    }

    "properly propely work on single elem" in {
      Elems.foreach(e ⇒ {
        val s = DaoUtils.placeholders(Seq(e))
        baseChecks(s)
        assert(dropParens(s).trim == "?")
      })
    }

    "properly work on many elems" in {
      val s = DaoUtils.placeholders(Elems)
      baseChecks(s)
      val splitted = dropParens(s).split(",").map(_.trim)
      assert(splitted.length == Elems.size)
      assert(splitted.forall(_ == "?"))
    }
  }

  "DaoUtils.setters" should {
    def checkSetter(setter: String, fname: String) = {
      val elems = setter.split("=").map(_.trim).toList
      assert(elems.size == 2)
      assert(elems.head == fname)
      assert(elems(1) == "?")
    }

    "properly work on empty seq" in {
      val s = DaoUtils.setters(Seq())
      assert(s.forall(_.isSpaceChar))
    }

    "properly propely work on single elem" in {
      Elems.foreach(e ⇒ {
        val s = DaoUtils.setters(Seq(e))
        checkSetter(s, e)
      })
    }

    "properly work on many elems" in {
      val s = DaoUtils.setters(Elems)
      val splitted = s.split(",").map(_.trim)
      assert(splitted.length == Elems.size)
      splitted.zip(Elems).foreach(e => checkSetter(e._1, e._2))
    }

    "properly throws exception on illegal parameter" in {
      for (wrong <- wrongSeqs) {
        assertThrows[IllegalArgumentException] {
          DaoUtils.setters(wrong)
        }
      }
    }
  }
}