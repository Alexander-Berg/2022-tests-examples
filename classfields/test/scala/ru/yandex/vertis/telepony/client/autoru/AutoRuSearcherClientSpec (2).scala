package ru.yandex.vertis.telepony.client.autoru

import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.autoru.OfferId
import ru.yandex.vertis.telepony.service.AutoRuSearcherClient

/**
  * @author neron
  */
trait AutoRuSearcherClientSpec extends SpecBase with SimpleLogging {

  def autoRuSearcherClient: AutoRuSearcherClient

  val offerId = OfferId("1071726572")

  "AutoRuSearcherClient" should {
    "get similar offers" in {
//      autoRuSearcherClient.similarOffers("autoru-1069684196").futureValue
      autoRuSearcherClient.similarOffers(offerId).futureValue
    }
    "get offer id with hash" in {
      val offerIdWithHash = autoRuSearcherClient.getOfferIdWithHash(offerId).futureValue
      println(offerIdWithHash)
    }
  }

}
