package ru.yandex.vertis.shark

import baker.common.client.dadata.DadataClientConfig
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase
import ru.yandex.vertis.application.environment.{Configuration, Environments}
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.shark.config._
import ru.yandex.vertis.shark.resource.{BankResource, CreditProductResource, FiasGeoMappingResource}
import ru.yandex.vertis.zio_baker.zio.client.vos.VosAutoruClientConfig
import ru.yandex.vertis.zio_baker.zio.grpc.client.config.GrpcClientConfig
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResource
import common.zio.clients.s3.S3Client
import ru.yandex.vertis.zio_baker.zio.tvm.config.TvmConfig

case class TestSharedConfig(
    tvm: TvmConfig,
    s3Client: S3Client.S3Config,
    regionsResource: RegionsResource.Config,
    dadataClient: DadataClientConfig,
    vosAutoruClient: VosAutoruClientConfig,
    palmaGrpcClient: GrpcClientConfig,
    tinkoffBankAutoCreditClient: TinkoffBankClientConfig,
    tinkoffBankCardCreditClient: TinkoffBankClientConfig,
    tinkoffBankCardCreditReportsClient: TinkoffBankCardCreditReportsClientConfig,
    raiffeisenBankClient: RaiffeisenBankClientConfig,
    gazpromBankClient: GazpromBankClientConfig,
    rosgosstrahBankClient: RosgosstrahBankClientConfig,
    sovcomBankClient: SovcomBankClientConfig,
    alfaBankClient: AlfaBankClientConfig,
    sravniRuClient: SravniRuClientConfig,
    vtbClient: VtbClientConfig,
    psbClientConfig: PsbClientConfig,
    ecreditClient: EcreditClientConfig,
    s3Edr: S3EdrConfig,
    creditProductResource: CreditProductResource.Config,
    bankResource: BankResource.Config,
    fiasGeoMappingResource: FiasGeoMappingResource.Config) {
  require(VertisRuntime.environment == Environments.Local, "only for local launch")
}

object TestSharedConfig {

  private val _emptyConfig = ConfigFactory.empty()

  def local: TestSharedConfig =
    Configuration
      .application(Environments.Local, _emptyConfig, _emptyConfig)
      .getConfig("shared")
      .resolve()
      .as[TestSharedConfig]
}
