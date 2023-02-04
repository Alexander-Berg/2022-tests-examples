package ru.yandex.vertis.shark

import com.yandex.ydb.table.transaction.TransactionMode
import ru.yandex.vertis.zio_baker.geobase.{GeobaseParser, Tree}
import ru.yandex.vertis.shark.dictionary.impl.RegionsDictionaryImpl
import ru.yandex.vertis.shark.dictionary.{CreditProductDictionary, DealerConfigurationDictionary, RegionsDictionary}
import ru.yandex.vertis.shark.enricher.CreditProductEnricher
import ru.yandex.vertis.ydb.RetryOptions
import ru.yandex.vertis.ydb.zio.{Tx, TxEnv, TxError}
import ru.yandex.vertis.zio_baker.zio.client.geocoder.GeocoderClient
import ru.yandex.vertis.zio_baker.zio.client.vos.VosAutoruClient
import ru.yandex.vertis.zio_baker.zio.dao.TransactionSupport
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResourceSpecBase
import zio.{ULayer, ZIO, ZLayer}
import zio.clock.Clock
import zio.test.mock.Expectation.value
import zio.test.mock.mockable

import scala.util.{Try, Using}

object Mock {

  @mockable[VosAutoruClient.Service]
  object VosAutoruClientMock

  @mockable[CreditProductDictionary.Service]
  object CreditProductDictionaryMock

  @mockable[GeocoderClient.Service]
  object GeocoderClientMock

  @mockable[DealerConfigurationDictionary.Service]
  object DealerConfigurationDictionaryMock

  @mockable[CreditProductEnricher.Service]
  object CreditProductEnricherMock

  object FakeTransactionSupport extends TransactionSupport.Service {

    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    override def transactionally[R <: Clock, E, T](
        mode: TransactionMode,
        retryOptions: RetryOptions
      )(action: Tx[R, E, T]): ZIO[R, TxError[E], T] =
      for {
        r <- ZIO.environment[R]
        txEnv <- TxEnv.make(null, r)
        t <- action.provide(txEnv)
      } yield t
  }

  val fakeTransactionSupportLayer: ULayer[TransactionSupport] =
    ZLayer.succeed(FakeTransactionSupport)

  private lazy val resourceRegionsDictionaryTry: Try[RegionsDictionary.Service] = {
    val resourcePath = "/regions.xml"
    Using(
      Option(RegionsResourceSpecBase.getClass.getResourceAsStream(resourcePath))
        .getOrElse(sys.error(s"Resource not found: $resourcePath"))
    )(GeobaseParser.parse)
      .map(regions => new RegionsDictionaryImpl(new Tree(regions)))
  }

  /**
    * Lightweight RegionsDictionary stub using a resource file.
    */
  val resourceRegionsDictionaryLayer: ULayer[RegionsDictionary] =
    ZLayer.fromEffect(
      ZIO.fromTry(resourceRegionsDictionaryTry).orDie
    )
}
