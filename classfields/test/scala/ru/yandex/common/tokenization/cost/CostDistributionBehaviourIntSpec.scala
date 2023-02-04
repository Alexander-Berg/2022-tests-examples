package ru.yandex.common.tokenization.cost

import org.scalacheck.Gen
import org.scalatest.Ignore
import ru.yandex.common.tokenization.DistributionBehaviour.AcquireReasons
import ru.yandex.common.tokenization.DistributionBehaviour.Action.{Acquire, Replace}
import ru.yandex.common.tokenization.OwnerConfig.constantCost
import ru.yandex.common.tokenization._

/**
  * @author evans
  */
class CostDistributionBehaviourIntSpec
    extends DistributionBehaviourPropertySpec
        with DistributionBehaviourSpec {

  override def behaviourForOwnerConfig(ownerConfig: OwnerConfig): DistributionBehaviour =
    new CostDistributionBehaviour(ownerConfig)

  "Cost distribution behaviour property" should {
    "distribute pseudo dc tokens" in {
      val tokens = new IntTokens(3)
      val ownerConfigs = 1.to(3).map {
        o =>
          def cost(t: String) = if (t.toInt % 3 == o) 1 else 1000
          OwnerConfig(Owner(o.toString), cost)
      }
      forAll(ownershipsGen(tokens, ownerConfigs)) {
        ownerships: Ownerships =>
          check(ownerships, ownerConfigs, checkGlobalOptimum = true)
      }
    }
    "distribute arbitrary cost tokens" in {
      val tokens = new IntTokens(9)
      val ownerIdGen = Gen.choose(1, 1000)
      val ownerConfigsGen = for {
        ownersCount <- Gen.choose(1, 10)
        ownerIds <- Gen.listOfN(ownersCount, ownerIdGen)
        ownerConfigs = ownerIds.distinct.map {
          o =>
            def cost(t: String) = (o + t.toInt).toString.hashCode % 1000
            OwnerConfig(Owner(o.toString), cost)
        }
      } yield ownerConfigs

      val ownerConfigs = ownerConfigsGen.sample.get
      forAll(ownershipsGen(tokens, ownerConfigs)) {
        ownerships: Ownerships =>
          check(ownerships, ownerConfigs, checkGlobalOptimum = false)
      }
    }
  }

  "Cost distribution behaviour" should {
    "distribute free token if i haven't enough" in {
      val ownerConfig = OwnerConfig(Owner("me"), constantCost(1))
      val behaviour = behaviourForOwnerConfig(ownerConfig)
      val ownerships = Ownerships(
        Set(ownership("foo", "1"), ownership("foo", "2"), ownership("me", "3")),
        tokens
      )
      val actual = behaviour.decide(ownerships, None)
      actual shouldEqual Some(Acquire(AcquireReasons.OwnerlessTokens, ownership("me", "0")))
    }

    "steal last token" in {
      val ownerConfig = OwnerConfig(Owner("me"), constantCost(1))
      val behaviour = behaviourForOwnerConfig(ownerConfig)
      val ownerships = Ownerships(
        Set(
          ownership("foo", "1", 1000),
          ownership("me", "2"),
          ownership("me", "3"),
          ownership("me", "0")
        ),
        tokens
      )
      val actual = behaviour.decide(ownerships, None)
      actual shouldEqual Some(Replace(ownership("foo", "1", 1000), ownership("me", "1")))
    }

    "don't do anything" in {
      val ownerConfig = OwnerConfig(Owner("me"), constantCost(1000))
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
      actual shouldEqual None
    }
  }
}
