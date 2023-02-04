package ru.yandex.vertis.general.favorites.model.testkit

import ru.yandex.vertis.general.favorites.model.SavedNote
import zio.random.Random
import zio.test.{Gen, Sized}

object SavedNoteGen {

  val anyOfferId: Gen[Random with Sized, String] = {
    Gen.anyUUID.map(uuid => uuid.toString).noShrink
  }

  val anyText: Gen[Random with Sized, String] = {
    Gen.alphaNumericStringBounded(10, 15).noShrink
  }

  def anySavedNote(
      offerId: Gen[Random with Sized, String] = anyOfferId,
      text: Gen[Random with Sized, String] = anyText): Gen[Random with Sized, SavedNote] =
    for {
      offerId <- offerId
      text <- text
    } yield SavedNote(offerId = offerId, text = text)

  val anySavedNote: Gen[Random with Sized, SavedNote] = anySavedNote().noShrink

  def anySavedNotes(count: Int): Gen[Random with Sized, List[SavedNote]] =
    Gen.listOfN(count)(anySavedNote).noShrink

}
