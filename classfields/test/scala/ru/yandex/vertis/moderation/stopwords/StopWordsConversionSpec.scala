package ru.yandex.vertis.moderation.stopwords

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.extdatacore.model.verba.stopwords.RawStopWord

import scala.util.{Success, Try}

@RunWith(classOf[JUnitRunner])
class StopWordsConversionSpec extends SpecBase {

  private val stopwordsRawOriginal =
    Set(
      "simple",
      "\"inQuotes\"",
      "!exclamation",
      "!\"exclamationInQuotes\"",
      "few words",
      "\"few words in quotes\""
    )

  private val stopwords = stopwordsRawOriginal.map(StopWord.fromOriginal)

  private val expectedAfterNormalization =
    Set(
      " simple ",
      " inquotes ",
      " exclamation ",
      " exclamationinquotes ",
      " few words ",
      " few words in quotes "
    )

  private val expectedAfterReplacingLatin =
    Set(
      " siтрlе ",
      " iпqиотеs ",
      " ехсlататiоп ",
      " ехсlататiопiпqиотеs ",
      " fеw wогдs ",
      " fеw wогдs iп qиотеs "
    )

  "StopWord.fromOriginal" should {
    "do not change original" in {
      stopwords.map(_.original) should smartEqual(stopwordsRawOriginal)
    }
    "correctly normalize words" in {
      stopwords.map(_.normalized) should smartEqual(expectedAfterNormalization)
    }
    "correctly normalize words and replace latin by cyrillic" in {
      stopwords.map(_.normalizedLatinReplacedByCyrillic) should smartEqual(expectedAfterReplacingLatin)
    }
  }
}
