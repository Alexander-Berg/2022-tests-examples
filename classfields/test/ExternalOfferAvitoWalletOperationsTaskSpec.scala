package auto.dealers.multiposting.scheduler.test

import common.scalapb.ScalaProtobuf
import auto.dealers.multiposting.clients.s3.testkit.S3MultipostingClientMock
import auto.dealers.multiposting.model.EventType
import auto.dealers.multiposting.model.ExternalOfferDealerOperations
import ru.auto.multiposting.operation_model.AvitoWalletOperation
import auto.dealers.multiposting.scheduler.config.AvitoWalletOperationsToBrokerConfig
import auto.dealers.multiposting.scheduler.task.ExternalOfferAvitoWalletOperationsTask
import auto.dealers.multiposting.scheduler.task.ExternalOfferAvitoWalletOperationsTask.kopeks
import auto.dealers.multiposting.scheduler.testkit.AvitoWalletOperationBrokerClientMock
import auto.dealers.multiposting.storage.testkit.{AvitoWalletOperationDaoMock, ExternalOfferEventFileDaoMock}
import zio.ZLayer
import zio.clock.Clock
import zio.stream.ZStream
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.mock.Expectation._

import java.time.{OffsetDateTime, ZoneOffset}
import cats.data.NonEmptySet
import io.prometheus.client.CollectorRegistry
import common.ops.prometheus.CollectorRegistryWrapper
import auto.dealers.multiposting.storage.testkit.ExternalOfferEventFileDaoMock

object ExternalOfferAvitoWalletOperationsTaskSpec extends DefaultRunnableSpec {
  val bucket = "bucket"
  val filenamePrefix: String = ExternalOfferAvitoWalletOperationsTask.filenamePrefix
  val task = new ExternalOfferAvitoWalletOperationsTask(new CollectorRegistryWrapper(CollectorRegistry.defaultRegistry))
  val cfg = ZLayer.succeed(AvitoWalletOperationsToBrokerConfig(bucket))

  val filename1 = s"$filenamePrefix-1"
  val filename2 = s"$filenamePrefix-2"
  val filename3 = s"$filenamePrefix-3"

  val templateAvitoOperation = ExternalOfferDealerOperations.Operation(
    updatedAt = OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"),
    operationType = "резервирование средств под услугу",
    serviceType = Some("perf_vas"),
    serviceId = Some(23),
    serviceName = Some("До 5 раз больше просмотров на 1 день"),
    operationName = "Резервирование средств",
    amountTotal = 559,
    amountRub = 533.25,
    amountBonus = 25.75,
    itemId = Some(2088927989)
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ExternalOfferAvitoWalletOperationsTask")(
      testM("do nothing if no filenames exist") {
        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.AvitoWalletOperation),
          value(None)
        )
        val avitoDao = AvitoWalletOperationDaoMock
          .FindLastOperationTimestamps(
            equalTo(NonEmptySet.of(1L)),
            value(Map.empty)
          )
          .atMost(0)
        val s3 = S3MultipostingClientMock.ListNewObjects(
          equalTo((bucket, filenamePrefix, None)),
          value(List.empty[String])
        )
        val broker = AvitoWalletOperationBrokerClientMock.empty

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ avitoDao ++ s3 ++ cfg ++ broker)
      },
      testM("do nothing if no new filenames exist") {
        val avitoDao = AvitoWalletOperationDaoMock
          .FindLastOperationTimestamps(
            equalTo(NonEmptySet.of(1L)),
            value(Map.empty)
          )
          .atMost(0)
        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.AvitoWalletOperation),
          value(Some(filename2))
        )
        val s3 = S3MultipostingClientMock.ListNewObjects(
          equalTo((bucket, filenamePrefix, Some(filename2))),
          value(List.empty)
        )
        val broker = AvitoWalletOperationBrokerClientMock.empty

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ avitoDao ++ s3 ++ cfg ++ broker)
      },
      testM("insert alphabetically latest filenames") {
        val json = """
          {
            "timestamp": 1617127814,
            "autoru_client_id": 3168,
            "operations": [
              {
                "updatedAt": "2021-04-01T18:03:01.062536+03:00",
                "operationType": "резервирование средств под услугу",
                "serviceType": "perf_vas",
                "serviceId": 23,
                "serviceName": "До 5 раз больше просмотров на 1 день",
                "operationName": "Резервирование средств",
                "amountTotal": 559,
                "amountRub": 533.25,
                "amountBonus": 25.75,
                "itemId": 2088927989
              }
            ]
          }
        """

        val operation = templateAvitoOperation.copy(
          updatedAt = OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"),
          operationType = "резервирование средств под услугу",
          operationName = "Резервирование средств",
          serviceId = Some(23L),
          serviceName = Some("До 5 раз больше просмотров на 1 день")
        )

        val expected = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"))),
          operationId = operation.operationIdHash(3168),
          clientId = 3168,
          serviceId = 23,
          serviceName = "До 5 раз больше просмотров на 1 день",
          operationName = "Резервирование средств",
          operationType = "резервирование средств под услугу",
          serviceType = "perf_vas",
          itemId = 2088927989,
          isVas = true,
          amountTotalKop = 55900L,
          amountRubKop = 53325L,
          amountBonusKop = 2575L
        )

        val avitoDao = AvitoWalletOperationDaoMock
          .FindLastOperationTimestamps(
            equalTo(NonEmptySet.of(3168L)),
            value(Map(3168L -> OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
          ) ++ AvitoWalletOperationDaoMock
          .FindLastOperationTimestamps(
            equalTo(NonEmptySet.of(3168L)),
            value(Map(3168L -> OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
          )
        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.AvitoWalletOperation),
          value(Some(filename1))
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(
          equalTo((EventType.AvitoWalletOperation, filename2)),
          unit
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(
          equalTo((EventType.AvitoWalletOperation, filename3)),
          unit
        )

        val s3 = S3MultipostingClientMock.ListNewObjects(
          equalTo((bucket, filenamePrefix, Some(filename1))),
          value(List(filename2, filename3))
        ) ++
          S3MultipostingClientMock.ReadLines(equalTo((bucket, filename2)), value(ZStream.succeed(json))) ++
          S3MultipostingClientMock.ReadLines(equalTo((bucket, filename3)), value(ZStream.succeed(json)))

        val broker = AvitoWalletOperationBrokerClientMock
          .Send(
            equalTo((expected, None, None)),
            unit
          )
          .atLeast(2)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ avitoDao ++ s3 ++ cfg ++ broker)
      },
      testM("filter processed operations") {
        val json = """
          {
            "timestamp": 1617127814,
            "autoru_client_id": 3168,
            "operations": [
              {
                "updatedAt": "2021-04-01T18:03:01.062536+03:00",
                "operationType": "резервирование средств под услугу",
                "serviceType": "perf_vas",
                "serviceId": 23,
                "serviceName": "До 5 раз больше просмотров на 1 день",
                "operationName": "Резервирование средств",
                "amountTotal": 559,
                "amountRub": 533.25,
                "amountBonus": 25.75,
                "itemId": 2088927989
              }
            ]
          }
        """

        val operation = templateAvitoOperation.copy(
          updatedAt = OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"),
          operationType = "резервирование средств под услугу",
          operationName = "Резервирование средств",
          serviceId = Some(23L),
          serviceName = Some("До 5 раз больше просмотров на 1 день")
        )

        val expected = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"))),
          operationId = operation.operationIdHash(3168),
          clientId = 3168,
          serviceId = 23,
          serviceName = "До 5 раз больше просмотров на 1 день",
          operationName = "Резервирование средств",
          operationType = "резервирование средств под услугу",
          serviceType = "perf_vas",
          itemId = 2088927989,
          isVas = true,
          amountTotalKop = 55900L,
          amountRubKop = 53325L,
          amountBonusKop = 2575L
        )

        val avitoDao = AvitoWalletOperationDaoMock
          .FindLastOperationTimestamps(
            equalTo(NonEmptySet.of(3168L)),
            value(Map(3168L -> OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
          )
        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.AvitoWalletOperation),
          value(Some(filename1))
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(
          equalTo((EventType.AvitoWalletOperation, filename2)),
          unit
        )

        val s3 = S3MultipostingClientMock.ListNewObjects(
          equalTo((bucket, filenamePrefix, Some(filename1))),
          value(List(filename2))
        ) ++
          S3MultipostingClientMock.ReadLines(equalTo((bucket, filename2)), value(ZStream.succeed(json)))

        val broker = AvitoWalletOperationBrokerClientMock
          .Send(
            equalTo((expected, None, None)),
            unit
          )
          .atMost(0)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ avitoDao ++ s3 ++ cfg ++ broker)
      },
      testM("filter operations with unknown service type") {
        val json = """
          {
            "timestamp": 1617127814,
            "autoru_client_id": 3168,
            "operations": [
              {
                "updatedAt": "2021-04-01T18:03:01.062536+03:00",
                "operationType": "резервирование средств под услугу",
                "serviceType": "random_service_type",
                "serviceId": 23,
                "serviceName": "До 5 раз больше просмотров на 1 день",
                "operationName": "Резервирование средств",
                "amountTotal": 559,
                "amountRub": 533.25,
                "amountBonus": 25.75,
                "itemId": 2088927989
              }
            ]
          }
        """

        val operation = templateAvitoOperation.copy(
          updatedAt = OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"),
          operationType = "резервирование средств под услугу",
          operationName = "Резервирование средств",
          serviceId = Some(23L),
          serviceName = Some("До 5 раз больше просмотров на 1 день")
        )

        val expected = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"))),
          operationId = operation.operationIdHash(3168),
          clientId = 3168,
          serviceId = 23,
          serviceName = "До 5 раз больше просмотров на 1 день",
          operationName = "Резервирование средств",
          operationType = "резервирование средств под услугу",
          serviceType = "perf_vas",
          itemId = 2088927989,
          isVas = true,
          amountTotalKop = 55900L,
          amountRubKop = 53325L,
          amountBonusKop = 2575L
        )

        val avitoDao = AvitoWalletOperationDaoMock
          .FindLastOperationTimestamps(
            equalTo(NonEmptySet.of(3168L)),
            value(Map.empty)
          )
        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.AvitoWalletOperation),
          value(Some(filename1))
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(
          equalTo((EventType.AvitoWalletOperation, filename2)),
          unit
        )

        val s3 = S3MultipostingClientMock.ListNewObjects(
          equalTo((bucket, filenamePrefix, Some(filename1))),
          value(List(filename2))
        ) ++
          S3MultipostingClientMock.ReadLines(equalTo((bucket, filename2)), value(ZStream.succeed(json)))

        val broker = AvitoWalletOperationBrokerClientMock
          .Send(
            equalTo((expected, None, None)),
            unit
          )
          .atMost(0)

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ avitoDao ++ s3 ++ cfg ++ broker)
      },
      testM("correctly process first operation") {
        val json = """
          {
            "timestamp": 1617127814,
            "autoru_client_id": 3168,
            "operations": [
              {
                "updatedAt": "2021-04-01T18:03:01.062536+03:00",
                "operationType": "резервирование средств под услугу",
                "serviceType": "perf_vas",
                "serviceId": 23,
                "serviceName": "До 5 раз больше просмотров на 1 день",
                "operationName": "Резервирование средств",
                "amountTotal": 559,
                "amountRub": 533.25,
                "amountBonus": 25.75,
                "itemId": 2088927989
              }
            ]
          }
        """

        val operation = templateAvitoOperation.copy(
          updatedAt = OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"),
          operationType = "резервирование средств под услугу",
          operationName = "Резервирование средств",
          serviceId = Some(23L),
          serviceName = Some("До 5 раз больше просмотров на 1 день")
        )

        val expected = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(OffsetDateTime.parse("2021-04-01T18:03:01.062536+03:00"))),
          operationId = operation.operationIdHash(3168),
          clientId = 3168,
          serviceId = 23,
          serviceName = "До 5 раз больше просмотров на 1 день",
          operationName = "Резервирование средств",
          operationType = "резервирование средств под услугу",
          serviceType = "perf_vas",
          itemId = 2088927989,
          isVas = true,
          amountTotalKop = 55900L,
          amountRubKop = 53325L,
          amountBonusKop = 2575L
        )

        val avitoDao = AvitoWalletOperationDaoMock
          .FindLastOperationTimestamps(
            equalTo(NonEmptySet.of(3168L)),
            value(Map.empty[Long, OffsetDateTime])
          )
        val fileDao = ExternalOfferEventFileDaoMock.GetLastProcessedFilename(
          equalTo(EventType.AvitoWalletOperation),
          value(Some(filename1))
        ) ++ ExternalOfferEventFileDaoMock.InsertNewFilename(
          equalTo((EventType.AvitoWalletOperation, filename2)),
          unit
        )

        val s3 = S3MultipostingClientMock.ListNewObjects(
          equalTo((bucket, filenamePrefix, Some(filename1))),
          value(List(filename2))
        ) ++
          S3MultipostingClientMock.ReadLines(equalTo((bucket, filename2)), value(ZStream.succeed(json)))

        val broker = AvitoWalletOperationBrokerClientMock
          .Send(
            equalTo((expected, None, None)),
            unit
          )

        assertM(task.program)(isUnit)
          .provideCustomLayer(Clock.live ++ fileDao ++ avitoDao ++ s3 ++ cfg ++ broker)
      },
      testM("avito wallet message of type Placement") {
        val clientId = 3168

        val input = templateAvitoOperation.copy(
          serviceType = Some("tariff")
        )

        val expected = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(input.updatedAt)),
          operationId = input.operationIdHash(clientId),
          clientId,
          serviceId = input.serviceId.get,
          serviceName = input.serviceName.get,
          operationName = input.operationName,
          operationType = input.operationType,
          serviceType = "tariff",
          itemId = input.itemId.get,
          isPlacement = true,
          amountTotalKop = (input.amountTotal * kopeks).toLong,
          amountRubKop = (input.amountRub * kopeks).toLong,
          amountBonusKop = (input.amountBonus * kopeks).toLong
        )

        assertM(task.operationToProto(clientId, input))(equalTo(expected))
      },
      testM("avito wallet message of type Other") {
        val clientId = 3168

        val input = templateAvitoOperation.copy(
          serviceType = None
        )

        val expected = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(input.updatedAt)),
          operationId = input.operationIdHash(clientId),
          clientId,
          serviceId = input.serviceId.get,
          serviceName = input.serviceName.get,
          operationName = input.operationName,
          operationType = input.operationType,
          serviceType = "",
          itemId = input.itemId.get,
          isOther = true,
          amountTotalKop = (input.amountTotal * kopeks).toLong,
          amountRubKop = (input.amountRub * kopeks).toLong,
          amountBonusKop = (input.amountBonus * kopeks).toLong
        )

        assertM(task.operationToProto(clientId, input))(equalTo(expected))
      },
      testM("avito wallet message negative amount when operationType == `чарджбэк'") {
        val clientId = 3168

        val input = templateAvitoOperation.copy(
          operationType = "чарджбэк"
        )

        val expected = AvitoWalletOperation(
          timestamp = Some(ScalaProtobuf.toTimestamp(input.updatedAt)),
          operationId = input.operationIdHash(clientId),
          clientId,
          serviceId = input.serviceId.get,
          serviceName = input.serviceName.get,
          operationName = input.operationName,
          operationType = input.operationType,
          serviceType = "perf_vas",
          itemId = input.itemId.get,
          isVas = true,
          amountTotalKop = (input.amountTotal * -kopeks).toLong,
          amountRubKop = (input.amountRub * -kopeks).toLong,
          amountBonusKop = (input.amountBonus * -kopeks).toLong
        )

        assertM(task.operationToProto(clientId, input))(equalTo(expected))
      }
    ) @@ sequential

}
