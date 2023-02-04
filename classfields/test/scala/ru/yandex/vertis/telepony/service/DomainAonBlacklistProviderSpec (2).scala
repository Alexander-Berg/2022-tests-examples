package ru.yandex.vertis.telepony.service

import org.joda.time.DateTime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.{AonBlockInfoGen, RefinedSourceGen}
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.RefinedSource
import ru.yandex.vertis.telepony.service.impl.DomainAonBlacklistProviderImpl

import scala.concurrent.Future

class DomainAonBlacklistProviderSpec extends SpecBase with MockitoSupport {

  private val Prefix = "multiple_unmatched_calls.prank"

  private val Pattern = s"$Prefix.*".r

  "DomainAonBlacklistProvider" should {
    "return verdict allowed by regexp" in {
      val delegate = mock[AonBlacklistService]
      val expectedVerdict = s"${Prefix}_suffix"
      stub(delegate.get(_: RefinedSource)) { case source =>
        val blockInfo = AonBlockInfoGen.next.copy(
          source = source,
          verdict = expectedVerdict
        )
        Future.successful(Some(blockInfo))
      }
      val provider = new DomainAonBlacklistProviderImpl(delegate, Set(Pattern))

      val source = RefinedSourceGen.next
      provider.get(source).futureValue match {
        case Some(actual) =>
          actual.source shouldBe source
          actual.verdict shouldBe expectedVerdict
        case None => fail("Expect block info but got nothing")
      }
    }
    "return nothing" when {
      "verdict not allowed by regexp" in {
        val delegate = mock[AonBlacklistService]
        val verdict = s"a_$Prefix"
        stub(delegate.get(_: RefinedSource)) { case source =>
          val blockInfo = AonBlockInfoGen.next.copy(
            source = source,
            verdict = verdict
          )
          Future.successful(Some(blockInfo))
        }
        val provider = new DomainAonBlacklistProviderImpl(delegate, Set(Pattern))

        val source = RefinedSourceGen.next
        provider.get(source).futureValue shouldBe None
      }
    }

    "return verdict when time is passed" in {
      val delegate = mock[AonBlacklistService]
      val time = DateTime.now
      val deadlineIsNotOver = time.plusHours(1)
      val expectedVerdict = s"${Prefix}_suffix"
      stub(delegate.getForTime(_: RefinedSource, _: DateTime)) { case (source, _) =>
        val blockInfo = AonBlockInfoGen.next.copy(
          source = source,
          verdict = expectedVerdict,
          deadline = Some(deadlineIsNotOver)
        )
        Future.successful(Some(blockInfo))
      }
      val provider = new DomainAonBlacklistProviderImpl(delegate, Set(Pattern))

      val source = RefinedSourceGen.next
      provider.getForTime(source, time).futureValue match {
        case Some(actual) =>
          actual.source shouldBe source
          actual.verdict shouldBe expectedVerdict
        case None => fail("Expect block info but got nothing")
      }
    }
  }

}
