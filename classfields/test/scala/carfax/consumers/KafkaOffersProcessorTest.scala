package carfax.consumers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.DiffLogModel.OfferChangeEvent

class KafkaOffersProcessorTest extends AnyWordSpecLike with Matchers {

  "hasDiffSpecialCheck" should {
    "return false for false not empty diff" in {
      val event = OfferChangeEvent.newBuilder()
      event.getDiffBuilder

      val res = event.build()

      res.hasDiff shouldBe true

      KafkaOffersProcessor.hasDiffSpecialCheck(res) shouldBe false
    }

    "return false for empty diff" in {
      val event = OfferChangeEvent.newBuilder()

      val res = event.build()

      res.hasDiff shouldBe false

      KafkaOffersProcessor.hasDiffSpecialCheck(res) shouldBe false

    }

    "return true for not empty diff" in {
      val event = OfferChangeEvent.newBuilder()
      val diff = event.getDiffBuilder
      diff.getBodyNumberBuilder.setNewValue("55")

      val res = event.build()

      res.hasDiff shouldBe true

      KafkaOffersProcessor.hasDiffSpecialCheck(res) shouldBe true
    }
  }

}
