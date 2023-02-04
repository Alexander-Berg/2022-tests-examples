package ru.yandex.vertis.moderation.stopwords

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  AutoruEssentialsGen,
  RealtyEssentialsGen,
  StringGen,
  _
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{
  AutoruEssentials,
  Diff,
  Essentials,
  Instance,
  RealtyEssentials,
  UpdateJournalRecord
}
import ru.yandex.vertis.moderation.model.signal.{
  AutomaticSource,
  BanSignalSource,
  SignalFactory,
  SignalInfoSet,
  SignalSet
}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, ModerationRequest}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.searcher.core.saas.document.Document
import ru.yandex.vertis.moderation.stopwords.impl.{CommonStopWordsDecider, StopWordsMatcherImpl}
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class CommonStopWordsDeciderSpec extends AbstractStopWordsDeciderSpec {
  import AbstractStopWordsDeciderSpec._

  val service: Service = Gen.oneOf(Service.REALTY, Service.AUTORU).next

  def essentialsWithWord(word: String): Essentials = {
    val description = s"${StringGen.next} $word ${StringGen.next}"
    (service: @unchecked) match {
      case Service.REALTY =>
        RealtyEssentialsGen.next.copy(description = Some(description))
      case Service.AUTORU =>
        AutoruEssentialsGen.next.copy(description = Some(description))
    }
  }

  def essentialsNotContainWord(instance: Instance, word: String): Boolean =
    instance.essentials match {
      case e: RealtyEssentials => !e.description.exists(_.contains(word))
      case e: AutoruEssentials => !e.description.exists(_.contains(word))
      case _                   => true
    }

  case class StopwordWithCyrillic(latin: String, cyrillic: String)

  def generateStopwordWithCyrillic: StopwordWithCyrillic = {
    val latinStopword =
      StringGen
        .suchThat(_.exists { symbol =>
          Document.LatinToCyrillicAnalogs.keySet(symbol)
        })
        .next

    val cyrillicStopword =
      latinStopword.map { symbol =>
        Document.LatinToCyrillicAnalogs.getOrElse(symbol, symbol)
      }

    StopwordWithCyrillic(latinStopword, cyrillicStopword)
  }

  private val signalSource =
    BanSignalSource(
      domain = Domain.default(service),
      source = AutomaticSource(Application.MODERATION, tag = Some("stopWords")),
      info = None,
      detailedReason = DetailedReason.Stopword,
      ttl = None,
      timestamp = None,
      outerComment = None,
      auxInfo = SignalInfoSet.Empty
    )

  private def createDecider(considerLatinAsCyrillic: Boolean = false) =
    new CommonStopWordsDecider(
      new StopWordsMatcherImpl(stopWordsProvider, () => considerLatinAsCyrillic),
      StopWordsDeciderFactory.banSignalSrc(service, DetailedReason.Stopword)
    )

  "StopWordsDecider" should {
    "add ban signal" in {
      val stopWord = StringGen.next
      val timestamp = DateTime.now()
      val depth = IntGen.next
      val instance: Instance =
        instanceGen(service).next
          .copy(essentials = essentialsWithWord(stopWord))

      val record = UpdateJournalRecord(timestamp, depth, instance, None, Diff.Realty(Set()))

      val expectedRequest =
        ModerationRequest.AppendSignals(
          instance.externalId,
          Seq(signalSource.withInfo(Some(stopWord))),
          timestamp,
          depth
        )

      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(stopWord))

      createDecider().decide(record).futureValue shouldBe Some(expectedRequest)
    }

    "add ban signal with several stop words" in {
      val stopWord0 = StringGen.next
      val stopWord1 = StringGen.next
      val timestamp = DateTime.now()
      val depth = IntGen.next
      val instance: Instance =
        instanceGen(service).next
          .copy(essentials = essentialsWithWord(s"$stopWord0 ${StringGen.next} $stopWord1"))

      val record = UpdateJournalRecord(timestamp, depth, instance, None, Diff.Realty(Set()))

      val expectedRequest0 =
        ModerationRequest.AppendSignals(
          instance.externalId,
          Seq(signalSource.withInfo(Some(s"$stopWord0, $stopWord1"))),
          timestamp,
          depth
        )
      val expectedRequest1 =
        ModerationRequest.AppendSignals(
          instance.externalId,
          Seq(signalSource.withInfo(Some(s"$stopWord1, $stopWord0"))),
          timestamp,
          depth
        )

      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(stopWord0, stopWord1))

      createDecider().decide(record).futureValue should (be(Some(expectedRequest0)).or(be(Some(expectedRequest1))))
    }

    "remove ban signal" in {
      val stopWord = StringGen.next
      val timestamp = DateTime.now()
      val depth = IntGen.next
      val stopWordBanSignal = SignalFactory.newSignal(signalSource, DateTimeUtil.now())
      val instance: Instance =
        instanceGen(service)
          .suchThat(essentialsNotContainWord(_, stopWord))
          .next
          .copy(signals = SignalSet(stopWordBanSignal))

      val record = UpdateJournalRecord(timestamp, depth, instance, None, Diff.Realty(Set()))

      val source = AutomaticSource(Application.MODERATION)
      val expectedRequest =
        ModerationRequest.RemoveSignals(
          instance.externalId,
          Set(stopWordBanSignal.key),
          Some(source),
          timestamp,
          depth
        )

      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(stopWord))

      createDecider().decide(record).futureValue shouldBe Some(expectedRequest)
    }

    "do nothing if no stopwords found" in {
      val stopWord = StringGen.next
      val timestamp = DateTime.now()
      val depth = IntGen.next
      val instance: Instance =
        instanceGen(service)
          .suchThat(essentialsNotContainWord(_, stopWord))
          .next
          .copy(signals = SignalSet.Empty)

      val record = UpdateJournalRecord(timestamp, depth, instance, None, Diff.Realty(Set()))

      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(stopWord))

      createDecider().decide(record).futureValue shouldBe None
    }

    "not find stopWord inside part of word" in {
      val stopWord = StringGen.next
      val wordWithStopWordAsAPart = StringGen.next + stopWord + StringGen.next
      val timestamp = DateTime.now()
      val depth = IntGen.next
      val instance: Instance =
        instanceGen(service).next
          .copy(essentials = essentialsWithWord(wordWithStopWordAsAPart))

      val record = UpdateJournalRecord(timestamp, depth, instance, None, Diff.Realty(Set()))

      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(stopWord))

      createDecider().decide(record).futureValue shouldBe None
    }

    "not find stopWord with latin symbols with disabled feature" in {
      val StopwordWithCyrillic(latin, cyrillic) = generateStopwordWithCyrillic
      val timestamp = DateTime.now()
      val depth = IntGen.next

      val instance: Instance =
        instanceGen(service).next
          .copy(essentials = essentialsWithWord(latin))

      val record = UpdateJournalRecord(timestamp, depth, instance, None, Diff.Realty(Set()))

      val expectedRequest =
        ModerationRequest.AppendSignals(
          instance.externalId,
          Seq(signalSource.withInfo(Some(latin))),
          timestamp,
          depth
        )

      val decider = createDecider()
      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(latin))
      decider.decide(record).futureValue shouldBe Some(expectedRequest)
      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(cyrillic))
      decider.decide(record).futureValue shouldBe None
    }

    "find stopWord with latin symbols with enabled feature" in {
      val StopwordWithCyrillic(latin, cyrillic) = generateStopwordWithCyrillic
      val timestamp = DateTime.now()
      val depth = IntGen.next
      val instance: Instance =
        instanceGen(service).next
          .copy(essentials = essentialsWithWord(latin))

      val record = UpdateJournalRecord(timestamp, depth, instance, None, Diff.Realty(Set()))

      val expectedRequest =
        ModerationRequest.AppendSignals(
          instance.externalId,
          Seq(signalSource.withInfo(Some(cyrillic))),
          timestamp,
          depth
        )

      when(stopWordsProvider.getStopWords).thenReturn(stopWordSet(cyrillic))
      createDecider(considerLatinAsCyrillic = true).decide(record).futureValue shouldBe Some(expectedRequest)
    }
  }
}
