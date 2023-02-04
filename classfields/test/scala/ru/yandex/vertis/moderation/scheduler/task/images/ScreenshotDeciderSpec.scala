package ru.yandex.vertis.moderation.scheduler.task.images

import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.scheduler.task.images.PhotoDecider.{ScreenshotVerdictActionDecider, VerdictAction}
import ru.yandex.vertis.moderation.scheduler.task.images.ScreenshotFinder.ScreenshotPhoto

@RunWith(classOf[JUnitRunner])
class ScreenshotDeciderSpec extends SpecBase {
  private val source = mock[PhotoDecider.Source]
  private val finder = mock[ScreenshotFinder]

  private val verdictDecider: ScreenshotVerdictActionDecider =
    photo =>
      photo.confidence match {
        case c if c >= 0.9            => VerdictAction.Ban
        case c if c >= 0.6 && c < 0.9 => VerdictAction.Warn
        case _                        => VerdictAction.NoAction
      }

  private val screenshotDecider = new ScreenshotDecider(finder, verdictDecider)

  "ScreenshotDecider" should {
    "return ok verdict if no screenshots found" in {
      when(finder.apply(any())).thenReturn(Seq.empty)

      screenshotDecider.apply(source) shouldBe PhotoDecider.Verdict.Ok
    }

    "return verdict with bans with DAMAGED_PHOTO reason if max confidence is above ban threshold" in {
      val screenshotPhotos =
        Seq(
          ScreenshotPhoto("1", 0.5),
          ScreenshotPhoto("2", 0.6),
          ScreenshotPhoto("3", 0.9),
          ScreenshotPhoto("4", 0.8)
        )
      when(finder.apply(any())).thenReturn(screenshotPhotos)

      val verdict = screenshotDecider.apply(source)

      verdict.bans.size shouldBe 1
      verdict.bans.head.reason shouldBe DetailedReason.DamagedPhoto
      verdict.bans.head.info.get should startWith("image_id:3")
    }

    "return verdict with warns with SCREENSHOT_ON_PHOTO reason if max confidence is between ban and warn threshold" in {
      val screenshotPhotos =
        Seq(
          ScreenshotPhoto("1", 0.5),
          ScreenshotPhoto("2", 0.6),
          ScreenshotPhoto("4", 0.8)
        )
      when(finder.apply(any())).thenReturn(screenshotPhotos)

      val verdict = screenshotDecider.apply(source)

      verdict.warns.size shouldBe 1
      verdict.warns.head.reason shouldBe DetailedReason.ScreenshotOnPhoto
      verdict.warns.head.info.get should startWith("image_id:4")
    }

    "return ok verdict if max confidence is below warn threshold" in {
      val screenshotPhotos =
        Seq(
          ScreenshotPhoto("1", 0.5),
          ScreenshotPhoto("2", 0.4),
          ScreenshotPhoto("4", 0.3)
        )
      when(finder.apply(any())).thenReturn(screenshotPhotos)

      val verdict = screenshotDecider.apply(source)

      verdict shouldBe PhotoDecider.Verdict.Ok
    }
  }
}
