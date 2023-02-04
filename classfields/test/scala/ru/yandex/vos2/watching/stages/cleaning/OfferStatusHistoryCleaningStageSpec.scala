package ru.yandex.vos2.watching.stages.cleaning

import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vos2.OfferModel.OfferStatusHistoryItem
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.realty.model.offer.OfferStatusHistoryGenerator.OshItemGen
import ru.yandex.vos2.util.StageUtils
import ru.yandex.vos2.watching.ProcessingState

import scala.collection.JavaConverters._

/**
  * Created by theninthowl on 5/15/20
  */
class OfferStatusHistoryCleaningStageSpec extends WordSpec with Matchers with StageUtils {

  private val stage = new OfferStatusHistoryCleaningStage
  private val maximumStatusHistory = 50

  "OfferStatusHistoryCleaningStage" should {
    "not process offers with normal status history size" in {
      val offer = TestUtils.createOffer().build()
      stage.shouldProcess(offer) shouldBe false
    }

    "clear status history for offers with long status history size" in {
      val statusHistorySampe = OshListGen.sample.get
      val offer = TestUtils.createOffer().addAllStatusHistory(statusHistorySampe.asJava).build()
      stage.shouldProcess(offer) shouldBe true
      stage.process(ProcessingState(offer, offer)).offer.getStatusHistoryCount shouldBe maximumStatusHistory
    }
  }

  private val OshListGen: Gen[List[OfferStatusHistoryItem]] =
    Gen
      .const(1001)
      .flatMap(n ⇒ Gen.listOfN(n, OshItemGen))
      .map(_.sortBy(_.getTimestamp))

}
