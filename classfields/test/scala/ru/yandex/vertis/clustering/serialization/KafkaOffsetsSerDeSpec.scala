package ru.yandex.vertis.clustering.serialization

import org.apache.kafka.common.TopicPartition
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.model.KafkaOffsets
import ru.yandex.vertis.clustering.serialization.KafkaOffsetsSerDeSpec._

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class KafkaOffsetsSerDeSpec extends BaseSpec {

  "KafkaOffsetsSerDe" should {
    "convert KafkaOffsets to and from String" in {
      val offsets = genOffsets
      val str = KafkaOffsetsSerDe.serialize(offsets)
      val parsed = KafkaOffsetsSerDe.deserialize(str).get
      parsed shouldBe offsets
    }
  }

}

object KafkaOffsetsSerDeSpec {
  private val oneOffsetGen: Gen[(TopicPartition, Long)] = for {
    topic <- Gen.nonEmptyListOf[Char](Gen.alphaNumChar).map(_.mkString)
    partition <- Gen.chooseNum(0, 1000)
    offset <- Gen.chooseNum(0, 1000000).map(_.toLong)
  } yield new TopicPartition(topic, partition) -> offset

  private val offsetsGen: Gen[KafkaOffsets] = for {
    n <- Gen.chooseNum(1, 10)
    offsets <- Gen.listOfN(n, oneOffsetGen)
  } yield offsets.toMap

  private def genOffsets: KafkaOffsets =
    Iterator.continually(offsetsGen.sample).flatten.take(1).toIterable.head
}