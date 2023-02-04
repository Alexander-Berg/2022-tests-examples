package vsmoney.auction.services.testkit

import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.{Metric, MetricName, TopicPartition}
import zio.kafka.producer.Producer
import zio.kafka.serde.Serializer
import zio.{Chunk, Has, RIO, Task, UIO, ZIO, ZLayer}

object BidKafkaProducerMock {

  class TestProducer(fail: Boolean) extends Producer {
    private var _records: Chunk[ProducerRecord[_, _]] = Chunk.empty

    def records[K, V]: UIO[Chunk[ProducerRecord[K, V]]] =
      ZIO.effectTotal(_records.asInstanceOf[Chunk[ProducerRecord[K, V]]])

    override def produce[R, K, V](
        record: ProducerRecord[K, V],
        keySerializer: Serializer[R, K],
        valueSerializer: Serializer[R, V]): RIO[R, RecordMetadata] = ???

    override def produce[R, K, V](
        topic: String,
        key: K,
        value: V,
        keySerializer: Serializer[R, K],
        valueSerializer: Serializer[R, V]): RIO[R, RecordMetadata] = ???

    override def produceAsync[R, K, V](
        record: ProducerRecord[K, V],
        keySerializer: Serializer[R, K],
        valueSerializer: Serializer[R, V]): RIO[R, Task[RecordMetadata]] = ???

    override def produceAsync[R, K, V](
        topic: String,
        key: K,
        value: V,
        keySerializer: Serializer[R, K],
        valueSerializer: Serializer[R, V]): RIO[R, Task[RecordMetadata]] = ???

    override def produceChunkAsync[R, K, V](
        records: Chunk[ProducerRecord[K, V]],
        keySerializer: Serializer[R, K],
        valueSerializer: Serializer[R, V]): RIO[R, Task[Chunk[RecordMetadata]]] = ???

    override def produceChunk[R, K, V](
        records: Chunk[ProducerRecord[K, V]],
        keySerializer: Serializer[R, K],
        valueSerializer: Serializer[R, V]): RIO[R, Chunk[RecordMetadata]] = {
      ZIO
        .effectTotal {
          _records = _records ++ records
        }
        .as(Chunk(new RecordMetadata(new TopicPartition("topic", 0), 0, 0, 0, 0, 0))) <*
        ZIO.fail(new Exception("Boom")).when(fail)
    }

    override def flush: Task[Unit] = ???

    override def metrics: Task[Map[MetricName, Metric]] = ???
  }

  def records[K, V]: ZIO[Has[TestProducer], Nothing, Chunk[ProducerRecord[K, V]]] =
    ZIO.serviceWith[TestProducer](_.records[K, V])

  def mock(fail: Boolean = false): ZLayer[Any, Nothing, Has[Producer] with Has[TestProducer]] = {
    val producer = new TestProducer(fail)
    ZLayer.succeedMany(
      Has.allOf(
        producer,
        producer: Producer
      )
    )
  }

}
