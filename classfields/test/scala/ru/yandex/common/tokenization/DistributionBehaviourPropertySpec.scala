package ru.yandex.common.tokenization

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.common.tokenization.DistributionBehaviour.Action
import ru.yandex.common.tokenization.DistributionBehaviour.Action.{Acquire, Replace}
import ru.yandex.common.tokenization.OwnerConfig.constantCost

/**
  * @author evans
  */
trait DistributionBehaviourPropertySpec
    extends Matchers
        with WordSpecLike
        with PropertyChecks {

  def ownershipsGen(tokens: Tokens, ownerConfigs: Seq[OwnerConfig]): Gen[Ownerships] = for {
    acquiredTokens <- Gen.someOf(tokens.toSet)
    ownerConfigs <- Gen.listOfN(acquiredTokens.size, Gen.oneOf(ownerConfigs))
    ownerships = acquiredTokens.zip(ownerConfigs).map {
      case (token, ownerConfig) => Ownership(ownerConfig.owner, token, ownerConfig.cost(token))
    }
  } yield Ownerships(ownerships.toSet, tokens)

  def behaviourForOwnerConfig(ownerConfig: OwnerConfig): DistributionBehaviour

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 10000)

  def check(ownerships: Ownerships, ownerConfigs: Seq[OwnerConfig], checkGlobalOptimum: Boolean) = {
    val myOwnerConfig = ownerConfigs.head
    val behaviour = new CostDistributionBehaviour(myOwnerConfig)

    val otherOwnerConfigs = ownerConfigs.tail
    val actualOpt = behaviour.decide(ownerships, None)
    actualOpt match {
      case Some(action) =>
        val nextOwnerships = withAction(ownerships, action)
        if (!isConverge(ownerships, nextOwnerships)) {
          fail("New ownerships worse than previous")
        }
      case None if checkGlobalOptimum =>
        val isTerminal =
          otherOwnerConfigs
              .flatMap(config => behaviourForOwnerConfig(config).decide(ownerships, None)).isEmpty
        if (isTerminal && !isOptimal(ownerships, ownerConfigs)) {
          fail("Non optimal ownerships")
        }
      case _ =>
    }
  }

  "Distribution behaviour property" should {
    "distribute equal tokens" in {
      val tokens = new IntTokens(8)
      val ownerConfigs = 1.to(3).map(o => OwnerConfig(Owner(o.toString), constantCost(1)))
      forAll(ownershipsGen(tokens, ownerConfigs)) {
        ownerships: Ownerships =>
          check(ownerships, ownerConfigs, checkGlobalOptimum = true)
      }
    }
  }

  def isOptimal(ownerships: Ownerships, ownerConfigs: Seq[OwnerConfig]) = {
    val optimalCost = ownerships.tokens.toSet.toList.map {
      token =>
        ownerConfigs.map(_.cost(token)).min
    }.sum

    freeTokensMetric(ownerships) == 0 && totalCostMetric(ownerships) == optimalCost
  }

  def withAction(ownerships: Ownerships, action: Action): Ownerships = action match {
    case Acquire(reason, newOwnership) =>
      ownerships.copy(values = ownerships.values + newOwnership)
    case Replace(ownership, newOwnership) =>
      ownerships.copy(values = ownerships.values - ownership + newOwnership)
  }

  def freeTokensMetric(ownerships: Ownerships) =
    ownerships.freeTokens.size

  def totalCostMetric(ownerships: Ownerships) =
    ownerships.toList.map(_.cost).sum

  /**
    * Check is metric optimized during step.
    * Our metric: (freeTokens, totalCost over all owners, variance in total cost between owners)
    */
  def isConverge(prevOwnerships: Ownerships, newOwnerships: Ownerships) = {
    val ownerCount = (prevOwnerships.allOwners ++ newOwnerships).size

    def varianceMetric(ownerships: Ownerships) = {
      def variance() = {
        val costMap = ownerships.groupBy(_.owner).mapValues(_.toList.map(_.cost).sum)
        val mean = costMap.values.sum / ownerCount
        val squareMean = costMap.values.map(math.pow(_, 2)).sum / ownerCount
        squareMean - math.pow(mean, 2)
      }
      if (ownerships.isEmpty) {
        Double.MaxValue
      } else {
        variance()
      }

    }

    def asMetric(ownerships: Ownerships) =
      (freeTokensMetric(ownerships), totalCostMetric(ownerships), varianceMetric(ownerships))

    val ordering = Ordering.by(asMetric)

    ordering.gt(prevOwnerships, newOwnerships)
  }

}
