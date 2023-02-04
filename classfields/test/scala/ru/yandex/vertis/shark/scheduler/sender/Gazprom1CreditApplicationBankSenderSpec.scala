package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.GazpromBankClient
import ru.yandex.vertis.shark.client.bank.converter.GazpromBankConverter
import ru.yandex.vertis.shark.client.bank.dictionary.gazprom.{GazpromBankDictionary, StaticGazpromBankResource}
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.Gazprom1CreditApplicationBankSender.GazpromBankSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.client.geocoder.GeocoderClient
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.clock.Clock
import zio.test.environment.TestEnvironment
import zio.test.mock.mockable
import zio.{RLayer, URLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Gazprom1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  @mockable[GeocoderClient.Service]
  object GeocoderClientMock

  override def creditProduct: CreditProductId = Gazprom1CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.gazpromBankClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.gazpromBankClient) >>> GazpromBankClient.live
    val converterLayer = ZLayer.succeed[Resource[Any, GazpromBankDictionary.Service]](new StaticGazpromBankResource) ++
      GeocoderClientMock.empty ++ Clock.any ++ regionsDictionaryLayer >>> GazpromBankConverter.live
    val senderLayer: URLayer[GazpromBankSenderEnvironment, CreditApplicationBankSender] = (for {
      bankClient <- ZIO.service[GazpromBankClient.Service]
      converter <- ZIO.service[GazpromBankConverter.Service]
    } yield new Gazprom1CreditApplicationBankSender(bankClient, converter)).toLayer

    clientLayer ++ converterLayer >>> senderLayer
  }

  override protected def selfEmployee: TestCases.TestCase =
    super.selfEmployee.withAssertion(TestCases.assertIsSome)

  override protected def pensionAge: TestCases.TestCase =
    super.pensionAge.withAssertion(TestCases.assertValidationError)

  override protected def driverLicenseNo: TestCases.TestCase =
    super.driverLicenseNo.withAssertion(TestCases.assertIsSome)

  override protected def foreignPassportNo: TestCases.TestCase =
    super.foreignPassportNo.withAssertion(TestCases.assertIsSome)
}
