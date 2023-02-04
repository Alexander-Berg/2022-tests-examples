package ru.yandex.common.tokenization

import org.scalatest.{Matchers, WordSpecLike}

/**
 * Spec on [[OwnershipsDiff]]
 */
class OwnershipsDiffSpec
  extends Matchers
  with WordSpecLike {

  "OwnershipsDiff" should {
    val tokens = new IntTokens(16)

    "be empty for equal ownerships" in {
      val ownerships = Ownerships(
        Set(ownership("foo", "1"), ownership("foo", "2"), ownership("bar", "3")),
        tokens
      )
      OwnershipsDiff(ownerships, ownerships) should be(OwnershipsDiff.empty)
    }

    "have acquired token" in {
      val before = Ownerships(
        Set(ownership("foo", "1"), ownership("foo", "2"), ownership("bar", "3")),
        tokens
      )
      val acquired = ownership("bar", "4")
      val after = before.acquire(acquired)
      OwnershipsDiff(before, after) should be(OwnershipsDiff(Set(acquired), Set.empty[Ownership]))
    }

    "have released token" in {
      val before = Ownerships(
        Set(ownership("foo", "1"), ownership("foo", "2"), ownership("bar", "3")),
        tokens
      )
      val released = ownership("foo", "1")
      val after = before.release(released)
      OwnershipsDiff(before, after) should be(OwnershipsDiff(Set.empty[Ownership], Set(released)))
    }

    "have acquired and released tokens" in {
      val before = Ownerships(
        Set(ownership("foo", "1"), ownership("foo", "2"), ownership("bar", "3")),
        tokens
      )
      val after = before.acquire(ownership("bar", "1"))
      val expectedDiff = OwnershipsDiff(Set(ownership("bar", "1")), Set(ownership("foo", "1")))
      OwnershipsDiff(before, after) should be(expectedDiff)
    }
  }

  private def ownership(id: String, token: Token) = Ownership(Owner(id), token, 1)

}
