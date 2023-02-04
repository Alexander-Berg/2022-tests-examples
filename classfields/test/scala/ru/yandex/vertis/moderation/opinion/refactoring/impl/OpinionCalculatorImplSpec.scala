package ru.yandex.vertis.moderation.opinion.refactoring.impl

import org.mockito.Mockito.when
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.Opinion.{Failed, Ok, Unknown}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  essentialsGen,
  BanSignalGen,
  DomainGen,
  UnbanSignalGen,
  UserIdGen,
  WarnSignalGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, ManualSource, SignalSet}
import ru.yandex.vertis.moderation.model.{Domain, Opinion, Opinions}
import ru.yandex.vertis.moderation.opinion.SignalFilteringPolicy
import ru.yandex.vertis.moderation.opinion.refactoring.OpinionDetailsCalculator
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.util.DateTimeUtil

class OpinionCalculatorImplSpec extends SpecBase {
  val signalFilteringPolicy = mock[SignalFilteringPolicy]
  val opinionDetailsCalculator = mock[OpinionDetailsCalculator]
  when(signalFilteringPolicy.apply(any(), any(), any())).thenReturn(true)
  when(opinionDetailsCalculator.calculateOpinionDetails(any(), any(), any(), any(), any(), any())).thenReturn(None)

  val calculator = new OpinionCalculatorImpl(signalFilteringPolicy, opinionDetailsCalculator)

  protected def nextInstance(domain: Domain, signals: SignalSet): Instance =
    CoreGenerators
      .instanceGen(CoreGenerators.ExternalIdGen.next, essentialsGen(domain.service))
      .next
      .copy(signals = signals)

  "OpinionCalculatorImpl" should {
    "return failed opinion for ban signal" in {
      val domain = DomainGen.next
      val signal =
        BanSignalGen.next
          .copy(
            domain = domain,
            source = AutomaticSource(Application.MODERATION),
            switchOff = None
          )
      val signals = SignalSet(signal)
      val instance = nextInstance(domain, signals)
      val domains = OpinionCalculatorImpl.extractDomains(instance.service, instance.signals)

      val expectedResult =
        Opinions(
          domains.map(_ -> Failed(Set(signal.detailedReason), Set.empty)).toMap[Domain, Opinion]
        )
      calculator.apply(instance) shouldBe expectedResult
    }

    "return unknown opinion for warn signal" in {
      val domain = DomainGen.next
      val signal =
        WarnSignalGen.next.copy(
          domain = domain,
          source = AutomaticSource(Application.MODERATION),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(domain, signals)
      val domains = OpinionCalculatorImpl.extractDomains(instance.service, instance.signals)

      val expectedResult =
        Opinions(
          domains.map(_ -> Unknown(Set(signal.detailedReason))).toMap[Domain, Opinion]
        )
      calculator.apply(instance) shouldBe expectedResult
    }

    "return ok opinion for unban signal" in {
      val domain = DomainGen.next
      val userId = UserIdGen.next
      val signal =
        UnbanSignalGen.next.copy(
          domain = domain,
          source = ManualSource(userId),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(domain, signals)
      val domains = OpinionCalculatorImpl.extractDomains(instance.service, instance.signals)

      val expectedResult =
        Opinions(
          domains.map(_ -> Ok(Set.empty)).toMap[Domain, Opinion]
        )
      calculator.apply(instance) shouldBe expectedResult
    }

    "squash reasons of ban signals in opinion" in {
      val userId = UserIdGen.next
      val domain = DomainGen.next
      val autoSignal =
        BanSignalGen.next.copy(
          domain = domain,
          source = AutomaticSource(Application.MODERATION),
          switchOff = None
        )
      val manualSignal =
        BanSignalGen.next.copy(
          domain = domain,
          source = ManualSource(userId),
          switchOff = None
        )
      val signals = SignalSet(autoSignal, manualSignal)
      val instance = nextInstance(domain, signals)
      val domains = OpinionCalculatorImpl.extractDomains(instance.service, instance.signals)

      val expectedResult =
        Opinions(
          domains
            .map(_ -> Failed(Set(manualSignal.detailedReason, autoSignal.detailedReason), Set.empty))
            .toMap[Domain, Opinion]
        )
      calculator.apply(instance) shouldBe expectedResult
    }

    "prefer manual signal in opinion" in {
      val userId = UserIdGen.next
      val domain = DomainGen.next
      val now = DateTimeUtil.now()
      val autoSignal =
        BanSignalGen.next.copy(
          domain = domain,
          source = AutomaticSource(Application.MODERATION),
          switchOff = None,
          timestamp = now
        )
      val manualSignal =
        UnbanSignalGen.next.copy(
          domain = domain,
          source = ManualSource(userId),
          switchOff = None,
          timestamp = now.minusDays(1)
        )
      val signals = SignalSet(autoSignal, manualSignal)
      val instance = nextInstance(domain, signals)
      val domains = OpinionCalculatorImpl.extractDomains(instance.service, instance.signals)

      val expectedResult =
        Opinions(
          domains.map(_ -> Ok(Set.empty)).toMap[Domain, Opinion]
        )
      calculator.apply(instance) shouldBe expectedResult
    }

    "prefer newer signal in opinion (Failed case)" in {
      val userId = UserIdGen.next
      val domain = DomainGen.next
      val now = DateTimeUtil.now()
      val banSignal =
        BanSignalGen.next.copy(
          domain = domain,
          source = ManualSource(userId),
          switchOff = None,
          timestamp = now
        )
      val unbanSignal =
        UnbanSignalGen.next.copy(
          domain = domain,
          source = ManualSource(userId),
          switchOff = None,
          timestamp = now.minusDays(1)
        )
      val signals = SignalSet(banSignal, unbanSignal)
      val instance = nextInstance(domain, signals)
      val domains = OpinionCalculatorImpl.extractDomains(instance.service, instance.signals)

      val expectedResult =
        Opinions(
          domains.map(_ -> Failed(Set(banSignal.detailedReason), Set.empty)).toMap[Domain, Opinion]
        )
      calculator.apply(instance) shouldBe expectedResult
    }

    "prefer newer signal in opinion (Ok case)" in {
      val userId = UserIdGen.next
      val domain = DomainGen.next
      val now = DateTimeUtil.now()
      val banSignal =
        BanSignalGen.next.copy(
          domain = domain,
          source = ManualSource(userId),
          switchOff = None,
          timestamp = now
        )
      val unbanSignal =
        UnbanSignalGen.next.copy(
          domain = domain,
          source = ManualSource(userId),
          switchOff = None,
          timestamp = now.plusDays(1)
        )
      val signals = SignalSet(banSignal, unbanSignal)
      val instance = nextInstance(domain, signals)
      val domains = OpinionCalculatorImpl.extractDomains(instance.service, instance.signals)

      val expectedResult =
        Opinions(
          domains.map(_ -> Ok(Set.empty)).toMap[Domain, Opinion]
        )
      calculator.apply(instance) shouldBe expectedResult
    }
  }
}
