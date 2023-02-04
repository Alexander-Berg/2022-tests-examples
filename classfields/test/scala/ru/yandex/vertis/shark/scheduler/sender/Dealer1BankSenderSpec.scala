package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.EcreditClient
import ru.yandex.vertis.shark.client.bank.EcreditClient.EcreditClient
import ru.yandex.vertis.shark.client.bank.converter.DealerApplicationConverter.DealerApplicationConverter
import ru.yandex.vertis.shark.client.bank.converter.EcreditEditAppConverter.EcreditEditAppConverter
import ru.yandex.vertis.shark.client.bank.converter.EcreditNewAppConverter.EcreditNewAppConverter
import ru.yandex.vertis.shark.client.bank.converter.{
  DealerApplicationConverter,
  EcreditEditAppConverter,
  EcreditNewAppConverter
}
import ru.yandex.vertis.shark.client.bank.dictionary.ecredit.{EcreditMarksDictionary, StaticEcreditMarksResource}
import ru.yandex.vertis.shark.client.dealerapplication.DealerApplicationClient
import ru.yandex.vertis.shark.dictionary.DealerConfigurationDictionary
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender.CreditApplicationBankSender
import ru.yandex.vertis.shark.sender.impl.Dealer1BankSender
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.palma.PalmaClient
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.test.environment.TestEnvironment
import zio.{RLayer, ZIO, ZLayer}
import zio.blocking.Blocking

object Dealer1BankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Dealer1BankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {
    val palmaClientLayer = tvmPalmaGrpcClientLayer >>> PalmaClient.live
    val appClientLayer = palmaClientLayer >>> DealerApplicationClient.live
    val appConverterLayer = dealerConfDictionaryLayer >>> DealerApplicationConverter.live

    val ecreditClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(config.ecreditClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.ecreditClient) >>> EcreditClient.live
    val ecreditNewAppConverterLayer =
      ZLayer.succeed[Resource[Any, EcreditMarksDictionary.Service]](new StaticEcreditMarksResource) >>>
        EcreditMarksDictionary.live >>>
        EcreditNewAppConverter.live
    val ecreditEditAppConverter = EcreditEditAppConverter.live

    type Dealer1BankSenderEnv = DealerApplicationClient
      with DealerApplicationConverter
      with DealerConfigurationDictionary
      with EcreditClient
      with EcreditNewAppConverter
      with EcreditEditAppConverter

    val senderLayer: RLayer[Dealer1BankSenderEnv, CreditApplicationBankSender] =
      (for {
        client <- ZIO.service[DealerApplicationClient.Service]
        converter <- ZIO.service[DealerApplicationConverter.Service]
        dealerConfiguration <- ZIO.service[DealerConfigurationDictionary.Service]
        ecreditClient <- ZIO.service[EcreditClient.Service]
        ecreditNewAppConverter <- ZIO.service[EcreditNewAppConverter.Service]
        ecreditEditAppConverter <- ZIO.service[EcreditEditAppConverter.Service]
      } yield new Dealer1BankSender(
        client,
        converter,
        dealerConfiguration,
        ecreditClient,
        ecreditNewAppConverter,
        ecreditEditAppConverter
      )).toLayer

    appClientLayer ++ appConverterLayer ++ dealerConfDictionaryLayer ++
      ecreditClientLayer ++ ecreditNewAppConverterLayer ++ ecreditEditAppConverter >>> senderLayer
  }
}
