package ru.yandex.vertis.curator.util

import org.scalatest.{Matchers, WordSpec}

/**
 * Specs on [[CuratorUtils]].
 *
 * @author dimas
 */
class CuratorUtilsSpec
  extends WordSpec
  with Matchers {

  "CuratorUtils" should {
    "neutralize ZooKeeper path" in {
      import CuratorUtils.{neutralizePath => np}
      np(null) should be("/")
      np("") should be("/")
      np("  ") should be("/")
      np("////") should be("/")
      np("foo") should be("/foo")
      np("foo/") should be("/foo")
      np("//foo/") should be("/foo")
      np("/foo///bar///baz////") should be("/foo/bar/baz")
    }
  }

}
