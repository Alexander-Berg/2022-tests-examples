package vertis.pipeline.convert

import com.google.protobuf.{Descriptors, Message}
import common.zio.clients.clickhouse.jdbc.ClickhouseJdbcClient.ClickhouseJdbcClient
import common.zio.logging.Logging
import ru.yandex.vertis.util.collection._
import vertis.broker.pipeline.ch.batching.TestBatching
import vertis.broker.pipeline.ch.sink.converter._
import vertis.broker.pipeline.ch.sink.model.ChProtoSchema
import vertis.broker.pipeline.ch.sink.queries.{ClickhouseQueries, QueriesSupport}
import vertis.broker.pipeline.ch.testkit.ChTestDescriptors
import vertis.stream.model.{OffsetRange, TopicPartition}
import vertis.zio.BaseEnv
import zio._
import zio.test.Assertion.equalTo
import zio.test._

/**
 * @author kusaeva, reimai
  */
object ChWriterIntSpec
  extends ClickhouseTestSpec
  with TestBatching
  with QueriesSupport
  with ChProtoCompareSupport
  with ChTestDescriptors {

  private val tp = TopicPartition(topic = "myt//test/topic", partition = 0)

  override protected def chSpec: ZSpec[BaseEnv with ClickhouseJdbcClient, Any] =
    suite("ChWriter")(
      (Seq(
        createAndWrite,
        createAndWriteComplex,
        writeOffsets,
        notWriteEmptyOffsets,
        aggregateOffsets,
        writeBatchedOfMixedVersions
      ) ++ writeDifferentMessages): _*
    )

  private val createAndWrite =
    testM("create table and write simple message correctly") {
      chTestM(SimpleMessage.getDescriptor, "smoke_test").use { chTest =>
        import chTest._

        for {
          table <- UIO(getTable).tap(schemaEvolution.update)
          exists <- assertM(client.executeQuery(ClickhouseQueries.checkTableExists(sinkConf.id)))(equalTo(true))

          messages = genMessages(descriptor, 1)
          pmessages = getPMessages(messages, samplePartition(), 0L)
          batch <- createBatchFromPMessages(table.schema, pmessages)
          _ <- writer.write(batch)
          message = messages.head
          pmessage = pmessages.head
          id = pmessage.envelope.id
          tableId = table.id
          _ <- Logging.info("Batch written")
          gotId <- testExpectedField[String](client, tableId, ChProtoSchema.IdColumn, id, id)
          gotOffset <- testExpectedField[Long](
            client,
            tableId,
            ChProtoSchema.OffsetColumn,
            id,
            pmessage.sourceMeta.offset
          )
          gotTp <- testExpectedField[TopicPartition](
            client,
            tableId,
            ChProtoSchema.PartitionColumn,
            id,
            pmessage.sourceMeta.tp
          )
          gotInt <- testProtoField[Int](client, tableId, "int32", id, message)
          gotUInt <- testProtoField[Int](client, tableId, "uint32", id, message)
          gotStr <- testProtoField[String](client, tableId, "str", id, message)
          gotBool <- testProtoField[Int](
            client,
            tableId,
            "bool",
            id,
            message,
            x => (if (x.asInstanceOf[Boolean]) 1 else 0).asInstanceOf[AnyRef]
          )
          gotTs <- testTsField(client, tableId, table.schema.timestampColumnName, id, message)
        } yield exists && gotId && gotOffset && gotTp && gotInt && gotUInt && gotStr && gotBool && gotTs
      }
    }

  private val createAndWriteComplex =
    testM("create table and write complex message correctly") {
      chTestM(ComplexMessage.getDescriptor, "complex").use { chTest =>
        import chTest._

        for {
          table <- UIO(getTable).tap(schemaEvolution.update)
          exists <- client.executeQuery(ClickhouseQueries.checkTableExists(sinkConf.id))
          tableCreated = zio.test.assertTrue(exists)

          messages = genMessages(descriptor, 1)
          pmessages = getPMessages(messages, samplePartition(), 0L)
          batch <- createBatchFromPMessages(table.schema, pmessages)
          _ <- writer.write(batch)
          _ <- Logging.info("Batch written")

          message = messages.head
          pmessage = pmessages.head
          id = pmessage.envelope.id
          tableId = table.id

          gotId <- testExpectedField[String](client, tableId, ChProtoSchema.IdColumn, id, id)
          gotOffset <- testExpectedField[Long](
            client,
            tableId,
            ChProtoSchema.OffsetColumn,
            id,
            pmessage.sourceMeta.offset
          )
          gotTp <-
            testExpectedField[TopicPartition](
              client,
              tableId,
              ChProtoSchema.PartitionColumn,
              id,
              pmessage.sourceMeta.tp
            )
          gotLong <- testProtoField[Long](
            client,
            tableId,
            "uint64",
            id,
            message,
            x => unwrapPrimitiveWrapper(x.asInstanceOf[Message]).asInstanceOf[AnyRef]
          )
          gotString <- testProtoField[String](
            client,
            tableId,
            "simple",
            id,
            message,
            x => ProtoClickhouseConverterImpl.jsonPrinter.print(x.asInstanceOf[Message]).asInstanceOf[AnyRef]
          )
          gotEnum <- testProtoField[String](
            client,
            tableId,
            "enum",
            id,
            message,
            x => x.asInstanceOf[Descriptors.EnumValueDescriptor].getName.asInstanceOf[AnyRef]
          )
          gotTs <- testTsField(
            client,
            tableId,
            table.schema.timestampColumnName,
            id,
            message
          )
          _ <- Logging.info(s"Written to table $tableId")
        } yield tableCreated && gotId && gotOffset && gotTp && gotLong && gotString && gotEnum && gotTs
      }
    }

  /** just checking different messages writing for no errors */
  private val writeDifferentMessages = descriptors.map { descr =>
    testM(s"write ${descr.getName}") {
      chTestM(descr).use { test =>
        import test._
        for {
          table <- UIO(getTable).tap(schemaEvolution.update)
          tableCreated <- assertM(client.executeQuery(ClickhouseQueries.checkTableExists(table.id)))(equalTo(true))
          actualSchema <- client.executeQuery(ClickhouseQueries.describeTable(table.id))
          rightSchema = assert(actualSchema)(equalTo(table.schema.columns))
          batch <- createBatch(table.schema, descriptor, tp, 1)
          _ <- writer.write(batch)
          _ <- Logging.info("Batch written")
        } yield tableCreated && rightSchema
      }
    }
  }

  private val writeOffsets = testM("write offsets") {
    chTestM(TestMessage.getDescriptor, "write_offsets").use { test =>
      import test._
      val tp2 = tp.copy(partition = 1)
      for {
        _ <- UIO(getTable).tap(schemaEvolution.update)
        offsets = Map(tp -> OffsetRange(tp, 0L, 100500L), tp2 -> OffsetRange(tp2, 100L, 300L))
        _ <- writer.writeOffsets(offsets)
        offsetsOk <- test.checkOffsets(offsets.mapValuesStrict(_.to))
      } yield offsetsOk
    }
  }

  private val notWriteEmptyOffsets = testM("not write empty offsets") {
    chTestM(TestMessage.getDescriptor, "empty_offsets").use { test =>
      import test._
      val tp2 = tp.copy(partition = 1)
      for {
        _ <- UIO(getTable).tap(schemaEvolution.update)
        offsets = Map(tp -> OffsetRange(tp, 0L, 100500L), tp2 -> OffsetRange(tp2, 100L, 300L))
        _ <- writer.writeOffsets(offsets)
        offsetsWritten <- test.checkOffsets(offsets.mapValuesStrict(_.to))
        emptyOffsets = Map.empty[TopicPartition, OffsetRange]
        _ <- writer.writeOffsets(emptyOffsets)
        offsetsDidNotChange <- test.checkOffsets(offsets.mapValuesStrict(_.to))
      } yield offsetsWritten && offsetsDidNotChange
    }
  }

  private val aggregateOffsets = testM("aggregate offsets as max") {
    chTestM(TestMessage.getDescriptor, "agg_offsets").use { test =>
      import test._
      val tp2 = tp.copy(partition = 1)
      for {
        _ <- UIO(getTable).tap(schemaEvolution.update)
        offsetsOne = Map(tp -> OffsetRange(tp, 0L, 10L), tp2 -> OffsetRange(tp2, 5L, 15L))
        offsetsTwo = Map(tp -> OffsetRange(tp, 10L, 20L), tp2 -> OffsetRange(tp2, 0L, 5L))
        _ <- ZIO.foreachPar_(Seq(offsetsOne, offsetsTwo))(writer.writeOffsets)
        offsetsExpected = Map(tp -> 20L, tp2 -> 15L)
        offsetsMerged <- test.checkOffsets(offsetsExpected)
        offsetsThree = Map(tp -> OffsetRange(tp, 0L, 0L), tp2 -> OffsetRange(tp2, 0L, 0L))
        _ <- writer.writeOffsets(offsetsThree)
        offsetsNotDowngraded <- test.checkOffsets(offsetsExpected)
      } yield offsetsMerged && offsetsNotDowngraded
    }
  }

  private val writeBatchedOfMixedVersions = testM("write batches of mixed versions") {
    chTestM(WithTimestampAndRepeated.getDescriptor, "mixed_versions").use { test =>
      import test._
      val v1 = "v0.0.1"
      val v2 = "v0.0.2"
      for {
        _ <- Logging.info("start")
        table <- UIO(getTable).tap(schemaEvolution.update)
        messages1 = genMessages(WithTimestampAndRepeated.getDescriptor, 1)
        pmessages1 = getPMessages(messages1, samplePartition(), 0L, Some(v2))
        // add message with missing fields
        messages2 = genMessages(WithTimestamp.getDescriptor, 1)
        pmessages2 = getPMessages(messages2, samplePartition(), 1L, Some(v1))
        batch <- createBatchFromPMessages(table.schema, pmessages1 ++ pmessages2)
        _ <- Logging.info(s"batch to write: $batch")
        _ <- writer.write(batch)
        _ <- Logging.info("Batch of two schemas written")
      } yield assertTrue(true)
    }
  }
}
