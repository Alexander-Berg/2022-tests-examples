package ru.yandex.vertis.vsquality.callgate.api.service

import cats.instances.try_._
import ru.yandex.vertis.vsquality.callgate.model.VerdictName._
import ru.yandex.vertis.vsquality.callgate.model.api.ApiRequestV2.CleanWebApplyTaskResult
import ru.yandex.vertis.vsquality.callgate.model.cleanweb.CleanWebVerdictView
import ru.yandex.vertis.vsquality.callgate.model.{CleanWebCallgateResolution, CleanWebVerdict, TaskDescriptor}
import ru.yandex.vertis.hobo.proto.model.QueueId
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

import scala.util.Try

class CleanWebConverterImplSpec extends SpecBase {
  private val converter = new CleanWebConverterImpl[Try]

  private val cleanWebTaskKey = "082a1b2c3d4e5f6g7"
  private val queueId = QueueId.fromValue(82)
  private val taskKey = "a1b2c3d4e5f6g7"
  private val descriptor = TaskDescriptor(queueId, taskKey)

  private def cleanWebTaskResult(
      verdicts: List[String],
      probability: Option[Double] = None,
      imageUrl: Option[String] = None): CleanWebApplyTaskResult =
    CleanWebApplyTaskResult(
      verdicts.map(CleanWebVerdictView(cleanWebTaskKey, _, value = true, probability, imageUrl))
    )

  "CleanWebConverterImpl.convert" should {
    "convert default ok result" in {
      val cleanWebApplyTaskResult = cleanWebTaskResult(List("clean_web_moderation_end"))
      val comment = Some("clean_web_moderation_end")
      val expectedResult =
        (descriptor, CleanWebCallgateResolution(List(CleanWebVerdict(CleanWebModerationEnd, None, None)), comment))
      converter.convert(cleanWebApplyTaskResult).get shouldBe expectedResult
    }

    "convert ok with auto check result" in {
      val cleanWebApplyTaskResult = cleanWebTaskResult(List("clean_web_moderation_end", "text_auto_good"))
      val comment = Some("clean_web_moderation_end, text_auto_good")
      val verdicts = List(CleanWebVerdict(CleanWebModerationEnd, None, None), CleanWebVerdict(TextAutoGood, None, None))
      val expectedResult = (descriptor, CleanWebCallgateResolution(verdicts, comment))
      converter.convert(cleanWebApplyTaskResult).get shouldBe expectedResult
    }

    "convert bad result" in {
      val cleanWebApplyTaskResult = cleanWebTaskResult(List("clean_web_moderation_end", "text_toloka_spam"))
      val comment = Some("clean_web_moderation_end, text_toloka_spam")
      val verdicts =
        List(CleanWebVerdict(CleanWebModerationEnd, None, None), CleanWebVerdict(TextTolokaSpam, None, None))
      val expectedResult = (descriptor, CleanWebCallgateResolution(verdicts, comment))
      converter.convert(cleanWebApplyTaskResult).get shouldBe expectedResult
    }

    "convert good + bad verdict" in {
      val cleanWebApplyTaskResult =
        cleanWebTaskResult(List("clean_web_moderation_end", "text_auto_good", "text_toloka_spam"))
      val comment = Some("clean_web_moderation_end, text_auto_good, text_toloka_spam")
      val verdicts =
        List(
          CleanWebVerdict(CleanWebModerationEnd, None, None),
          CleanWebVerdict(TextAutoGood, None, None),
          CleanWebVerdict(TextTolokaSpam, None, None)
        )
      val expectedResult = (descriptor, CleanWebCallgateResolution(verdicts, comment))
      converter.convert(cleanWebApplyTaskResult).get shouldBe expectedResult
    }

    "convert with unparsed (unsupported) verdict" in {
      val cleanWebApplyTaskResult = cleanWebTaskResult(List("clean_web_moderation_end", "text_manual_toloka_meme"))
      val comment = Some("clean_web_moderation_end, text_manual_toloka_meme")
      val expectedResult =
        (descriptor, CleanWebCallgateResolution(List(CleanWebVerdict(CleanWebModerationEnd, None, None)), comment))
      converter.convert(cleanWebApplyTaskResult).get shouldBe expectedResult
    }

    "convert bad result with probability" in {
      val probability = Some(0.05)
      val cleanWebApplyTaskResult = cleanWebTaskResult(List("media_auto_porn_probability"), probability)
      val comment = Some("media_auto_porn_probability")
      val verdicts = List(CleanWebVerdict(MediaAutoPornProbability, probability, None))
      val expectedResult = (descriptor, CleanWebCallgateResolution(verdicts, comment))
      converter.convert(cleanWebApplyTaskResult).get shouldBe expectedResult
    }

    "convert bad result and filter if probability is too low" in {
      val cleanWebApplyTaskResult = cleanWebTaskResult(List("media_auto_porn_probability"), Some(0.001))
      val expectedResult =
        (descriptor, CleanWebCallgateResolution(List(CleanWebVerdict(CleanWebModerationEnd, None, None)), Some("")))
      converter.convert(cleanWebApplyTaskResult).get shouldBe expectedResult
    }
  }
}
