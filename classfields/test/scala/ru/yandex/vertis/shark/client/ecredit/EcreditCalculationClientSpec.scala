package ru.yandex.vertis.shark.client.ecredit

import ru.yandex.vertis.shark.config.EcreditCalculationClientConfig
import ru.yandex.vertis.shark.model.Api.EcreditCalculationRequest
import ru.yandex.vertis.shark.model.UserRef.AutoruDealer
import ru.yandex.vertis.shark.proto.model.Api.EcreditCalculationRequest.{CreditSubtype, GosSubsidyType, IncomeProof}
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio.test.Assertion.isUnit
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{ZIO, ZLayer}
import zio.blocking.Blocking

object EcreditCalculationClientSpec extends DefaultRunnableSpec {

  private val config = EcreditCalculationClientConfig(
    http = HttpClientConfig(
      url = "https://calculation.e-credit.one"
    )
  )

  private lazy val httpClientBackendLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(config.http) >>> HttpClient.blockingLayer

  private lazy val layer = ZLayer.succeed(config) ++ httpClientBackendLayer >>> EcreditCalculationClient.live

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("EcreditCalculationClient") {
      testM("eCredit returns result") {
        val res = for {
          client <- ZIO.service[EcreditCalculationClient.Service]
          configs <- client.calculation(
            externalId = "2313",
            request = EcreditCalculationRequest(
              dealerRef = AutoruDealer(2313L),
              carName = EmptyString,
              used = true,
              carAge = None,
              price = 778000L,
              period = Seq(12),
              initialFeeMoney = None,
              initialFee = None,
              buybackPayment = None,
              creditSubtype = Seq(CreditSubtype.CLASSIC),
              creditSubtypeHide = Seq.empty,
              bankCreditsClients = None,
              gosSubsideType = GosSubsidyType.UNKNOWN_GOS_SUBTYPE,
              dohodConfirmType = IncomeProof.UNKNOWN_INCOME_PROOF,
              kaskoInsurancePrice = None,
              kaskoInsuranceTerm = None,
              kaskoInsuranceInCredit = None,
              requiredKasko = None,
              lifeInsurancePrice = None,
              lifeInsuranceTerm = None,
              lifeInsuranceInCredit = None,
              requiredLife = None,
              gapInsurancePrice = None,
              gapInsuranceTerm = None,
              gapInsuranceInCredit = None,
              requiredGap = None,
              otherPrice = None,
              otherPriceInCredit = None,
              equipmentPrice = None,
              equipmentPriceInCredit = None
            )
          )
        } yield println(configs)
        assertM(res)(isUnit).provideLayer(layer)
      }
    } @@ ignore
  }
}
