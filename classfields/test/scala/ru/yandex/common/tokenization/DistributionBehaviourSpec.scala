package ru.yandex.common.tokenization

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.common.tokenization.DistributionBehaviour.AcquireReasons
import ru.yandex.common.tokenization.DistributionBehaviour.Action.{Acquire, Replace}
import ru.yandex.common.tokenization.OwnerConfig.constantCost

/**
  * @author evans
  */
trait DistributionBehaviourSpec
    extends Matchers
        with WordSpecLike {

  val tokens = new IntTokens(4)

  def behaviourForOwnerConfig(ownerConfig: OwnerConfig): DistributionBehaviour

  "Distribution behaviour" should {
    "distribute free token if i haven't tokens" in {
      val ownerConfig = OwnerConfig(Owner("me"), constantCost(1))
      val behaviour = behaviourForOwnerConfig(ownerConfig)
      val ownerships = Ownerships(
        Set(ownership("foo", "1"), ownership("foo", "2"), ownership("bar", "3")),
        tokens
      )
      val actual = behaviour.decide(ownerships, None)
      actual shouldEqual Some(Acquire(AcquireReasons.HaveNothing, ownership("me", "0")))
    }

    "distribute free token if i am single" in {
      val ownerConfig = OwnerConfig(Owner("me"), constantCost(1))
      val behaviour = behaviourForOwnerConfig(ownerConfig)
      val ownerships = Ownerships(
        Set(ownership("me", "1"), ownership("me", "2"), ownership("me", "3")),
        tokens
      )
      val actual = behaviour.decide(ownerships, None)
      actual shouldEqual Some(Acquire(AcquireReasons.Single, ownership("me", "0")))
    }

    "steal token if i haven't enough" in {
      val ownerConfig = OwnerConfig(Owner("me"), constantCost(1))
      val behaviour = behaviourForOwnerConfig(ownerConfig)
      val ownerships = Ownerships(
        Set(
          ownership("foo", "1"),
          ownership("foo", "2"),
          ownership("foo", "3"),
          ownership("foo", "0")
        ),
        tokens
      )
      val actual = behaviour.decide(ownerships, None)
      actual.get shouldBe an[Replace]
    }
  }

  def ownership(id: String, token: Token, cost: Int = 1) =
    Ownership(Owner(id), token, cost)
}
