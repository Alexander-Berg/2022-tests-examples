package ru.yandex.vertis.stream.yt.util.zio.clients

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vertis.stream.model.TopicPartition
import vertis.stream.sink.StoredOffsetConverters.UnparseableOffsetException
import vertis.stream.yt.util.zio.clients.PipelineYtAttributes.YtOffsetAttributes

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class PipelineYtAttributesTest extends AnyWordSpec with Matchers {

  "YtOffsetAttributes" should {

    "parse offsets with underscore" in {
      val withUnderscore = "offset_man||vertis|broker|prod|client-topics|pushnoy|push_sent_event_1"
      val expected = TopicPartition("man//vertis/broker/prod/client-topics/pushnoy/push_sent_event", 1)
      YtOffsetAttributes.toOffsetName(withUnderscore) should contain(expected)
    }

    "parse offsets with digits" in {
      val withDigits = "offset_iva||vertis|broker|test|client-topics|auto|vos2|offer-recall-event_0"
      val expected = TopicPartition("iva//vertis/broker/test/client-topics/auto/vos2/offer-recall-event", 0)
      YtOffsetAttributes.toOffsetName(withDigits) should contain(expected)
    }

    "fail on broken offset name" in {
      val noPartition = "offset_man||vertis|broker|prod|client-topics|pushnoy|push_sent_event_"
      an[UnparseableOffsetException] should be thrownBy YtOffsetAttributes.toOffsetName(noPartition)
    }

    "skip non-offset attributes" in {
      val notAnOffset = "user_attribute_keys"
      YtOffsetAttributes.toOffsetName(notAnOffset) shouldBe empty
    }
  }
}
