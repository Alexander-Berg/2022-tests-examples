package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter.{Source, TinkoffBankConverter}
import ru.yandex.vertis.shark.client.bank.converter.impl.TinkoffBankCardCreditConverter
import ru.yandex.vertis.shark.client.bank.{TinkoffBankCardCreditReportsClient, TinkoffBankClient}
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.scheduler.sender.TestCases.{hasDriverLicenseBlock, withForeignPassportBlock}
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.Tinkoff2CreditApplicationBankSender.Tinkoff2BankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import zio.clock.Clock
import zio.test.environment.TestEnvironment
import zio.{RLayer, ULayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Tinkoff2CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Tinkoff2CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val clientSendLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.tinkoffBankCardCreditClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.tinkoffBankCardCreditClient) >>> TinkoffBankClient.live

    val clientReportsLayer =
      ZLayer.succeed(config.tinkoffBankCardCreditReportsClient.http) >>>
        HttpClient.asyncLayer ++ ZLayer.succeed(config.tinkoffBankCardCreditReportsClient) >>>
        TinkoffBankCardCreditReportsClient.live

    val converter: ULayer[TinkoffBankConverter] =
      ZLayer.succeed(new TinkoffBankCardCreditConverter)
    val senderLayer: URLayer[Tinkoff2BankSenderEnvironment with Clock, CreditApplicationBankSender] = (
      for {
        converter <- ZIO.service[TinkoffBankConverter.Service[Source]]
        bankClient <- ZIO.service[TinkoffBankClient.Service]
        bankCreditCardClient <- ZIO.service[TinkoffBankCardCreditReportsClient.Service]
        clockService <- ZIO.service[Clock.Service]
      } yield new Tinkoff2CreditApplicationBankSender(converter, bankClient, bankCreditCardClient, clockService)
    ).toLayer
    ZLayer.requires[Clock] ++ clientSendLayer ++ clientReportsLayer ++ converter >>> senderLayer
  }

  override protected def foreignPassportYes: TestCases.TestCase =
    super.foreignPassportYes
      .withBuilder(_.setForeignPassport(withForeignPassportBlock).setDriverLicense(hasDriverLicenseBlock))
}
