package ru.yandex.vertis.shark.scheduler.sender

import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter.{Source, TinkoffBankConverter}
import ru.yandex.vertis.shark.client.bank.converter.impl.TinkoffBankCardCreditConverter
import ru.yandex.vertis.shark.client.bank.{TinkoffBankCardCreditReportsClient, TinkoffBankClient}
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.{TestSharedConfig, TmsStaticSamples}
import ru.yandex.vertis.shark.sender.impl.Tinkoff2CreditApplicationBankSender.Tinkoff2BankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import zio.clock.Clock
import zio.test.Assertion.isSome
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{RLayer, ULayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Tinkoff2CreditApplicationBankSenderCheckSpec extends DefaultRunnableSpec with TestLayers {

  override lazy val config: TestSharedConfig = TestSharedConfig.local

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {

    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.tinkoffBankCardCreditClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.tinkoffBankCardCreditClient) >>> TinkoffBankClient.live

    val clientCreditCardLayer =
      ZLayer.succeed(config.tinkoffBankCardCreditReportsClient.http) >>>
        HttpClient.asyncLayer ++ ZLayer.succeed(config.tinkoffBankCardCreditReportsClient) >>>
        TinkoffBankCardCreditReportsClient.live

    val converterLayer: ULayer[TinkoffBankConverter] =
      ZLayer.succeed(new TinkoffBankCardCreditConverter)

    val senderLayer: URLayer[Tinkoff2BankSenderEnvironment, CreditApplicationBankSender] = (
      for {
        converter <- ZIO.service[TinkoffBankConverter.Service[Source]]
        bankClient <- ZIO.service[TinkoffBankClient.Service]
        bankCreditCardClient <- ZIO.service[TinkoffBankCardCreditReportsClient.Service]
        clockService <- ZIO.service[Clock.Service]
      } yield new Tinkoff2CreditApplicationBankSender(converter, bankClient, bankCreditCardClient, clockService)
    ).toLayer

    ZLayer.requires[Clock] ++ clientLayer ++ clientCreditCardLayer ++ converterLayer >>> senderLayer
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Tinkoff2CreditApplicationBankSenderCheck")(
      testM("check") {
        val res = for {
          sender <- ZIO.service[CreditApplicationBankSender.Service]
          creditApplication = TmsStaticSamples.sampleCreditApplication
          claimId = "43b84b6d-69a2-4d49-9b11-ba13dbbe59b1".taggedWith[Tag.CreditApplicationClaimId]
          response <- sender.check(creditApplication, claimId)
        } yield {
          println(response)
          response
        }
        assertM(res)(isSome).provideLayer(testLayer)
      }
    ) @@ ignore

}
