package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito.verify
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterAll
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.salesman.SalesmanClient
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.{getNow, OfferIRef}

import scala.util.{Failure, Try}

class CommercialActivationWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val featureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featureRegistry)

    val salesmanClient = mock[SalesmanClient]

    val worker = new CommercialActivationV1WorkerYdb(
      salesmanClient
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  "Commercial Activation Stage" should {
    "activate commercial offer" in new Fixture {
      val offer: Offer = TestUtils.createOffer(getNow, dealer = true).build()
      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Try(true))
      val resultState = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)
    }

    "reschedule stage of request error" in new Fixture {
      val offer: Offer = TestUtils.createOffer(getNow, dealer = true).build()
      when(salesmanClient.addGoods(?, ?, ?, ?)(?)).thenReturn(Failure(new IllegalArgumentException))
      val resultState = worker.process(offer, None)

      val clientId: OfferIRef = offer.getUserRef.filter(_.isDigit).toLong
      verify(salesmanClient).addGoods(clientId, offer.getOfferID, offer.oldCategoryId, offer.getOfferAutoru.getSection)

      resultState.nextCheck.nonEmpty shouldBe true
    }
  }

}
