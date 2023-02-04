package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.SovcomBankClient
import ru.yandex.vertis.shark.client.bank.converter.SovcomBankConverter
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.Sovcom1CreditApplicationBankSender.SovcomBankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import zio.test.environment.TestEnvironment
import zio.{RLayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Sovcom1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Sovcom1CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.sovcomBankClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.sovcomBankClient) >>> SovcomBankClient.live
    val converterLayer = SovcomBankConverter.live
    val senderLayer: URLayer[SovcomBankSenderEnvironment, CreditApplicationBankSender] = (for {
      bankClient <- ZIO.service[SovcomBankClient.Service]
      converter <- ZIO.service[SovcomBankConverter.Service]
    } yield new Sovcom1CreditApplicationBankSender(bankClient, converter)).toLayer

    clientLayer ++ converterLayer >>> senderLayer
  }

  override protected def driverLicenseNo: TestCases.TestCase =
    super.driverLicenseNo.withAssertion(TestCases.assertIsSome)

  override protected def foreignPassportNo: TestCases.TestCase =
    super.foreignPassportNo.withAssertion(TestCases.assertIsSome)
}
