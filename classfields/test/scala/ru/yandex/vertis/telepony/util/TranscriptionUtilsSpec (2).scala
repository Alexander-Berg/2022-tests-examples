package ru.yandex.vertis.telepony.util

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.{CallDialog, Transcription}

import scala.concurrent.duration._

class TranscriptionUtilsSpec extends SpecBase {

  private val unusedText: String = "unused"
  private val nearbyPhrasesInterval: Long = 350L

  "TranscriptionUtils.toDialog" should {
    "make empty dialog on empty chunks" in {
      val chunks = Seq.empty[Transcription.Chunk]
      val dialog = TranscriptionUtils.toDialogue(chunks, CallDialog.Speakers.Source, nearbyPhrasesInterval)
      dialog.phrases shouldBe empty
    }

    "make empty dialog on empty alternatives" in {
      val chunks = Seq(Transcription.Chunk(Seq.empty, Transcription.ChannelTags.FirstChannel))
      val dialog = TranscriptionUtils.toDialogue(chunks, CallDialog.Speakers.Source, nearbyPhrasesInterval)
      dialog.phrases shouldBe empty
    }

    "make dialog with one by one speakers in order by start intervals" in {
      val chunks: Seq[Transcription.Chunk] = Seq(
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(700.millis, 1000.millis, "2.1")), unusedText)),
          Transcription.ChannelTags.SecondChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(0.millis, 500.millis, "1.1")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(1200.millis, 2000.millis, "2.2")), unusedText)),
          Transcription.ChannelTags.SecondChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(600.millis, 800.millis, "1.2")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(2200.millis, 2300.millis, "1.3")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(2300.millis, 2700.millis, "2.3")), unusedText)),
          Transcription.ChannelTags.SecondChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(3000.millis, 4000.millis, "1.4")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        )
      )

      val dialog = TranscriptionUtils.toDialogue(chunks, CallDialog.Speakers.Source, nearbyPhrasesInterval)
      val expectedPhrases: Seq[CallDialog.Phrase] = Seq(
        CallDialog.Phrase(CallDialog.Speakers.Source, 0L, 800L, "1.1 1.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 700L, 2700L, "2.1 2.2 2.3", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Source, 2200L, 4000L, "1.3 1.4", Seq.empty)
      )
      (dialog.phrases.toSeq should contain).theSameElementsInOrderAs(expectedPhrases)
    }

    "always merge sequential one speaker phrases" in {
      val chunks: Seq[Transcription.Chunk] = Seq(
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(0.millis, 500.millis, "1.1")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(2000.millis, 5000.millis, "1.2")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(10000.millis, 12000.millis, "2.1")), unusedText)),
          Transcription.ChannelTags.SecondChannel
        )
      )

      val dialog = TranscriptionUtils.toDialogue(chunks, CallDialog.Speakers.Source, nearbyPhrasesInterval)
      val expectedPhrases: Seq[CallDialog.Phrase] = Seq(
        CallDialog.Phrase(CallDialog.Speakers.Source, 0L, 5000L, "1.1 1.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 10000L, 12000L, "2.1", Seq.empty)
      )
      (dialog.phrases.toSeq should contain).theSameElementsInOrderAs(expectedPhrases)
    }

    "not merge far non-sequential one speaker phrases" in {
      val chunks: Seq[Transcription.Chunk] = Seq(
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(0.millis, 500.millis, "1.1")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(550.millis, 1000.millis, "2.1")), unusedText)),
          Transcription.ChannelTags.SecondChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(900.millis, 2000.millis, "1.2")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        )
      )

      val dialog = TranscriptionUtils.toDialogue(chunks, CallDialog.Speakers.Source, nearbyPhrasesInterval)
      val expectedPhrases: Seq[CallDialog.Phrase] = Seq(
        CallDialog.Phrase(CallDialog.Speakers.Source, 0L, 500L, "1.1", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 550L, 1000L, "2.1", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Source, 900L, 2000L, "1.2", Seq.empty)
      )
      (dialog.phrases.toSeq should contain).theSameElementsInOrderAs(expectedPhrases)
    }

    "merge nearby non-sequential one speaker phrases" in {
      val chunks: Seq[Transcription.Chunk] = Seq(
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(0.millis, 500.millis, "1.1")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(550.millis, 1000.millis, "2.1")), unusedText)),
          Transcription.ChannelTags.SecondChannel
        ),
        Transcription.Chunk(
          Seq(Transcription.Alternative(Seq(Transcription.Word(800.millis, 2000.millis, "1.2")), unusedText)),
          Transcription.ChannelTags.FirstChannel
        )
      )

      val dialog = TranscriptionUtils.toDialogue(chunks, CallDialog.Speakers.Source, nearbyPhrasesInterval)
      val expectedPhrases: Seq[CallDialog.Phrase] = Seq(
        CallDialog.Phrase(CallDialog.Speakers.Source, 0L, 2000L, "1.1 1.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 550L, 1000L, "2.1", Seq.empty)
      )
      (dialog.phrases.toSeq should contain).theSameElementsInOrderAs(expectedPhrases)
    }
  }

  "TranscriptionUtils.mergeNearPhrases" should {
    "keep one by one speakers order" in {
      val rawPhrases: Seq[CallDialog.Phrase] = Seq(
        CallDialog.Phrase(CallDialog.Speakers.Source, 0L, 500L, "1.1", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Source, 600L, 800L, "1.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 700L, 1000L, "2.1", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 1200L, 2000L, "2.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Source, 2200L, 2300L, "1.3", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 2300L, 2700L, "2.3", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 3500L, 4000L, "2.4", Seq.empty)
      )
      val result = TranscriptionUtils.mergeNearPhrases(rawPhrases, nearbyPhrasesInterval)
      val expected: Seq[CallDialog.Phrase] = Seq(
        CallDialog.Phrase(CallDialog.Speakers.Source, 0L, 800L, "1.1 1.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 700L, 2700L, "2.1 2.2 2.3", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Source, 2200L, 2300L, "1.3", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 3500L, 4000L, "2.4", Seq.empty)
      )
      (result should contain).theSameElementsInOrderAs(expected)
    }

    "do nothing if applied to its result" in {
      val rawPhrases: Seq[CallDialog.Phrase] = Seq(
        CallDialog.Phrase(CallDialog.Speakers.Source, 0L, 800L, "1.1", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 550L, 1000L, "2.1", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Source, 1200L, 1500L, "1.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Target, 1300L, 1600L, "2.2", Seq.empty),
        CallDialog.Phrase(CallDialog.Speakers.Source, 1550L, 2000L, "1.3", Seq.empty)
      )

      val firstResult = TranscriptionUtils.mergeNearPhrases(rawPhrases, nearbyPhrasesInterval)
      val secondResult = TranscriptionUtils.mergeNearPhrases(firstResult, nearbyPhrasesInterval)
      firstResult shouldBe secondResult
    }
  }
}
