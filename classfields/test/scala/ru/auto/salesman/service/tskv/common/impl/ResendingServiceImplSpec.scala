package ru.auto.salesman.service.tskv.common.impl

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.exceptions.CompositeException
import ru.auto.salesman.model.broker.MessageId
import ru.auto.salesman.model.log.buffer.{
  LogBufferEntry,
  LogBufferEntryId,
  LogBufferEntryPayload,
  LogBufferEntryType
}
import ru.auto.salesman.service.broker.LogsBrokerService
import ru.auto.salesman.service.log.buffer.LogBuffer
import ru.auto.salesman.service.tskv.common.impl.ResendingServiceImpl.executeAllWithResultsPar
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.tskv.SalesmanStatisticLog.SalesmanStatisticLogEntry
import ru.auto.salesman.util.copypaste.Protobuf._
import zio.ZIO

class ResendingServiceImplSpec extends BaseSpec {
  import ResendingServiceImplSpec._

  "ResendLogsToBrokerTask" should {
    val buffer = mock[LogBuffer]
    val brokerService = mock[LogsBrokerService]
    val task = new ResendingServiceImpl(
      buffer,
      brokerService
    )

    "send all logs to broker" in {
      val recordsIdBuffer = 5
      mockBuffer(recordsIdBuffer, testBatchSize)

      (brokerService
        .sendTasksTskvLogEntry(_: MessageId, _: Array[Byte]))
        .expects(*, *)
        .returningZ(())
        .repeated(recordsIdBuffer)

      val dropRepeats = recordsIdBuffer / testBatchSize + 1
      (buffer.drop _).expects(*).returningZ(()).repeated(dropRepeats)

      task.resend(testBatchSize, testParallelism).success
    }

    "process all success results in chunk then fails, if broker fails on one send" in {
      val recordsIdBuffer = 3
      mockBuffer(recordsIdBuffer, testBatchSize)

      (brokerService
        .sendTasksTskvLogEntry(_: MessageId, _: Array[Byte]))
        .expects(*, *)
        .returningZ(())

      (brokerService
        .sendTasksTskvLogEntry(_: MessageId, _: Array[Byte]))
        .expects(*, *)
        .throwingZ(new Exception("bla"))

      (buffer.drop _).expects(*).returningZ(()).repeated(1)

      val res = task.resend(testBatchSize, testParallelism).failure.exception
      res shouldBe an[CompositeException]
    }

    "fails if drop fails" in {
      val recordsIdBuffer = 3
      mockBuffer(recordsIdBuffer, testBatchSize)

      (brokerService
        .sendTasksTskvLogEntry(_: MessageId, _: Array[Byte]))
        .expects(*, *)
        .returningZ(())
        .twice()

      (buffer.drop _).expects(*).throwingZ(new Exception("bla"))

      val res = task.resend(testBatchSize, testParallelism).failure.exception
      res shouldBe an[CompositeException]
    }

    def mockBuffer(count: Int, batchSize: Int): Unit = {
      val generator = new TestEventStorage(count)
      (buffer.firstN _)
        .expects(batchSize)
        .returning(generator.get(batchSize))
    }
  }

  "executeAllWithResultsPar" should {
    "return all successes and no errors if no errors exists" in {
      val expected = List(1, 2, 3)
      val result =
        executeAllWithResultsPar(testParallelism)(
          expected.map(v => ZIO.succeed(v))
        ).success.value
      result.successes should contain theSameElementsAs expected
      result.errors shouldBe empty
    }

    "dont fails on effects fail and return right results" in {
      val expectedSuccess = List(1, 3)
      val effects = List(
        ZIO.succeed(1),
        ZIO.fail(new Exception("bla")),
        ZIO.succeed(3)
      )
      val result =
        executeAllWithResultsPar(testParallelism)(effects).success.value
      result.successes should contain theSameElementsAs expectedSuccess
      result.errors.length shouldBe 1
      result.errors.head shouldBe an[Exception]
    }
  }

}

object ResendingServiceImplSpec {

  val testBatchSize = 2
  val testParallelism = 10

  private val testTime = DateTime.parse("2021-08-25")

  private def genRecord(id: Int) = {
    val proto = SalesmanStatisticLogEntry
      .newBuilder()
      .setTimestamp(testTime.toProtobufTimestamp)
      .setOperationTimestamp(testTime.toProtobufTimestamp)
      .build()
      .toByteArray
    LogBufferEntry(
      LogBufferEntryId(id),
      LogBufferEntryPayload(MessageId(s"id_$id"), proto),
      LogBufferEntryType.SalesmanTasksLog,
      testTime
    )
  }

  private def genRecords(count: Int): List[LogBufferEntry] =
    (1 to count).map(genRecord).toList

  class TestEventStorage(maxCount: Int) {
    private var data = genRecords(maxCount)

    def get(count: Int): Task[List[LogBufferEntry]] =
      Task.succeed {
        val (res, rest) = data.splitAt(count)
        data = rest
        res
      }
  }
}
