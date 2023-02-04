package ru.yandex.common.tokenization

import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import ru.yandex.common.ZooKeeperAware

import scala.util.{Success, Try}

/**
 * Specs on [[OwnershipsClient]]
 */
class OwnershipsClientIntSpec
  extends Matchers
  with WordSpecLike
  with ZooKeeperAware
  with BeforeAndAfter {

  val path = "ownerships"
  val namespace = "ownerships-test"
  val curator = curatorBase.usingNamespace(namespace)
  val client = new OwnershipsClient(curator, "ownerships")

  before {
    Try(curator.delete().deletingChildrenIfNeeded().forPath(s"/$path"))
  }

  "OwnershipsClient" should {
    "once acquire free token" in {
      val ownership = Ownership(Owner("foo"), token = "a")
      client.acquire(ownership) should be(Success(true))
      client.acquire(ownership) should be(Success(false))
    }

    "not acquire already acquired token" in {
      client.acquire(Ownership(Owner("foo"), token = "a")) should be(Success(true))
      client.acquire(Ownership(Owner("bar"), token = "a")) should be(Success(false))
    }

    "not release not exists token" in {
      val ownership = Ownership(Owner("foo"), token = "not-exists-token")
      client.release(ownership) should be(Success(false))
    }

    "not release foreign token" in {
      client.acquire(Ownership(Owner("bar"), token = "b")) should be(Success(true))
      client.release(Ownership(Owner("foo"), token = "b")) should be(Success(false))
    }

    "release own token" in {
      val ownership = Ownership(Owner("foo"), token = "a")
      client.acquire(ownership) should be(Success(true))
      client.release(ownership) should be(Success(true))
    }

    "steal token" in {
      val ownership = Ownership(Owner("foo"), token = "a")
      val newOwnership = Ownership(Owner("bar"), token = "a", 2)
      client.acquire(ownership) should be(Success(true))
      client.steal(ownership, newOwnership) should be(Success(true))
    }

    "not steal not-existed token" in {
      val newOwnership = Ownership(Owner("bar"), token = "a", 2)
      client.steal(Ownership(Owner("foo"), token = "a"), newOwnership) should be(Success(false))
    }

    "steal own token" in {
      val ownership = Ownership(Owner("foo"), token = "a")
      val newOwnership = ownership.copy(cost = 2)
      client.acquire(ownership) should be(Success(true))
      client.steal(ownership, newOwnership) should be(Success(true))
    }

    "steal token from not its owner" in {
      val newOwnership = Ownership(Owner("zoo"), token = "a", 2)
      client.acquire(Ownership(Owner("foo"), token = "a")) should be(Success(true))
      client.steal(Ownership(Owner("bar"), token = "a"), newOwnership) should be(Success(false))
    }
  }

}
