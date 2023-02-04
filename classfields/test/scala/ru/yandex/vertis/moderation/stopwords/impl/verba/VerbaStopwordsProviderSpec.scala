package ru.yandex.vertis.moderation.stopwords.impl.verba

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.extdatacore.model.verba.stopwords.VerbaActions
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.stopwords.StopWord

@RunWith(classOf[JUnitRunner])
class VerbaStopwordsProviderSpec extends SpecBase {

  private val service = Service.AUTORU
  private val verbaBan = VerbaKey(service, VerbaActions.Ban)
  private val verbaWarn = VerbaKey(service, VerbaActions.Warn)

  private case class TestCase(description: String,
                              stopWords: Map[VerbaKey, Set[StopWord]],
                              verbaKeys: Set[VerbaKey],
                              expected: Set[StopWord]
                             )

  private val testCases =
    Seq(
      TestCase(
        description = "gets nothing if holder is empty",
        stopWords = Map.empty,
        verbaKeys = Set(verbaBan),
        expected = Set.empty
      ),
      TestCase(
        description = "gets nothing if holder doesn't have this key",
        stopWords = Map(verbaWarn -> Set("ccc")),
        verbaKeys = Set(verbaBan),
        expected = Set.empty
      ),
      TestCase(
        description = "gets all by key if holder has this key",
        stopWords = Map(verbaBan -> Set("aaa", "bbb"), verbaWarn -> Set("ccc")),
        verbaKeys = Set(verbaBan),
        expected = Set("aaa", "bbb")
      ),
      TestCase(
        description = "gets all if holder has every key",
        stopWords = Map(verbaBan -> Set("aaa", "bbb"), verbaWarn -> Set("ccc")),
        verbaKeys = Set(verbaWarn, verbaBan),
        expected = Set("aaa", "bbb", "ccc")
      )
    )

  "VerbaStopwordsProvider.getStopWords" should {
    testCases.foreach { case TestCase(description, stopWords, verbaKeys, expected) =>
      description in {
        val holder: VerbaStopwordsHolder = () => stopWords
        val actual = new VerbaStopwordsProvider(holder, verbaKeys).getStopWords
        actual shouldBe expected
      }
    }
  }

  implicit def string2StopWord(s: String): StopWord = StopWord.fromOriginal(s)
}
