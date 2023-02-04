package ru.yandex.vertis.shark.client.palma

import cats.implicits._
import ru.yandex.vertis.palma.service_common.Filter
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryServiceGrpc._
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.zio.grpc.client.config.GrpcClientConfig
import ru.yandex.vertis.zio_baker.zio.grpc.client.GrpcClient
import ru.yandex.vertis.zio_baker.zio.palma.PalmaClient
import ru.yandex.vertis.zio_baker.zio.palma.PalmaClient.PalmaType.scalapb
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.Assertion._
import zio.test._
import zio._

object CreditProductClientSpec extends DefaultRunnableSpec {

  private val palmaConfigLayer = ZLayer.succeed {
    GrpcClientConfig(
      endpoint = "palma-api-grpc-api.vrts-slb.test.vertis.yandex.net:80",
      maxRetryAttempts = 3.some
    )
  }

  private val palmaGrpcClientLayer = palmaConfigLayer >>>
    GrpcClient.live[ProtoDictionaryService](new ProtoDictionaryServiceStub(_))

  private val palmaClientLayer = palmaGrpcClientLayer >>> PalmaClient.live

  def spec: ZSpec[TestEnvironment, Any] =
    suite("CreditProductClient")(
      testM("list") {
        val result: ZIO[PalmaClient, Throwable, Seq[proto.CreditProduct]] =
          PalmaClient.list[proto.CreditProduct](Seq(Filter(BankIdFieldName, BankIdFieldValue)))
        assertM(result.map(_.map(_.bankId)))(equalTo(Seq(BankIdFieldValue))).provideLayer(palmaClientLayer)
      },
      testM("get None") {
        val result: ZIO[PalmaClient, Throwable, Option[proto.CreditProduct]] =
          PalmaClient.get[proto.CreditProduct](NoneCreditProductId)
        assertM(result)(isNone).provideLayer(palmaClientLayer)
      },
      testM("get Some") {
        val result: ZIO[PalmaClient, Throwable, Option[proto.CreditProduct]] =
          PalmaClient.get[proto.CreditProduct](SomeCreditProductId)
        assertM(result.map(_.map(_.id)))(equalTo(SomeCreditProductId.some)).provideLayer(palmaClientLayer)
      }
    ) @@ ignore

  private val BankIdFieldName: String = "bank_id"
  private val BankIdFieldValue: String = "tinkoff"
  private val NoneCreditProductId: String = "missing-id"
  private val SomeCreditProductId: String = "test2"
}
