package vs.registry.producer

import bootstrap.logging.log
import bootstrap.test.BootstrapSpec
import bootstrap.tracing.$
import bootstrap.ydb.{TxError, YDB}
import strict.{Bytes, Uint64}
import vertis.vasgen.document.{PrimaryKey, RawDocument}
import vs.core.events.Publisher
import vs.core.rawdocument.{
  DocumentIsExpired,
  DocumentIssue,
  EmptyFieldValue,
  RawDocumentOps,
}
import vs.registry.db.{QueueStorage, ShardingTool}
import vs.registry.document.DocumentConverter
import vs.registry.domain.*
import vs.registry.sample.RawDocumentSamples
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.time.Instant

object RegistryProducerSpec extends BootstrapSpec[Context.Type] {

  val emptySubscription: Publisher.Subscription = () => ZIO.unit

  type ConfiguredRegistryProducer = RegistryProducerImpl[TEST]
  type ConfiguredQueueStorage     = QueueStorage[TEST]
  type ConfiguredYDB              = YDB[TEST]

  val shard1: ShardId = ShardingTool
    .getShard(RawDocumentSamples.upsert1.pkAsString)

  val shard2: ShardId = ShardingTool
    .getShard(RawDocumentSamples.upsert2.pkAsString)

  val suites: Spec[
    $ & ConfiguredYDB & ConfiguredRegistryProducer & ConfiguredQueueStorage,
    Throwable,
  ] =
    suite("RegistryProducer")(
      test("Assign all shards to producer")(
        for {
          producer <- ZIO.service[ConfiguredRegistryProducer]
          _        <- producer.assignShards(ShardId.cast((0 until 128).toSet))
        } yield assertTrue(true),
      ),
      test("Don't store document with issues")(
        for {
          pipeline <- ZIO.service[ConfiguredRegistryProducer]
          result <- runPipeline(tx =>
            pipeline.pipeline(
              tx,
              List(RawDocumentSamples.upsert1),
              failOnNonCritical = true,
            ),
          )
        } yield assertTrue(
          result ==
            List(
              RegistryProducer.reject(
                RawDocumentSamples.upsert1,
                Seq(DocumentIssue("empty", EmptyFieldValue)),
              ),
            ),
        ),
      ),
      test("Validate and store document")(
        assertZIO(
          (
            for {
              pipeline <- ZIO.service[ConfiguredRegistryProducer]
              result <- runPipeline(tx =>
                pipeline.pipeline(
                  tx,
                  List(RawDocumentSamples.upsert1),
                  failOnNonCritical = false,
                ),
              )
            } yield result
          ).exit,
        )(
          succeeds(
            isSubtype[List[RegistryProducer.Result]](
              hasSameElements(
                Seq(
                  RegistryProducer.accept(
                    RawDocumentSamples.upsert1,
                    Seq(DocumentIssue("empty", EmptyFieldValue)),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
      test("Attempt to store same document")(
        assertZIO(
          (
            for {

              pipeline <- ZIO.service[ConfiguredRegistryProducer]
              result <- runPipeline(tx =>
                pipeline.pipeline(
                  tx,
                  List(RawDocumentSamples.upsert1),
                  failOnNonCritical = false,
                ),
              )
            } yield result
          ).exit,
        )(
          succeeds(
            isSubtype[List[RegistryProducer.Result]](
              equalTo(
                List(
                  RegistryProducer.reject(
                    RawDocumentSamples.upsert1,
                    Seq(
                      DocumentIssue(
                        RawDocumentSamples.upsert1,
                        DocumentIsExpired(
                          RawDocumentSamples.upsert1.epoch.toLong,
                          RawDocumentSamples.upsert1.version,
                          RawDocumentSamples
                            .upsert1
                            .modifiedAt
                            .map(ts =>
                              Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong),
                            )
                            .get,
                        ),
                      ),
                      DocumentIssue("empty", EmptyFieldValue),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
      test("Store same document with increased version")(
        assertZIO(
          (
            for {
              pipeline <- ZIO.service[ConfiguredRegistryProducer]
              result <- runPipeline(tx =>
                pipeline.pipeline(
                  tx,
                  List(RawDocumentSamples.upsert1.copy(version = 85L)),
                  failOnNonCritical = false,
                ),
              )
            } yield result
          ).exit,
        )(
          succeeds(
            isSubtype[List[RegistryProducer.Result]](
              equalTo(
                List(
                  RegistryProducer.accept(
                    RawDocumentSamples.upsert1.copy(version = 85L),
                    Seq(DocumentIssue("empty", EmptyFieldValue)),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
      test("Validate and store another document")(
        assertZIO(
          (
            for {
              producer <- ZIO.service[ConfiguredRegistryProducer]
              result <- runPipeline(tx =>
                producer.pipeline(
                  tx,
                  List(RawDocumentSamples.upsert2),
                  failOnNonCritical = false,
                ),
              )
            } yield result
          ).exit,
        )(
          succeeds(
            isSubtype[List[RegistryProducer.Result]](
              equalTo(
                List(
                  RegistryProducer.accept(RawDocumentSamples.upsert2, Seq.empty),
                ),
              ),
            ),
          ),
        ),
      ),
      test("Select all events") {
        (
          for {
            storage <- ZIO.service[ConfiguredQueueStorage]

            (record, _) <-
              storage.consumePortion(QueueId.main)(
                Map(shard1 -> Uint64(0), shard2 -> Uint64(0)),
                10,
              )
            dd1 <- DocumentConverter
              .convert(RawDocumentSamples.upsert1.copy(version = 85L))
            dd2 <- DocumentConverter.convert(RawDocumentSamples.upsert2)
          } yield assertTrue(
            record ==
              Chunk(
                QueueDocument(
                  shard1,
                  Uint64(4),
                  RawDocumentSamples.upsert1.pkAsString,
                  1L,
                  85L,
                  DocumentData(
                    status = Status.Exists,
                    Bytes(dd1.toByteString.toByteArray),
                  ),
                ),
                QueueDocument(
                  shard2,
                  Uint64(1),
                  RawDocumentSamples.upsert2.pkAsString,
                  1L,
                  1L,
                  DocumentData(status = Status.Exists, Bytes(dd2.toByteArray)),
                ),
              ),
          )
        )
      },
      test("Store many documents")(
        assertZIO(
          (
            for {
              producer <- ZIO.service[ConfiguredRegistryProducer]
              log      <- ZIO.service[$]
              result <-
                ZIO.foreach((1 to 2).toList)(version =>
                  log.subOperation(RegistryProducer.registryMainTxSpan.name)(
                    producer.pipelineTx(
                      (1 to 512).map(generate(_, version.toLong)).toList,
                      failOnNonCritical = false,
                    ),
                  ),
                )
            } yield result
          ).exit,
        )(
          succeeds(
            isSubtype[List[List[RegistryProducer.Result]]](
              equalTo(
                (1 to 2)
                  .toList
                  .map(version =>
                    (1 to 512)
                      .toList
                      .map(i =>
                        RegistryProducer
                          .accept(generate(i, version.toLong), Seq.empty),
                      ),
                  ),
              ),
            ),
          ),
        ),
      ),
      test("Select small subset of events") {
        for {
          storage <- ZIO.service[ConfiguredQueueStorage]
          (record, idx) <-
            storage.consumePortion(QueueId.main)(
              Map(
                ShardId(0)  -> Uint64(0),
                ShardId(32) -> Uint64(0),
                ShardId(64) -> Uint64(0),
              ),
              13,
            )
          chunk <-
            ZIO.foreach(
              Chunk(
                (13, 2L, Uint64(12)),
                (57, 2L, Uint64(13)),
                (24, 2L, Uint64(12)),
                (68, 2L, Uint64(13)),
                (35, 2L, Uint64(12)),
                (79, 2L, Uint64(13)),
              ),
            ) { case (i, version, idx) =>
              queueDocument(i, version, idx)
            }
        } yield assertTrue(record == chunk) &&
          assertTrue(
            idx ==
              Map(
                ShardId(0)  -> Uint64(13),
                ShardId(32) -> Uint64(13),
                ShardId(64) -> Uint64(13),
              ),
          )
      },
      test("Select another subset of events") {
        for {
          storage <- ZIO.service[ConfiguredQueueStorage]
          (records, idx) <-
            storage.consumePortion(QueueId.main)(
              Map(
                ShardId(0)  -> Uint64(19),
                ShardId(1)  -> Uint64(9),
                ShardId(32) -> Uint64(19),
                ShardId(64) -> Uint64(16),
                ShardId(65) -> Uint64(0),
              ),
              4,
            )
          chunk <-
            ZIO.foreach(
              Chunk(
                (376, 2L, Uint64(20)),
                (453, 2L, Uint64(21)),
                (497, 2L, Uint64(22)),
                (14, 2L, Uint64(12)),
                (58, 2L, Uint64(13)),
                (387, 2L, Uint64(20)),
                (420, 2L, Uint64(21)),
                (464, 2L, Uint64(22)),
                (277, 2L, Uint64(17)),
                (310, 2L, Uint64(18)),
                (354, 2L, Uint64(19)),
                (398, 2L, Uint64(20)),
              ),
            ) { case (i, version, idx) =>
              queueDocument(i, version, idx)
            }
        } yield assertTrue(records == chunk) &&
          assertTrue(
            idx ==
              Map(
                ShardId(0)  -> Uint64(22),
                ShardId(1)  -> Uint64(13),
                ShardId(32) -> Uint64(22),
                ShardId(64) -> Uint64(20),
                ShardId(65) -> Uint64(4),
              ),
          )
      },
      test("Select all events from specific shard") {
        for {
          storage <- ZIO.service[ConfiguredQueueStorage]
          (record, idx) <-
            storage
              .consumePortion(QueueId.main)(Map(ShardId(0) -> Uint64(0)), 100)

          chunk <-
            ZIO.foreach(
              Chunk(
                (13, 2L, Uint64(12)),
                (57, 2L, Uint64(13)),
                (134, 2L, Uint64(14)),
                (178, 2L, Uint64(15)),
                (211, 2L, Uint64(16)),
                (255, 2L, Uint64(17)),
                (299, 2L, Uint64(18)),
                (332, 2L, Uint64(19)),
                (376, 2L, Uint64(20)),
                (453, 2L, Uint64(21)),
                (497, 2L, Uint64(22)),
              ),
            ) { case (i, version, idx) =>
              queueDocument(i, version, idx)
            }
        } yield assertTrue(record == chunk) &&
          assertTrue(idx == Map(ShardId(0) -> Uint64(22)))
      },
    )

  override def spec = suites @@ TestAspect.sequential // @@ TestAspect.ignore

  private def distributionExpectations =
    DistributionMock
      .empty // .Subscribe(anything, value(emptySubscription)).optional

  private def generatePk(i: Int): String = f"PK-$i%03d"

  private def generate(i: Int, version: Long): RawDocument =
    RawDocumentSamples
      .upsert2
      .withPk(PrimaryKey.defaultInstance.withStr(generatePk(i)))
      .withVersion(version)

  private def queueDocument(
    i: Int,
    version: Long,
    idx: Uint64,
  ): Task[QueueDocument] = {
    val raw = generate(i, version)
    for {
      document <- DocumentConverter.convert(raw)

    } yield QueueDocument(
      shard = ShardId(document.shardId.value),
      idx = idx,
      pk = document.pkAsString,
      epoch = document.epoch.toLong,
      version = document.version,
      document = DocumentData(
        status = Status.Exists,
        Bytes(document.toByteArray),
      ),
    )
  }

  def runPipeline[T](
    z: YDB.Tx => ZIO[$, Throwable, T],
  ): ZIO[$ & ConfiguredYDB, Throwable, T] =
    for {
      ydb <- ZIO.service[ConfiguredYDB]
      result <- ydb.withTx(tx =>
        log
          .subOperation(RegistryProducer.registryPipelineSpan.name)(z(tx))
          .mapError(TxError.abort),
      )
    } yield result

  override def RLive = (distributionExpectations >>> Context.layer).orDie
}
