package ru.yandex.vertis.billing.integration.test.mocks

import ru.yandex.vertis.billing.model_core.{CallFact, CampaignHeader, CampaignId, Funds}
import ru.yandex.vertis.billing.service.CallPriceEstimateService

import scala.util.{Success, Try}

class TestCallPriceEstimateService extends CallPriceEstimateService {

  private var prices = Map.empty[CampaignId, Funds]

  override def getPrice(call: CallFact, header: CampaignHeader): Try[Funds] = synchronized {
    Success(prices.getOrElse(header.id, 200L))
  }

  def upsertPrices(newPrices: (CampaignId, Funds)*) = synchronized {
    prices = prices ++ newPrices.toMap
  }

}
