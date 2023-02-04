package ru.yandex.vertis.general.personal.model.testkit

import ru.yandex.vertis.general.personal.model.SearchHistoryEntry
import zio.random.Random
import zio.test.{Gen, Sized}

import java.time.Instant

object SearchHistoryEntryGen {

  val anySearchHistoryEntry: Gen[Random with Sized, SearchHistoryEntry] = for {
    timestamp <- Gen.anyInt.map(math.abs).map(Instant.ofEpochSecond(_))
    text <- Gen.alphaNumericStringBounded(1, 100)
  } yield SearchHistoryEntry(
    timestamp = timestamp,
    text = text
  )
}
