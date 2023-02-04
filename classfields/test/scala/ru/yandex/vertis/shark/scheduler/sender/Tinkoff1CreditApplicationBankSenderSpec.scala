package ru.yandex.vertis.shark.scheduler.sender

import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.client.bank.TinkoffBankClient
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter.{
  Source,
  TinkoffBankCarConverter,
  TinkoffBankConverter
}
import ru.yandex.vertis.shark.client.bank.converter.impl.{TinkoffBankAutoCreditConverter, TinkoffBankCarConverterImpl}
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.scheduler.sender.TestCases.{hasDriverLicenseBlock, withForeignPassportBlock}
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.Tinkoff1CreditApplicationBankSender.Tinkoff1BankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.client.vos.VosAutoruClient
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import zio.clock.Clock
import zio.test.environment.TestEnvironment
import zio.{RLayer, ULayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Tinkoff1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Tinkoff1CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.tinkoffBankAutoCreditClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.tinkoffBankAutoCreditClient) >>> TinkoffBankClient.live
    val generalConverter: ULayer[TinkoffBankConverter] =
      ZLayer.succeed(new TinkoffBankAutoCreditConverter)
    val targetConverter: ULayer[TinkoffBankCarConverter] =
      ZLayer.succeed(new TinkoffBankCarConverterImpl)
    val senderLayer: URLayer[Tinkoff1BankSenderEnvironment with Clock, CreditApplicationBankSender] = (
      for {
        generalConverter <- ZIO.service[TinkoffBankConverter.Service[Source]]
        targetConverter <- ZIO.service[TinkoffBankConverter.Service[Offer]]
        tinkoffBankClient <- ZIO.service[TinkoffBankClient.Service]
        vosAutoruClient <- ZIO.service[VosAutoruClient.Service]
        clockService <- ZIO.service[Clock.Service]
      } yield new Tinkoff1CreditApplicationBankSender(
        generalConverter,
        targetConverter,
        tinkoffBankClient,
        vosAutoruClient,
        clockService
      )
    ).toLayer

    ZLayer.requires[Clock] ++ clientLayer ++ generalConverter ++ targetConverter ++ vosAutoruClientLayer >>>
      senderLayer
  }

  override protected def foreignPassportYes: TestCases.TestCase =
    super.foreignPassportYes
      .withBuilder(_.setForeignPassport(withForeignPassportBlock).setDriverLicense(hasDriverLicenseBlock))
}
