package vertis.logbroker.client.test.unit

import common.zio.logging.Logging

import java.util.Collections
import org.scalacheck.Gen
import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.{MessageData, MessageMeta}
import vertis.logbroker.client.{BLbTask, LbTask}
import vertis.logbroker.client.consumer.model._
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession.ConsumerCb
import ru.yandex.vertis.util.collection._
import vertis.core.model.{DataCenter, DataCenters}
import vertis.logbroker.client.consumer.model.LbTopicPartition
import vertis.logbroker.client.consumer.model.in.{LbMessageData, ReadResult}
import vertis.logbroker.client.model.LbSuccess
import zio.{Queue, RIO, Ref, UIO}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
object ConsumerUnitTest {

  case class TestSourceConf(
      dc: DataCenter = DataCenters.Sas,
      topic: String = "/test-topic",
      partitions: Set[Int] = Set(0, 1, 2))

  object TestSourceConf {
    val default = TestSourceConf()
  }

  case class TestConsumer(make: ConsumerCb => BLbTask[LbNativeConsumerSession], control: TestConsumerControl)

  case class TestConsumerControl(
      reply: ConsumerCb, // prepare a reply for an active consumer
      generation: UIO[Int], // consumer's generation, starts from 1
      lbSuccess: UIO[LbSuccess[ReadResult]], // generate a success reply msg
      activeConsumers: UIO[Int], // must be <= 1
      committed: UIO[Seq[Cookie]])

  def create(
      topic: TestSourceConf = TestSourceConf.default,
      onInit: Option[Map[Int, Long]] => LbTask[Unit] = _ => UIO.unit,
      onClose: UIO[Unit] = UIO.unit): RIO[zio.ZEnv, TestConsumer] = {
    for {
      genCounter <- Ref.make(0)
      serverOffsets <- Ref.make(ServerOffsets.initial(topic))
      activeConsumers <- Ref.make(0)
      committedCookies <- Ref.make(Seq.empty[Cookie])
      q <- Queue.unbounded[LogbrokerReadResult]
    } yield {
      val reply: ConsumerCb = res =>
        Logging.info(s"Replying ${describe(res)} from ${topic.dc}") *>
          q.offer(res).unit
      val createConsumer: ConsumerCb => BLbTask[LbNativeConsumerSession] = { cb: ConsumerCb =>
        genCounter
          .update(_ + 1)
          .as(
            new TestNativeConsumer(
              cb,
              q,
              offsets =>
                activeConsumers.update(_ + 1) *>
                  Logging.info(s"Initializing test consumer in ${topic.dc}") *>
                  onInit(offsets) *>
                  Logging.info(s"Initialized test consumer in ${topic.dc}"),
              cookies =>
                Logging.info(s"Committing $cookies to ${topic.dc}") *>
                  committedCookies.update(_ ++ cookies),
              activeConsumers.update(_ - 1) *>
                Logging.info(s"Closing test consumer in ${topic.dc}") *>
                onClose *>
                Logging.info(s"Closed test consumer in ${topic.dc}")
            )
          )
      }
      val lbSuccess = serverOffsets.modify { o =>
        val (newO, msgs) = genMessages(topic, o.lastOffsets)
        val newServerOffsets = o.add(newO)
        LbSuccess(ReadResult(msgs, newServerOffsets.lastCookie)) -> newServerOffsets
      }
      TestConsumer(
        createConsumer,
        TestConsumerControl(reply, genCounter.get, lbSuccess, activeConsumers.get, committedCookies.get)
      )
    }
  }

  private def describe(res: LogbrokerReadResult): String =
    res match {
      case LbSuccess(rr) => rr.cookie.toString
      case other => other.getClass.getSimpleName
    }

  private val noMeta =
    new MessageMeta(Array.empty[Byte], 0L, 0L, 0L, "127.0.0.1", CompressionCodec.RAW, Collections.emptyMap())

  private def genMessages(
      conf: TestSourceConf,
      offsets: Map[Partition, Offset]): (Map[Partition, Offset], Map[LbTopicPartition, Seq[LbMessageData]]) = {
    val withOffsets = noOffsetMessageGen(conf).sample.get.map { case (tp, msgs) =>
      val baseOffset = offsets(tp.partition) + 1
      tp -> msgs.zipWithIndex.map { case (m, i) =>
        new LbMessageData(m.getRawData, baseOffset + i, m.getMessageMeta.getCodec)
      }
    }
    val newOffsets = offsets.map { case (p, o) =>
      p -> withOffsets.find(_._1.partition == p).map(_._2.last.offset).getOrElse(o)
    }
    newOffsets -> withOffsets
  }

  private val MsgGen: Gen[MessageData] = for {
    bytes <- Gen.nonEmptyListOf(Gen.alphaChar.map(_.toByte)).map(_.toArray)
  } yield new MessageData(bytes, 0L, noMeta)

  private def noOffsetMessageGen(conf: TestSourceConf): Gen[Map[LbTopicPartition, Seq[MessageData]]] = {
    val partitions = Gen.oneOf(conf.partitions).map(p => LbTopicPartition(conf.topic, conf.dc, p))
    val messages = Gen.nonEmptyListOf(MsgGen)
    Gen.resize(conf.partitions.size, Gen.nonEmptyMap(Gen.zip(partitions, messages)))
  }

  case class ServerOffsets(
      lastOffsets: Map[Partition, Offset],
      lastCookie: Cookie,
      cookies: Seq[(Cookie, Map[Partition, Offset])]) {

    def add(offsets: Map[Partition, Offset]): ServerOffsets = {
      val cookie = lastCookie + 1
      ServerOffsets(lastOffsets.merge(offsets)(Math.max), cookie, cookies :+ (cookie -> offsets))
    }
  }

  object ServerOffsets {

    def initial(topic: TestSourceConf): ServerOffsets =
      ServerOffsets(topic.partitions.map(p => p -> -1L).toMap, 0L, Seq.empty)
  }
}
