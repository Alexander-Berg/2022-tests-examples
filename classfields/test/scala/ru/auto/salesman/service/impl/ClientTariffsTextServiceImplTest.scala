package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.salesman.dao.{AdsRequestDao, QuotaRequestDao}
import ru.auto.salesman.dao.QuotaRequestDao.Actual
import ru.auto.salesman.model.{AdsRequestTypes, ProductId, QuotaRequest}
import ru.auto.salesman.model.TariffText._
import ru.auto.salesman.service.{BillingService, DetailedClientSource}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.ClientDetailsGen
import ru.auto.salesman.test.model.gens.activeCampaignHeaderGen
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import zio.ZIO

import scala.util.Failure

class ClientTariffsTextServiceImplTest extends BaseSpec {

  private val adsDao = mock[AdsRequestDao]
  private val quotaDao = mock[QuotaRequestDao]
  private val clients = mock[DetailedClientSource]
  private val billing = mock[BillingService]
  private val now = DateTime.parse("2020-10-14T10:00:00.000")

  private val client = new ClientTariffsTextServiceImpl(
    adsDao,
    quotaDao,
    clients,
    billing
  )

  "ClientChangedToTextService" should {
    "return empty if everything disabled" in {
      val clientDetails = ClientDetailsGen.next
      val id = clientDetails.clientId

      (adsDao.get _)
        .expects(id, AdsRequestTypes.CarsUsed)
        .returningT(None)
      (adsDao.get _)
        .expects(id, AdsRequestTypes.Commercial)
        .returningT(None)
      (quotaDao.get _)
        .expects(Actual(id, allowDisabled = true, time = now))
        .returningT(Nil)
      (billing.getCallCampaign _).expects(clientDetails).returningZ(None)
      (clients.resolve _)
        .expects(id, false)
        .returningT(Some(clientDetails))

      client
        .getTariffsText(id)
        .provideConstantClock(now)
        .success
        .value shouldBe Set.empty
    }

    "not call billing if not quotas suffice" in {
      val clientDetails = ClientDetailsGen.next
      val id = clientDetails.clientId

      val usedRecord = AdsRequestDao.Record(id, AdsRequestTypes.CarsUsed)
      (adsDao.get _)
        .expects(id, AdsRequestTypes.CarsUsed)
        .returningT(Some(usedRecord))

      val comRecord = AdsRequestDao.Record(id, AdsRequestTypes.Commercial)
      (adsDao.get _)
        .expects(id, AdsRequestTypes.Commercial)
        .returningT(Some(comRecord))

      (quotaDao.get _)
        .expects(Actual(id, allowDisabled = true, time = now))
        .returningT(
          List(
            QuotaRequest(
              id,
              ProductId.QuotaPlacementCarsNew,
              QuotaRequest.Settings(1, 1, None),
              now
            )
          )
        )

      (billing.getCallCampaign _).expects(*).never()
      (clients.resolve _).expects(*, *).never()

      client
        .getTariffsText(id)
        .provideConstantClock(now)
        .success
        .value should contain theSameElementsAs Set(
        CarsNew,
        CarsUsed,
        Commercial
      )
    }

    "call billing if required" in {
      val clientDetails = ClientDetailsGen.next
      val id = clientDetails.clientId

      (adsDao.get _)
        .expects(id, AdsRequestTypes.CarsUsed)
        .returningT(None)

      val comRecord = AdsRequestDao.Record(id, AdsRequestTypes.Commercial)
      (adsDao.get _)
        .expects(id, AdsRequestTypes.Commercial)
        .returningT(Some(comRecord))

      (quotaDao.get _)
        .expects(Actual(id, allowDisabled = true, time = now))
        .returningT(
          List(
            QuotaRequest(
              id,
              ProductId.QuotaPlacementMoto,
              QuotaRequest.Settings(1, 1, None),
              now
            )
          )
        )

      (clients.resolve _)
        .expects(id, false)
        .returningT(Some(clientDetails))
      (billing.getCallCampaign _)
        .expects(clientDetails)
        .returningZ(Some(activeCampaignHeaderGen.next))

      client
        .getTariffsText(id)
        .provideConstantClock(now)
        .success
        .value should contain theSameElementsAs Set(CarsNew, Commercial, Moto)
    }

    "using quota requests" when {
      "new cars quota exist" should {
        "return carsNew" in {
          val clientDetails = ClientDetailsGen.next
          val id = clientDetails.clientId

          (adsDao.get _)
            .expects(id, AdsRequestTypes.CarsUsed)
            .returningT(None)
          (adsDao.get _)
            .expects(id, AdsRequestTypes.Commercial)
            .returningT(None)

          (quotaDao.get _)
            .expects(*)
            .returningT(
              List(
                QuotaRequest(
                  id,
                  ProductId.QuotaPlacementCarsNew,
                  QuotaRequest.Settings(1, 1, None),
                  now
                )
              )
            )

          client
            .getTariffsText(id)
            .success
            .value should contain theSameElementsAs Set(CarsNew)
        }
      }

      "new cars quota doesn't exist" when {
        "billing returns some campaigns" should {
          "return carsNew" in {
            val clientDetails = ClientDetailsGen.next
            val id = clientDetails.clientId

            (adsDao.get _)
              .expects(id, AdsRequestTypes.CarsUsed)
              .returningT(None)
            (adsDao.get _)
              .expects(id, AdsRequestTypes.Commercial)
              .returningT(None)

            (quotaDao.get _)
              .expects(*)
              .returningT(Nil)

            (clients.resolve _)
              .expects(id, false)
              .returningT(Some(clientDetails))
            (billing.getCallCampaign _)
              .expects(clientDetails)
              .returningZ(Some(activeCampaignHeaderGen.next))

            client
              .getTariffsText(id)
              .success
              .value should contain theSameElementsAs Set(CarsNew)
          }
        }

        "client doesn't exist" should {
          "return nothing" in {
            val clientDetails = ClientDetailsGen.next
            val id = clientDetails.clientId

            (adsDao.get _)
              .expects(id, AdsRequestTypes.CarsUsed)
              .returningT(None)
            (adsDao.get _)
              .expects(id, AdsRequestTypes.Commercial)
              .returningT(None)

            (quotaDao.get _)
              .expects(*)
              .returningT(Nil)

            (clients.resolve _)
              .expects(id, false)
              .returningT(None)

            client
              .getTariffsText(id)
              .success
              .value shouldBe Set.empty
          }
        }

        "client fails" should {
          "throw exception up" in {
            val clientDetails = ClientDetailsGen.next
            val id = clientDetails.clientId

            (adsDao.get _)
              .expects(id, AdsRequestTypes.CarsUsed)
              .returningT(None)
            (adsDao.get _)
              .expects(id, AdsRequestTypes.Commercial)
              .returningT(None)

            (quotaDao.get _)
              .expects(*)
              .returningT(Nil)

            val exception = new Throwable("client error")

            (clients.resolve _)
              .expects(id, false)
              .returning(Failure(exception))

            client
              .getTariffsText(id)
              .failure
              .exception shouldBe exception
          }
        }

        "billing doesn't exist" should {
          "return nothing" in {
            val clientDetails = ClientDetailsGen.next
            val id = clientDetails.clientId

            (adsDao.get _)
              .expects(id, AdsRequestTypes.CarsUsed)
              .returningT(None)
            (adsDao.get _)
              .expects(id, AdsRequestTypes.Commercial)
              .returningT(None)

            (quotaDao.get _)
              .expects(*)
              .returningT(Nil)

            (clients.resolve _)
              .expects(id, false)
              .returningT(Some(clientDetails))
            (billing.getCallCampaign _)
              .expects(clientDetails)
              .returningZ(None)

            client
              .getTariffsText(id)
              .success
              .value shouldBe Set.empty
          }
        }

        "billing fails" should {
          "throws exception up" in {
            val clientDetails = ClientDetailsGen.next
            val id = clientDetails.clientId

            (adsDao.get _)
              .expects(id, AdsRequestTypes.CarsUsed)
              .returningT(None)
            (adsDao.get _)
              .expects(id, AdsRequestTypes.Commercial)
              .returningT(None)

            (quotaDao.get _)
              .expects(*)
              .returningT(Nil)

            val exception = new Throwable("billing error")

            (clients.resolve _)
              .expects(id, false)
              .returningT(Some(clientDetails))
            (billing.getCallCampaign _)
              .expects(clientDetails)
              .returning(ZIO.fail(exception))

            client
              .getTariffsText(id)
              .failure
              .exception shouldBe exception
          }
        }
      }

      "moto quota doesn't exist" should {
        "return nothing" in {
          val clientDetails = ClientDetailsGen.next
          val id = clientDetails.clientId

          (adsDao.get _)
            .expects(id, AdsRequestTypes.CarsUsed)
            .returningT(None)
          (adsDao.get _)
            .expects(id, AdsRequestTypes.Commercial)
            .returningT(None)

          (quotaDao.get _)
            .expects(*)
            .returningT(
              List(
                QuotaRequest(
                  id,
                  ProductId.QuotaPlacementCarsNew,
                  QuotaRequest.Settings(1, 1, None),
                  now
                )
              )
            )

          (billing.getCallCampaign _).expects(*).never()
          (clients.resolve _).expects(*, *).never()

          client
            .getTariffsText(id)
            .success
            .value shouldNot contain(Moto)
        }
      }
    }
  }
}
