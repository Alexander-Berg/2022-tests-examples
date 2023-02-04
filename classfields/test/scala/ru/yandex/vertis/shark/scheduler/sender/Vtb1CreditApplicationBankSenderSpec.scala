package ru.yandex.vertis.shark.scheduler.sender

import ru.yandex.vertis.shark.client.bank.VtbBankClient
import ru.yandex.vertis.shark.client.bank.converter.{VtbFullAppConverter, VtbMiniAppConverter}
import ru.yandex.vertis.shark.client.bank.dictionary.vtb.{StaticVtbResource, VtbDictionary}
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.impl.Vtb1CreditApplicationBankSender.VtbSenderEnvironment
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import common.id.IdGenerator
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.test.environment.TestEnvironment
import zio.{RLayer, URLayer, ZEnv, ZLayer, ZManaged}
import zio.blocking.Blocking

object Vtb1CreditApplicationBankSenderSpec extends TestCreditApplicationBankSender {

  override def creditProduct: CreditProductId = Vtb1CreditApplicationBankSender.CreditProductId

  override def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender] = {

    val cfg = config.vtbClient
    val clientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(cfg.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(cfg) >>> VtbBankClient.live

    val staticVtbResourceLayer = ZLayer.succeed[Resource[Any, VtbDictionary.Service]](new StaticVtbResource)

    val miniConverterLayer =
      regionsDictionaryLayer ++
        IdGenerator.snowflake ++
        staticVtbResourceLayer ++
        ZLayer.succeed(cfg) >>>
        VtbMiniAppConverter.live

    val fullConverterLayer =
      regionsDictionaryLayer ++
        staticVtbResourceLayer ++
        ZLayer.succeed(cfg) >>>
        VtbFullAppConverter.live

    val senderLayer: URLayer[VtbSenderEnvironment, CreditApplicationBankSender] = (for {
      bankClient <- ZManaged.service[VtbBankClient.Service]
      miniAppConverter <- ZManaged.service[VtbMiniAppConverter.Service]
      fullAppConverter <- ZManaged.service[VtbFullAppConverter.Service]
    } yield new Vtb1CreditApplicationBankSender(bankClient, miniAppConverter, fullAppConverter, cfg)).toLayer

    clientLayer ++ miniConverterLayer ++ fullConverterLayer >>> senderLayer
  }
}
