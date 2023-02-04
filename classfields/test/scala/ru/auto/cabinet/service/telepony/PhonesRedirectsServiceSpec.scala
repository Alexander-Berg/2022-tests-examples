package ru.auto.cabinet.service.telepony

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{AnyWordSpecLike => WordSpecLike}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.cabinet.environment
import ru.auto.cabinet.model.{CustomerId => _, _}
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.service.salesman.{Campaign, SalesmanClient}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.{reset, when}
import ru.auto.cabinet.service.salesman.Campaign.PaymentModel
import ru.auto.cabinet.trace.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PhonesRedirectsServiceSpec
    extends Matchers
    with WordSpecLike
    with ScalaFutures {
  implicit private val rc = Context.unknown

  private val salesmanClient = mock[SalesmanClient]

  implicit private val instr = new EmptyInstr("")

  private val phonesRedirectsService = new PhonesRedirectsService(
    salesmanClient)

  private val testPoiData = PoiData(
    1,
    Location(1, None),
    None,
    callTracking = true,
    properties = Some(PoiProperties("asd", None, None)))

  private val testClient = Client(
    1,
    0L,
    ClientProperties(
      regionId = 1,
      cityId = 1,
      "test",
      ClientStatuses.Active,
      environment.now,
      "",
      Some("website.test"),
      "test@yandex.ru",
      Some("manager@yandex.ru"),
      None,
      None,
      multipostingEnabled = true,
      callsAuctionAvailable = true,
      firstModerated = true,
      isAgent = false,
      comment = ""
    )
  )

  private val testPoiClient = PoiClient(testPoiData, testClient)

  "PhoneRedirectsInfo.getInfo()" should {

    "return autoru-billing domain if dealer got Calls ads campaign" in {
      reset(salesmanClient)

      when(
        salesmanClient
          .listCampaigns(testPoiClient.client.clientId, includeDisabled = true))
        .thenReturn(
          Future.successful(
            Set(
              Campaign(
                "tag",
                OfferCategories.Cars,
                Set(OfferSections.New),
                1,
                PaymentModel.Calls))
          ))

      val result = phonesRedirectsService
        .getInfo(testPoiClient, Some(Category.CARS), Some(Section.NEW))
        .futureValue
        .get

      result.domain shouldBe Domains.AutoDealersAuction

    }

    "return autoru-billing domain if dealer got SingleWithCalls ads campaign" in {
      reset(salesmanClient)

      when(
        salesmanClient
          .listCampaigns(testPoiClient.client.clientId, includeDisabled = true))
        .thenReturn(
          Future.successful(
            Set(
              Campaign(
                "tag",
                OfferCategories.Cars,
                Set(OfferSections.New),
                1,
                PaymentModel.SingleWithCalls))
          ))

      val result = phonesRedirectsService
        .getInfo(testPoiClient, Some(Category.CARS), Some(Section.NEW))
        .futureValue
        .get

      result.domain shouldBe Domains.AutoDealersAuction

    }

    "return autoru-dealer domain if dealer neither Calls nor SingleWithCalls" in {
      reset(salesmanClient)
      when(
        salesmanClient
          .listCampaigns(testPoiClient.client.clientId, includeDisabled = true))
        .thenReturn(
          Future.successful(
            Set(
              Campaign(
                "tag",
                OfferCategories.Cars,
                Set(OfferSections.New),
                1,
                PaymentModel.Single))
          ))

      val result = phonesRedirectsService
        .getInfo(testPoiClient, Some(Category.CARS), Some(Section.NEW))
        .futureValue
        .get

      result.domain shouldBe Domains.AutoDealers
    }

    "return autoru-dealer domain if dealer has force info and none Calls or SingleWithCalls and call" in {
      reset(salesmanClient)

      when(
        salesmanClient
          .listCampaigns(testPoiClient.client.clientId, includeDisabled = true))
        .thenReturn(
          Future.successful(
            Set(
              Campaign(
                "tag",
                OfferCategories.Cars,
                Set(OfferSections.New),
                1,
                PaymentModel.Single))
          ))

      val updatedTestPoi =
        testPoiClient.copy(poi = testPoiData.copy(callTracking = false))

      val result = phonesRedirectsService
        .getInfo(
          updatedTestPoi,
          Some(Category.CARS),
          Some(Section.NEW),
          forceInfo = true)
        .futureValue
        .get

      result.domain shouldBe Domains.AutoDealers
    }

    "return None if calltracking off, campaign not Calls nor SingleWithCalls" in {
      reset(salesmanClient)
      when(
        salesmanClient
          .listCampaigns(testPoiClient.client.clientId, includeDisabled = true))
        .thenReturn(
          Future.successful(
            Set(
              Campaign(
                "tag",
                OfferCategories.Cars,
                Set(OfferSections.New),
                1,
                PaymentModel.Single))
          ))

      val updatedTestPoi =
        testPoiClient.copy(poi = testPoiData.copy(callTracking = false))

      val result = phonesRedirectsService
        .getInfo(updatedTestPoi, Some(Category.CARS), Some(Section.NEW))
        .futureValue

      result shouldBe None
    }
  }

}
