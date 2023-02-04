package ru.yandex.vertis.moderation.stopwords

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalameter.api
import org.scalameter.api._
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{instanceGen, stringGen, IntGen, RealtyEssentialsGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Diff, UpdateJournalRecord}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.stopwords.impl.{CommonStopWordsDecider, StopWordsMatcherImpl}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Success, Try}

/**
  * Performance tests for StopWordsDecider. ScalaMeter library used for microbenchmarks.
  * Scalacheck generators reused to simplify objects initialization.
  * For better results run it from command line (it is a standalone app).
  * @author vnovichihin@
  */
object StopWordsPerfTest extends Bench.LocalTime {

  val deciderLatinReplaced = createDecider(stopWordsCount = 10000, considerLatinAsCyrillic = true)
  val deciderLatinNotReplaced = createDecider(stopWordsCount = 10000, considerLatinAsCyrillic = false)

  // generate records with different descriptions size
  val descSizes = api.Gen.range("description length")(from = 1000, upto = 10000, hop = 1000)
  val journalRecords =
    (for {
      size <- descSizes
    } yield createRecord(size)).cached

  val largeRecord = createRecord(descLength = 50000)

  val stopWordsCounts = api.Gen.range("stop-words count")(from = 5000, upto = 50000, hop = 10000)
  val deciders =
    (for {
      count <- stopWordsCounts
    } yield createDecider(count, considerLatinAsCyrillic = true)).cached

  performance
    .of("StopWordsDecider")
    .config(
      exec.benchRuns -> 10, // how many test attempts for each "measure"
      exec.independentSamples -> 1 // how many JVM instances (separate) to be used
    ) in {

    measure.method("by description size, considerLatinAsCyrillic=true") in {
      using(journalRecords) in { record =>
        Await.result(deciderLatinReplaced.decide(record), Duration.Inf)
      }
    }

    measure.method("by description size, considerLatinAsCyrillic=false") in {
      using(journalRecords) in { record =>
        Await.result(deciderLatinNotReplaced.decide(record), Duration.Inf)
      }
    }

    measure.method("by stop-words count, considerLatinAsCyrillic=true") in {
      using(deciders) in { decider =>
        Await.result(decider.decide(largeRecord), Duration.Inf)
      }
    }
  }

  // helpers

  private def createStopWordsProvider(wordsCount: Int): StopwordsProvider =
    new StopwordsProvider {
      private val cached =
        Iterator
          .continually(stringGen(5, 10).next)
          .take(wordsCount)
          .toSet
          .map(StopWord.fromOriginal)

      override def getStopWords: Set[StopWord] = cached
    }

  private def createRecord(descLength: Int) = {
    val instance =
      instanceGen(service).next
        .copy(essentials = RealtyEssentialsGen.next.copy(description = Some(stringGen(descLength, descLength).next)))
    UpdateJournalRecord(DateTime.now(), IntGen.next, instance, None, Diff.Realty(Set()))
  }

  private def service: Service = Gen.oneOf(Service.REALTY, Service.AUTORU).next

  private def createDecider(stopWordsCount: Int, considerLatinAsCyrillic: Boolean = false) =
    new CommonStopWordsDecider(
      new StopWordsMatcherImpl(createStopWordsProvider(stopWordsCount), () => considerLatinAsCyrillic),
      StopWordsDeciderFactory.banSignalSrc(service, DetailedReason.Stopword)
    )
}
