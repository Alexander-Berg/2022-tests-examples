package ru.yandex.vertis.subscriptions.core

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

import ru.yandex.vertis.subscriptions.DSL._

/**
  * Specs on common function for package
  */
@RunWith(classOf[JUnitRunner])
class Spec extends WordSpec with Matchers {

  "Query fingerprint" should {
    "be commutative for AND" in {
      val t1 = term(point("key1", "value1"))
      val t2 = term(point("key2", "value2"))
      assert(queryFingerprint(and(t1, t2)) === queryFingerprint(and(t2, t1)))
    }

    "be commutative for OR" in {
      val t1 = term(point("key1", "value1"))
      val t2 = term(point("key2", "value2"))
      assert(queryFingerprint(or(t1, t2)) === queryFingerprint(or(t2, t1)))
    }

    "be different in" in {
      val t1 = term(point("key1", "value1"))
      val t2 = term(point("key2", "value2"))
      assert(queryFingerprint(or(t1, t2)) != queryFingerprint(and(t1, t2)))
    }
  }

}
