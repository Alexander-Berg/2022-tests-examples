package ru.yandex.auto.util

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OfferIdUtilSpec extends FlatSpec {

  import OfferIdUtils._

  "trimmed ids" should "not contain prefixes and suffixes" in {

    val givenIds = List("autoru-123321", "autoru-7649850-f1b2c3d4", "32132442-f1b2c3d4", "7649850", "-")
    val expectedIds = List("autoru-123321", "autoru-7649850", "autoru-32132442", "autoru-7649850", "autoru-")

    assert(givenIds.map(transformId) == expectedIds)
  }

  "extract ids" should "" in {

    val givenIds = Set("autoru-123321", "7649850-f1b2c3d4", "-f1b2c3d4", "123123")
    val expectedIds = Set(Some(123321), Some(7649850), None, Some(123123))

    assert(expectedIds == givenIds.map(longId))
  }
}
