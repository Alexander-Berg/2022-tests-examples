package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.utils.recommendation.{Recommendation, RecommendationService}

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class RecommendationWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val repoMock = mock[RecommendationService]
    when(repoMock.getRecommendations(?)(?)).thenReturn(Success(Seq(Recommendation.ViewsDrop)))

    val worker = new RecommendationsWorkerYdb(repoMock) with YdbWorkerTestImpl
  }

  "skip inactive offers" in new Fixture {
    val offer = TestUtils.createOffer(dealer = true).addFlag(OfferModel.OfferFlag.OF_INACTIVE).build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "skip offers not from dealers" in new Fixture {
    val offerNotFromDealer = TestUtils.createOffer(dealer = false).addFlag(OfferModel.OfferFlag.OF_VALID).build
    assert(!worker.shouldProcess(offerNotFromDealer, None).shouldProcess)
  }

  "get new nextCheckTime after processing" in new Fixture {
    val activeOfferFromDealer = TestUtils.createOffer(dealer = true).addFlag(OfferModel.OfferFlag.OF_VALID).build
    val result = worker.process(activeOfferFromDealer, None)
    assert(result.nextCheck.nonEmpty)
  }

}
