package vertis.logbroker.client.ng

import org.scalatest.Ignore
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import vertis.logbroker.client.producer.model.Message
import vertis.zio.BaseEnv
import vertis.logbroker.client.test.{LbRead, LbTest, LbWrite, ReadWriteConfig}
import zio._
import zio.duration._

/** A smoke test, simple write and read
  */
class LbReadWriteIntSpec extends LbWrite with LbRead with LbTest {

  private val configs: Seq[ReadWriteConfig] = {
    for {
      blinkOnWrite <- Seq(None /*, Some(700.millis)*/ ) // write fail in-flight messages in case of error
      blinkOnRead <- Seq(None, Some(700.millis))
    } yield {
      ReadWriteConfig
        .createRandom()
        .copy(
          networkBlinkOnWrite = blinkOnWrite,
          networkBlinkOnRead = blinkOnRead
        )
    }
  }

  override protected val testTopics: Set[String] =
    Iterator.from(0).take(configs.size).map(i => s"$topicNameFromClassName-$i").toSet

  private val testCases: TableFor2[ReadWriteConfig, String] =
    Table("config" -> "topic", configs.zip(testTopics): _*)

  forAll(testCases) { case (config, topic) =>
    import config._
    s"producer-consumer" should {
      s"write and read [$config]" in ioTest {
        for {
          msgsWritten <- doWrite(config, topic)
          _ <- check(s"write:")(msgsWritten shouldBe totalMsgs)
          msgsRead <- doRead(config, topic)
          _ <- check(s"read:")(msgsRead shouldBe totalMsgs)
        } yield ()
      }
    }
  }

  private def doWrite(config: ReadWriteConfig, topic: String) =
    write(topic, config.groups.map(group => group -> groupMessages(config, group)).toMap, config.networkBlinkOnWrite)

  private def groupMessages(config: ReadWriteConfig, group: Int): Seq[Message] =
    Iterator
      .from(1)
      .map(i => Message.raw(i.toLong, s"some data $i for group $group".getBytes))
      .take(config.msgsPerPartition)
      .toSeq

  private def doRead(config: ReadWriteConfig, topic: String): ZIO[BaseEnv, Throwable, Long] = {
    import config._
    read(topic, groups, 10.seconds, networkBlinkOnWrite, readQSize, readBatchSize)
  }
}
