package ru.yandex.vos2.autoru.feedprocessor.util

import java.{lang, time, util}
import java.util.regex.Pattern

import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.{Metric, MetricName, PartitionInfo, TopicPartition}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import ru.yandex.vos2.autoru.feedprocessor.util.BatchingSupport.Commit

import scala.concurrent.duration._

class BatchingConsumerSpec extends AnyWordSpec with Matchers with OptionValues {

  "Batching consumer" should {
    "flush after flush period" in {
      BatchingSupport.ConsumerCommitFlushTime = 1.second.toMillis
      val consumer = initConsumer()
      val partition = new TopicPartition("test", 0)

      consumer.commitCandidate(partition, new OffsetAndMetadata(1))
      Thread.sleep(BatchingSupport.ConsumerCommitFlushTime)
      consumer.commitCandidate(partition, new OffsetAndMetadata(2)).get(partition).offset() shouldBe 2

      BatchingSupport.ConsumerCommitFlushTime = 1.minute.toMillis
    }

    "flush after big offset" in {
      val consumer = initConsumer()
      val partition = new TopicPartition("test", 0)
      consumer.commitCandidate(partition, new OffsetAndMetadata(1))
      consumer.flushOffsets()(partition).offset() shouldBe 1

      val bigOffset = BatchingSupport.ConsumerCommitFlushCount + 2

      consumer.commitCandidate(partition, new OffsetAndMetadata(bigOffset - 1)) shouldBe None
      consumer.commitCandidate(partition, new OffsetAndMetadata(bigOffset)).get(partition).offset() shouldBe bigOffset
    }

    "flush if two partitions have big offset in sum" in {
      val consumer = initConsumer()
      val partition1 = new TopicPartition("test", 0)
      val partition2 = new TopicPartition("test", 1)
      consumer.commitCandidate(partition1, new OffsetAndMetadata(0))
      consumer.commitCandidate(partition2, new OffsetAndMetadata(0))
      val commit1 = consumer.flushOffsets()
      commit1(partition1).offset() shouldBe 0
      commit1(partition2).offset() shouldBe 0

      val half = BatchingSupport.ConsumerCommitFlushCount / 2

      consumer.commitCandidate(partition1, new OffsetAndMetadata(half)) shouldBe None
      val commit2 = consumer.commitCandidate(partition2, new OffsetAndMetadata(half + 1)).get

      commit2(partition1).offset() shouldBe half
      commit2(partition2).offset() shouldBe half + 1
    }
  }

  //noinspection ScalaStyle
  def initConsumer(): BatchingSupport[Any, Any] = {

    val consumer = new Consumer[Any, Any] with BatchingSupport[Any, Any] {
      override def assignment(): util.Set[TopicPartition] = ???

      override def subscription(): util.Set[String] = ???

      override def subscribe(collection: util.Collection[String]): Unit = ???

      override def subscribe(collection: util.Collection[String],
                             consumerRebalanceListener: ConsumerRebalanceListener): Unit = ???

      override def assign(collection: util.Collection[TopicPartition]): Unit = ???

      override def subscribe(pattern: Pattern, consumerRebalanceListener: ConsumerRebalanceListener): Unit = ???

      override def subscribe(pattern: Pattern): Unit = ???

      override def unsubscribe(): Unit = ???

      override def poll(l: Long): ConsumerRecords[Any, Any] = ???

      override def poll(duration: time.Duration): ConsumerRecords[Any, Any] = ???

      override def commitSync(): Unit = ???

      override def commitSync(duration: time.Duration): Unit = ()

      override def commitSync(map: util.Map[TopicPartition, OffsetAndMetadata]): Unit = ()

      override def commitSync(map: util.Map[TopicPartition, OffsetAndMetadata], duration: time.Duration): Unit = ???

      override def commitAsync(): Unit = ???

      override def commitAsync(offsetCommitCallback: OffsetCommitCallback): Unit = ???

      override def commitAsync(map: util.Map[TopicPartition, OffsetAndMetadata],
                               offsetCommitCallback: OffsetCommitCallback): Unit = ???

      override def seek(topicPartition: TopicPartition, l: Long): Unit = ???

      override def seek(topicPartition: TopicPartition, offsetAndMetadata: OffsetAndMetadata): Unit = ???

      override def seekToBeginning(collection: util.Collection[TopicPartition]): Unit = ???

      override def seekToEnd(collection: util.Collection[TopicPartition]): Unit = ???

      override def position(topicPartition: TopicPartition): Long = ???

      override def position(topicPartition: TopicPartition, duration: time.Duration): Long = ???

      override def committed(topicPartition: TopicPartition): OffsetAndMetadata = ???

      override def committed(topicPartition: TopicPartition, duration: time.Duration): OffsetAndMetadata = ???

      override def committed(set: util.Set[TopicPartition]): util.Map[TopicPartition, OffsetAndMetadata] = ???

      override def committed(set: util.Set[TopicPartition],
                             duration: time.Duration): util.Map[TopicPartition, OffsetAndMetadata] = ???

      override def metrics(): util.Map[MetricName, _ <: Metric] = ???

      override def partitionsFor(s: String): util.List[PartitionInfo] = ???

      override def partitionsFor(s: String, duration: time.Duration): util.List[PartitionInfo] = ???

      override def listTopics(): util.Map[String, util.List[PartitionInfo]] = ???

      override def listTopics(duration: time.Duration): util.Map[String, util.List[PartitionInfo]] = ???

      override def paused(): util.Set[TopicPartition] = ???

      override def pause(collection: util.Collection[TopicPartition]): Unit = ???

      override def resume(collection: util.Collection[TopicPartition]): Unit = ???

      override def offsetsForTimes(
          map: util.Map[TopicPartition, lang.Long]
      ): util.Map[TopicPartition, OffsetAndTimestamp] = ???

      override def offsetsForTimes(map: util.Map[TopicPartition, lang.Long],
                                   duration: time.Duration): util.Map[TopicPartition, OffsetAndTimestamp] = ???

      override def beginningOffsets(collection: util.Collection[TopicPartition]): util.Map[TopicPartition, lang.Long] =
        ???

      override def beginningOffsets(collection: util.Collection[TopicPartition],
                                    duration: time.Duration): util.Map[TopicPartition, lang.Long] = ???

      override def endOffsets(collection: util.Collection[TopicPartition]): util.Map[TopicPartition, lang.Long] = ???

      override def endOffsets(collection: util.Collection[TopicPartition],
                              duration: time.Duration): util.Map[TopicPartition, lang.Long] = ???

      override def close(): Unit = ???

      override def close(l: Long, timeUnit: TimeUnit): Unit = ???

      override def close(duration: time.Duration): Unit = ???

      override def wakeup(): Unit = ???
    }

    consumer
  }
}
