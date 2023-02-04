package vs.registry.consumer

import bootstrap.tracing.$
import vertis.vasgen.document.{PrimaryKey, RawDocument}
import vs.registry.consumer.Context.{
  ConfiguredRegistryConsumer,
  ConfiguredRegistryProducer,
}
import vs.registry.db.ShardingTool
import vs.registry.document.{DocumentConverter, RegistryDocument}
import vs.registry.domain.*
import vs.registry.producer.RegistryProducer
import vs.registry.sample.RawDocumentSamples
import vs.registry.service.ShardLockerMock
import zio.*
import zio.test.*
import zio.test.Assertion.*

object RegistryConsumerSpec extends ZIOSpecDefault {

  val range1 = 1 to 512
  val range2 = 256 to 768

  val portion1 = portion(range1, ShardId.cast(Set(0, 32)))
    .map(i => generate(i, 2L))

  val portion2 = portion(range1, ShardId.cast(Set(1, 3)))
    .map(i => generate(i, 2L))

  val portion3 = portion(range2, ShardId.cast(Set(1, 3, 32)))
    .map(i => generate(i, 3L))

  override def spec =
    suite("WithLayer")(
      test("subscribe to queue")(
        assertZIO(
          for {
            consumer <- ZIO.service[ConfiguredRegistryConsumer]

            queue <- ZIO.service[Queue[ConsumedDocument]]
            _     <- consumer.subscribe(raw => queue.offer(raw).unit)
          } yield (),
        )(isUnit),
      ),
      test("Assign all shards to producer")(
        assertZIO(
          for {
            producer <- ZIO.service[ConfiguredRegistryProducer]
            locker   <- ZIO.service[ShardLockerMock]
            _        <- producer.assignShards(ShardId.cast((0 until 128).toSet))
            _        <- locker.waitForLock
          } yield (),
        )(isUnit),
      ),
      test("Assign two shards(0, 32) to consumer")(
        for {
          documents <- ZIO.foreach(portion1)(DocumentConverter.convert)
          consumer  <- ZIO.service[ConfiguredRegistryConsumer]
          _         <- consumer.assignShards(ShardId.cast(Set(0, 32)))
          locker    <- ZIO.service[ShardLockerMock]
          _         <- locker.waitForLock
          producer  <- ZIO.service[ConfiguredRegistryProducer]
          log       <- ZIO.service[$]
          _ <-
            ZIO.foreachDiscard((1 to 2).toList)(version =>
              log.subOperation(RegistryProducer.registryMainTxSpan.name)(
                producer.pipelineTx(
                  range1.map(generate(_, version.toLong)).toList,
                  false,
                ),
              ),
            )
          queue  <- ZIO.service[Queue[ConsumedDocument]]
          result <- waitForNUnique(queue, portion1.size, 2L)
          // Необходимо, чтобы дождаться окончания storeIdx()
          _ <- TestClock.adjust(100.millis)
        } yield assertTrue(result == documents),
      ),
      test(
        "Re-assign consumer shards(1, 3, 32) and consume rest(shards(1, 3))",
      )(
        for {
          documents <- ZIO.foreach(portion2)(DocumentConverter.convert)
          consumer  <- ZIO.service[ConfiguredRegistryConsumer]
          _         <- consumer.assignShards(ShardId.cast(Set(1, 32, 3)))
          locker    <- ZIO.service[ShardLockerMock]
          _         <- locker.waitForLock
          queue     <- ZIO.service[Queue[ConsumedDocument]]
          result    <- waitForNUnique(queue, portion2.size, 2L)
        } yield assert(result)(hasSameElements(documents)),
      ),
      test("Store another big portion of documents")(
        for {
          documents <- ZIO.foreach(portion3)(DocumentConverter.convert)
          consumer  <- ZIO.service[ConfiguredRegistryConsumer]
          _         <- consumer.assignShards(ShardId.cast(Set(1, 32, 3)))
          producer  <- ZIO.service[ConfiguredRegistryProducer]
          log       <- ZIO.service[$]
          _ <-
            log.subOperation(RegistryProducer.registryMainTxSpan.name)(
              producer.pipelineTx(range2.map(generate(_, 3L)).toList, false),
            )
          queue  <- ZIO.service[Queue[ConsumedDocument]]
          result <- waitForNUnique(queue, portion3.size, 3L)
        } yield assertTrue(result == documents),
      ),
    ).provideLayerShared(layer) @@ TestAspect.sequential

  def layer = Context.live ++ ZLayer.fromZIO(Queue.unbounded[ConsumedDocument])

  private def waitForNUnique(
    queue: Queue[ConsumedDocument],
    n: Int,
    version: Long,
  ) =
    for {
      ref <- Ref.make(Map.empty[String, RegistryDocument])
      documents <-
        (
          for {
            list <- queue.takeBetween(1, n)
            map <- ref.updateAndGet(map =>
              map ++
                list
                  .filter(_.document.version.value == version)
                  .map(record => record.document.pkAsString -> record.document)
                  .toMap,
            )
          } yield map
        ).repeatWhile(_.size < n)
    } yield documents.values.toList.sortBy(_.pkAsString)

  private def generate(i: Int, version: Long): RawDocument =
    RawDocumentSamples
      .upsert2
      .withPk(PrimaryKey.defaultInstance.withStr(generatePk(i)))
      .withVersion(version)

  def portion(range: Range, shards: Set[ShardId]): List[Int] =
    range
      .map(i => i -> ShardingTool.getShard(generatePk(i)))
      .filter(t => shards.contains(t._2))
      .map(_._1)
      .toList

  private def generatePk(i: Int): String = f"PK-$i%03d"

}
