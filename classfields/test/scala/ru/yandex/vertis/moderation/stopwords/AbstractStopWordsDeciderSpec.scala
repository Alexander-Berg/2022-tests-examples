package ru.yandex.vertis.moderation.stopwords

import ru.yandex.vertis.moderation.SpecBase

import scala.util.{Success, Try}

trait AbstractStopWordsDeciderSpec extends SpecBase {

  protected val stopWordsProvider: StopwordsProvider = mock[StopwordsProvider]
}

object AbstractStopWordsDeciderSpec {
  def stopWordSet(stopWords: String*): Set[StopWord] = stopWords.toSet.map(StopWord.fromOriginal)
}
