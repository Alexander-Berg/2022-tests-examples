package ru.yandex.vertis.telepony.service.impl

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.model.Operators.{Beeline, Mts, Mtt, Vox}
import ru.yandex.vertis.telepony.model.PhoneTypes._
import ru.yandex.vertis.telepony.model.StatusValues.{Downtimed, Ready}
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectInspectService.GetResult.{NoRedirects, Suspended}
import ru.yandex.vertis.telepony.service.strategy.ChooseOperatorStrategy.OperatorWrapper
import ru.yandex.vertis.telepony.service.strategy._
import ru.yandex.vertis.telepony.service.{OperatorLabelService, PhoneStatisticsLoader}
import ru.yandex.vertis.telepony.util.{OperatorUtils, Threads}
import ru.yandex.vertis.util.collection.RichMap

import scala.concurrent.Future
import scala.annotation.nowarn

/**
  * @author neron
  */
@nowarn
class ChooseOperatorStrategyImplSpec extends SpecBase with MockitoSupport with ScalaCheckPropertyChecks {

  private val Msk = 1

  private val alwaysAvailable = new OperatorAvailableLoader {
    override def isAvailable: Boolean = true
  }

  private val notAvailable = new OperatorAvailableLoader {
    override def isAvailable: Boolean = false
  }

  private def createEmptyMockedOLS: OperatorLabelService = {
    val m = mock[OperatorLabelService]
    when(m.getUnhealthy).thenReturn(Set.empty[Operator])
    when(m.getSuspended).thenReturn(Set.empty[Operator])
    m
  }

  private val plentyOfNumbers = new PhoneStatisticsLoader {

    override def statusCount(
        operator: Operator,
        originOperator: Operator,
        phoneType: PhoneType,
        geoId: GeoId): StatusCount =
      StatusCount(Map(Ready -> 1000))

    override def statusCount(operator: Operator, phoneType: PhoneType, geoId: GeoId): StatusCount = {
      StatusCount(Map(Ready -> 1000))
    }

  }

  private val MtsOperatorWrapper: OperatorWrapper = OperatorWrapper(Mts, Mts)
  private val MttOperatorWrapper: OperatorWrapper = OperatorWrapper(Mtt, Mtt)
  private val BeelineOperatorWrapper: OperatorWrapper = OperatorWrapper(Beeline, Beeline)
  private val VoxOperatorWrapper: OperatorWrapper = OperatorWrapper(Vox, Vox)
  private val VoxBeelineOperatorWrapper: OperatorWrapper = OperatorWrapper(Vox, Beeline)
  private val VoxMttOperatorWrapper: OperatorWrapper = OperatorWrapper(Vox, Mtt)

  private val DefaultOperatorWrappers: List[OperatorWrapper] = List(
    MtsOperatorWrapper,
    MttOperatorWrapper
  )

  private def mockStatusCount(
      loader: PhoneStatisticsLoader
    )(wrapper: OperatorWrapper,
      statusCount: Map[StatusValue, Int],
      phoneType: PhoneType = Mobile,
      geoId: GeoId = Msk): Unit = {
    when(loader.statusCount(eq(wrapper.operator), eq(wrapper.originOperator), eq(phoneType), eq(geoId)))
      .thenReturn(StatusCount(statusCount))
  }

  private def mockStatusCountReady(
      loader: PhoneStatisticsLoader
    )(wrapper: OperatorWrapper,
      count: Int,
      phoneType: PhoneType = Mobile,
      geoId: GeoId = Msk): Unit = {
    mockStatusCount(loader)(wrapper, Map(Ready -> count), phoneType, geoId)
  }

  private def compositeStrategy(
      psl: PhoneStatisticsLoader = plentyOfNumbers,
      mtsAv: OperatorAvailableLoader = alwaysAvailable,
      mttAv: OperatorAvailableLoader = alwaysAvailable,
      beelineAv: OperatorAvailableLoader = alwaysAvailable,
      percentThreshold: Double = 0.5,
      opLabel: OperatorLabelService = createEmptyMockedOLS,
      wrappers: List[OperatorWrapper] = DefaultOperatorWrappers): CompositeStrategy =
    new CompositeStrategyImpl(
      new PreferredOperatorStrategy,
      new AvailabilityStrategy(psl, mtsAv, mttAv, beelineAv),
      new FairDistributionStrategy(psl, percentThreshold),
      new PreferHealthyStrategy(opLabel),
      new AvoidSuspendedStrategy(opLabel),
      new HasNumbersStrategy(psl),
      new HealthyOperatorStrategy(opLabel),
      new NonSuspendedOperatorStrategy(opLabel),
      wrappers
    )

  def trySeveralTimes(f: => Unit): Unit = {
    (1 to 10).foreach(_ => f)
  }

  import Threads.lightWeightTasksEc

  "decider" when {

    "Mtt small, Mts small" should {
      "choose random" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCount(statLoader)(MttOperatorWrapper, Map(Ready -> 10, Downtimed -> 10000))
        mockStatusCount(statLoader)(MtsOperatorWrapper, Map(Ready -> 100, Downtimed -> 100000))
        val decider = compositeStrategy(statLoader)
        var accMtsScore = 0
        var accMttScore = 0
        eventually {
          val decisionFutures = (1 to 1000).map { _ =>
            decider.decide(Msk, Mobile, NoRedirects)
          }
          val decisions = Future.sequence(decisionFutures).futureValue
          val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
          decisionCount should have size 2
          accMtsScore += decisionCount(List(MtsOperatorWrapper, MttOperatorWrapper))
          accMttScore += decisionCount(List(MttOperatorWrapper, MtsOperatorWrapper))
          (accMttScore.toDouble / accMtsScore) should ===(0.10 +- 0.05)
        }
      }
    }

    "Mtt small, Mts big" should {
      "choose Mts" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCount(statLoader)(MttOperatorWrapper, Map(Ready -> 1, Downtimed -> 2))
        mockStatusCount(statLoader)(MtsOperatorWrapper, Map(Ready -> 1000, Downtimed -> 0))
        val decider = compositeStrategy(statLoader)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, NoRedirects)
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MtsOperatorWrapper, MttOperatorWrapper)) shouldEqual 1000
      }
    }

    "Mtt big, Mts small" should {
      "choose Mtt" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCount(statLoader)(MttOperatorWrapper, Map(Ready -> 1000, Downtimed -> 2))
        mockStatusCount(statLoader)(MtsOperatorWrapper, Map(Ready -> 1, Downtimed -> 10))
        val decider = compositeStrategy(statLoader)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, NoRedirects)
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MttOperatorWrapper, MtsOperatorWrapper)) shouldEqual 1000
      }
    }

    "Mtt big, Mts big" should {
      "choose random" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCount(statLoader)(MttOperatorWrapper, Map(Ready -> 9, Downtimed -> 1))
        mockStatusCount(statLoader)(MtsOperatorWrapper, Map(Ready -> 9, Downtimed -> 1))
        val decider = compositeStrategy(statLoader)
        var accMtsScore = 0
        var accMttScore = 0
        eventually {
          val decisionFutures = (1 to 100).map { _ =>
            decider.decide(Msk, Mobile, NoRedirects)
          }
          val decisions = Future.sequence(decisionFutures).futureValue
          val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
          decisionCount should have size 2
          accMtsScore += decisionCount(List(MtsOperatorWrapper, MttOperatorWrapper))
          accMttScore += decisionCount(List(MttOperatorWrapper, MtsOperatorWrapper))
          (accMttScore.toDouble / accMtsScore) should ===(1.0 +- 0.05)
        }
      }
    }

    "one operator not available" should {
      "choose another if it has ready numbers" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(MttOperatorWrapper, count = 10)
        mockStatusCountReady(statLoader)(MtsOperatorWrapper, count = 100)
        val decider = compositeStrategy(statLoader, mttAv = notAvailable)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, NoRedirects)
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MtsOperatorWrapper)) shouldEqual 1000
      }
      "choose right one vox wrapper by origin only if it has ready numbers" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(VoxOperatorWrapper, count = 0)
        mockStatusCountReady(statLoader)(VoxBeelineOperatorWrapper, count = 10)
        mockStatusCountReady(statLoader)(VoxMttOperatorWrapper, count = 10)
        val wrappers = List(VoxOperatorWrapper, VoxBeelineOperatorWrapper, VoxMttOperatorWrapper)
        val decider = compositeStrategy(statLoader, beelineAv = notAvailable, wrappers = wrappers)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, NoRedirects)
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(VoxMttOperatorWrapper)) shouldEqual 1000
      }
    }

    "preferred operator defined" should {
      "choose this operator" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(MttOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(MtsOperatorWrapper, count = 100)
        val decider = compositeStrategy(statLoader, mtsAv = notAvailable, mttAv = notAvailable)
        val decisionFutures =
          (1 to 1000).map { _ =>
            decider.decide(Msk, Mobile, NoRedirects, preferredOperator = Some(Mtt))
          }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MttOperatorWrapper)) shouldEqual 1000
      }
      "choose vox with origin mtt operator wrapper" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(MttOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(VoxOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(VoxBeelineOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(VoxMttOperatorWrapper, count = 100)
        val wrappers = List(MttOperatorWrapper, VoxOperatorWrapper, VoxBeelineOperatorWrapper, VoxMttOperatorWrapper)
        val decider = compositeStrategy(statLoader, mttAv = notAvailable, beelineAv = notAvailable, wrappers = wrappers)
        val decisionFutures =
          (1 to 1000).map { _ =>
            decider.decide(Msk, Mobile, NoRedirects, preferredOperator = Some(Mtt))
          }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MttOperatorWrapper)) shouldEqual 1000
      }
    }

    "unhealthy operators" should {
      List(false, true).foreach { mtsSuspended =>
        s"choose healthy${if (mtsSuspended) " and suspended" else ""}" in {
          val statLoader = mock[PhoneStatisticsLoader]
          mockStatusCountReady(statLoader)(MttOperatorWrapper, count = 100)
          mockStatusCountReady(statLoader)(MtsOperatorWrapper, count = 10)
          val mockedOLS = createEmptyMockedOLS
          when(mockedOLS.getUnhealthy).thenReturn(Set(Mtt))
          if (mtsSuspended) {
            when(mockedOLS.getSuspended).thenReturn(Set(Mts))
          }
          val decider = compositeStrategy(statLoader, opLabel = mockedOLS)
          val decisionFutures = (1 to 1000).map { _ =>
            decider.decide(Msk, Mobile, NoRedirects)
          }
          val decisions = Future.sequence(decisionFutures).futureValue
          val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
          decisionCount should have size 1
          decisionCount(List(MtsOperatorWrapper, MttOperatorWrapper)) shouldEqual 1000
        }
        s"choose healthy${if (mtsSuspended) " and suspended" else ""} by origin operator" in {
          val statLoader = mock[PhoneStatisticsLoader]
          mockStatusCountReady(statLoader)(VoxMttOperatorWrapper, count = 100)
          mockStatusCountReady(statLoader)(VoxBeelineOperatorWrapper, count = 10)
          val mockedOLS = createEmptyMockedOLS
          when(mockedOLS.getUnhealthy).thenReturn(Set(Mtt))
          if (mtsSuspended) {
            when(mockedOLS.getSuspended).thenReturn(Set(Beeline))
          }
          val wrappers = List(VoxMttOperatorWrapper, VoxBeelineOperatorWrapper)
          val decider = compositeStrategy(statLoader, opLabel = mockedOLS, wrappers = wrappers)
          val decisionFutures = (1 to 1000).map { _ =>
            decider.decide(Msk, Mobile, NoRedirects)
          }
          val decisions = Future.sequence(decisionFutures).futureValue
          val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
          decisionCount should have size 1
          decisionCount(List(VoxBeelineOperatorWrapper, VoxMttOperatorWrapper)) shouldEqual 1000
        }
      }
    }

    "suspended operators" should {
      "not choose suspended" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(MttOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(MtsOperatorWrapper, count = 10)
        val mockedOLS = createEmptyMockedOLS
        when(mockedOLS.getSuspended).thenReturn(Set(Mtt))
        val decider = compositeStrategy(statLoader, opLabel = mockedOLS)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, NoRedirects)
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MtsOperatorWrapper, MttOperatorWrapper)) shouldEqual 1000
      }

      "not choose suspended by origin operator" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(VoxMttOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(VoxBeelineOperatorWrapper, count = 10)
        val mockedOLS = createEmptyMockedOLS
        when(mockedOLS.getSuspended).thenReturn(Set(Mtt))
        val wrappers = List(VoxMttOperatorWrapper, VoxBeelineOperatorWrapper)
        val decider = compositeStrategy(statLoader, opLabel = mockedOLS, wrappers = wrappers)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, NoRedirects)
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(VoxBeelineOperatorWrapper, VoxMttOperatorWrapper)) shouldEqual 1000
      }

      "not return suspended operator when fallback exists" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(MttOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(MtsOperatorWrapper, count = 10)
        val mockedOLS = createEmptyMockedOLS
        when(mockedOLS.getSuspended).thenReturn(Set(Mtt))
        val decider = compositeStrategy(statLoader, opLabel = mockedOLS)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, Suspended(null))
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MtsOperatorWrapper)) shouldEqual 1000
      }

      "not return operator wrapper with suspended operator and origin operator when fallback exists" in {
        val statLoader = mock[PhoneStatisticsLoader]
        mockStatusCountReady(statLoader)(MtsOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(VoxMttOperatorWrapper, count = 100)
        mockStatusCountReady(statLoader)(VoxBeelineOperatorWrapper, count = 10)
        val mockedOLS = createEmptyMockedOLS
        when(mockedOLS.getSuspended).thenReturn(Set(Vox, Mtt))
        val wrappers = List(MtsOperatorWrapper, VoxMttOperatorWrapper, VoxBeelineOperatorWrapper)
        val decider = compositeStrategy(statLoader, opLabel = mockedOLS, wrappers = wrappers)
        val decisionFutures = (1 to 1000).map { _ =>
          decider.decide(Msk, Mobile, Suspended(null))
        }
        val decisions = Future.sequence(decisionFutures).futureValue
        val decisionCount = decisions.groupBy(identity).mapValuesStrict(_.size)
        decisionCount should have size 1
        decisionCount(List(MtsOperatorWrapper)) shouldEqual 1000
      }
    }

    "target phone operator" should {
      "be preferred if we have it's numbers" in {
        val wrappers = List(VoxOperatorWrapper, VoxMttOperatorWrapper, VoxBeelineOperatorWrapper)
        val decider = compositeStrategy(wrappers = wrappers)
        trySeveralTimes {
          val decision =
            decider.decide(Msk, Mobile, NoRedirects, targetOperatorOpt = Some(Operators.Mtt)).futureValue
          decision.headOption shouldBe Some(VoxMttOperatorWrapper)
        }
      }

      "not interfere if we have no numbers from it" in {
        val wrappers = List(VoxOperatorWrapper, VoxMttOperatorWrapper)
        val decider = compositeStrategy(wrappers = wrappers)
        trySeveralTimes {
          val decision =
            decider.decide(Msk, Mobile, NoRedirects, targetOperatorOpt = Some(Operators.Beeline)).futureValue
          decision.headOption should not be Some(VoxBeelineOperatorWrapper)
        }
      }
    }

    "HealthyOperatorStrategy" should {
      "do nothing when creating redirect is not temporary" in {
        forAll(OperatorGen, Gen.someOf(OperatorUtils.AllPossibleOperatorWrappers).map(_.toSet.toList)) {
          case (unhealthy, operators) =>
            val mockedOLS = createEmptyMockedOLS
            when(mockedOLS.getUnhealthy).thenReturn(Set(unhealthy))
            val hos = new HealthyOperatorStrategy(mockedOLS)
            val filter = hos.filter(unhealthyFallbackExists = false)
            filter(operators) should ===(operators)
        }
      }

      "filter out unhealthy operator when temporary redirect" in {
        forAll(OperatorGen, Gen.someOf(OperatorUtils.AllPossibleOperatorWrappers).map(_.toSet.toList)) {
          case (unhealthy, operators) =>
            val mockedOLS = createEmptyMockedOLS
            when(mockedOLS.getUnhealthy).thenReturn(Set(unhealthy))
            val hos = new HealthyOperatorStrategy(mockedOLS)
            val filter = hos.filter(unhealthyFallbackExists = true)
            filter(operators) should not contain (unhealthy)
        }
      }
    }

  }

}
