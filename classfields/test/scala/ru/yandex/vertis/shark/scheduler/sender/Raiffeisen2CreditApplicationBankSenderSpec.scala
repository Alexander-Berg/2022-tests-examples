package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.RaiffeisenBankClient
import ru.yandex.vertis.shark.client.bank.converter.RaiffeisenBankConverter
import ru.yandex.vertis.shark.client.bank.dictionary.raiffeizen.{RaiffeisenBankDictionary, StaticRaiffeisenBankResource}
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.Raiffeisen2CreditApplicationBankSender.Raiffeisen2BankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.test.environment.TestEnvironment
import zio.{RLayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

class Raiffeisen2CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Raiffeisen2CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.raiffeisenBankClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.raiffeisenBankClient) >>> RaiffeisenBankClient.live
    val converterLayer =
      ZLayer.succeed[Resource[Any, RaiffeisenBankDictionary.Service]](new StaticRaiffeisenBankResource) >>>
        RaiffeisenBankConverter.liveRefinancing
    val senderLayer: URLayer[Raiffeisen2BankSenderEnvironment, CreditApplicationBankSender] = (for {
      bankClient <- ZIO.service[RaiffeisenBankClient.Service]
      converter <- ZIO.service[RaiffeisenBankConverter.Service]
    } yield new Raiffeisen2CreditApplicationBankSender(converter, bankClient)).toLayer

    clientLayer ++ converterLayer >>> senderLayer
  }

  override protected def selfEmployee: TestCases.TestCase =
    super.selfEmployee.withAssertion(TestCases.assertValidationError)

  override protected def pensionAge: TestCases.TestCase =
    super.pensionAge.withAssertion(TestCases.assertValidationError)

  override protected def driverLicenseNo: TestCases.TestCase =
    super.driverLicenseNo.withAssertion(TestCases.assertIsSome)

  override protected def foreignPassportNo: TestCases.TestCase =
    super.foreignPassportNo.withAssertion(TestCases.assertIsSome)

}
