package vertis.broker.pipeline.ch.evolution

import common.zio.clients.clickhouse.jdbc.ClickhouseJdbcClient.ClickhouseJdbcClient
import vertis.broker.pipeline.ch.batching.TestBatching
import vertis.broker.pipeline.ch.sink.converter.ProtoClickhouseConverterImpl.toSchema
import vertis.broker.pipeline.ch.sink.converter.{WithBytes, WithEnum, WithTimestamp}
import vertis.broker.pipeline.ch.sink.model.ChTable
import vertis.broker.pipeline.ch.sink.queries.ClickhouseQueries
import vertis.broker.pipeline.ch.testkit.ChTestDescriptors
import vertis.pipeline.convert.ClickhouseTestSpec
import vertis.zio.BaseEnv
import zio.test.Assertion._
import zio.test._
import zio.{Ref, UIO}

/** @author kusaeva
  */
object ChEvolutionIntSpec extends ClickhouseTestSpec with ChTestDescriptors with TestBatching {

  override protected def chSpec: ZSpec[BaseEnv with ClickhouseJdbcClient, Any] =
    suite("ChSchemaEvolution")(
      (Seq(
        createOffsetViewForExistingTable,
        correctOffsetView,
        getSchemaFromDb,
        addColumns
      ) ++ evolveDifferentMessages): _*
    )

  private val evolveDifferentMessages = descriptors.map { d =>
    testM(s"create offsets view for ${d.getName}") {
      chTestM(d).use { test =>
        import test._
        for {
          schema <- UIO(getTable).tap(schemaEvolution.update).map(_.schema)
          n <- zio.random.nextIntBounded(20)
          partition <- zio.random.nextIntBounded(10).map(samplePartition)
          correctInitialOffsets <- checkOffsets(Map(partition -> -1L))
          _ <- createBatch(schema, descriptor, partition, n).tap(writer.write)
          offsetAfterN <- checkOffsets(Map(partition -> (n - 1L)))
          _ <- createBatch(schema, descriptor, partition, n, n.toLong).tap(writer.write)
          offsetsAfter2N <- checkOffsets(Map(partition -> (n * 2 - 1L)))
        } yield correctInitialOffsets && offsetAfterN && offsetsAfter2N
      }
    }
  }

  private val createOffsetViewForExistingTable =
    testM("create an offset view for an existing table") {
      chTestM(WithTimestamp.getDescriptor, "offset_view").use { test =>
        import test._
        val table = getTable
        for {
          _ <- client.executeQuery(ClickhouseQueries.touchDb(table.id.db))
          _ <- client.executeQuery(ClickhouseQueries.touchTable(table))
          noOffsetTable <- assertM(
            client
              .executeQuery(ClickhouseQueries.checkTableExists(table.offsetView))
          )(equalTo(false))
          _ <- schemaEvolution.touch(table)
          offsetTableCreated <- assertM(
            client
              .executeQuery(ClickhouseQueries.checkTableExists(table.offsetView))
          )(equalTo(true))
        } yield noOffsetTable && offsetTableCreated
      }
    }

  private val correctOffsetView =
    testM(s"create a correct offset view") {
      chTestM(WithEnum.getDescriptor, "correct_offset_view").use { test =>
        import test._
        val table = getTable
        for {
          _ <- schemaEvolution.update(table)
          offsetStore <- Ref.make(0)
          p = samplePartition()
          reps <- zio.random.nextIntBounded(100)
          _ <- writeRandom(p, offsetStore).repeatN(reps)
          expectedLastOffset <- offsetStore.get.map(_ - 1L)
          correctOffsets <- checkOffsets(Map(p -> expectedLastOffset))
        } yield correctOffsets
      }
    }

  private val getSchemaFromDb = testM("get schema from db") {
    chTestM(WithTimestamp.getDescriptor, "get_schema").use { test =>
      import test._
      val table = getTable.copy(expireInDays = Some(42))
      schemaEvolution.update(table) *>
        schemaEvolution.getSchemaFromDb(table.id).map { tableFromDb =>
          assert(tableFromDb.expireInDays)(equalTo(table.expireInDays)) &&
          assert(tableFromDb.columns)(equalTo(table.schema.columns))
        }
    }
  }

  private val addColumns = testM("add columns") {
    chTestM(WithTimestamp.getDescriptor, "add_columns").use { test =>
      import test._
      for {
        _ <- schemaEvolution.update(getTable)
        correctInitialSpec <- checkColumns("timestamp")
        tableWithBytes = ChTable.build(sinkConf, toSchema(WithBytes.getDescriptor, "timestamp", Seq("timestamp", "b")))
        _ <- schemaEvolution.update(tableWithBytes)
        addedB <- checkColumns("timestamp", "b")
        tableWithEnum = ChTable.build(
          sinkConf,
          toSchema(WithEnum.getDescriptor, "timestamp", Seq("timestamp", "num", "enum"))
        )
        _ <- schemaEvolution.update(tableWithEnum)
        addedNumEnum <- checkColumns("timestamp", "b", "num", "enum")
      } yield correctInitialSpec && addedB && addedNumEnum
    }
  }
}
