package vertis.logbroker.cfgclient

import org.scalatest.Ignore

import java.time
import java.util.{NoSuchElementException, Optional}
import org.testcontainers.containers.{Network, ToxiproxyContainer}
import org.testcontainers.utility.DockerImageName
import vertis.logbroker.cfgclient.config.LbConfigClientConfig
import vertis.logbroker.cfgclient.exceptions.{LbOperationException, LbProtoParseException}
import vertis.logbroker.cfgclient.model.LbPath
import vertis.logbroker.cfgclient.model.consumer.{ConsumerProperties, CreateConsumerRequest, RemoveConsumerRequest}
import vertis.logbroker.cfgclient.model.directory.{CreateDirectoryRequest, DirectoryProperties, RemoveDirectoryRequest}
import vertis.logbroker.cfgclient.model.readrule.ReadRuleMode.{AllOriginal, MirrorToCluster}
import vertis.logbroker.cfgclient.model.readrule.{CreateReadRuleRequest, ReadRule, RemoveReadRuleRequest}
import vertis.logbroker.cfgclient.model.topic.TopicDescription.TopicInstance
import vertis.logbroker.cfgclient.model.topic.{CreateTopicRequest, RemoveTopicRequest, TopicProperties}
import vertis.zio.test.ZioSpecBase
import zio.ZIO

@Ignore
class LbConfigClientIntSpec extends ZioSpecBase {

  private lazy val TOKEN = {
    val optToken = sys.env.get("LB_TOKEN")
    if (optToken.isEmpty) {
      throw new NoSuchElementException("Specify Logbroker token in LB_TOKEN environment variable")
    }
    optToken.get
  }

  private val VertisBrokerTest = "vertis/broker/test"
  private val LogbrokerPlayground = "logbroker-playground"
  private val SubscriptionsTopic = s"$VertisBrokerTest/client-topics/subscriptions/notification-event"

  private lazy val config = LbConfigClientConfig("cm.logbroker.yandex.net", 1111, TOKEN, "robot-vertis-zvez")

  private lazy val toxiproxy = {
    val imageName = DockerImageName.parse("shopify/toxiproxy");
    val p = new ToxiproxyContainer(imageName)
      .withNetwork(Network.SHARED)
      .withStartupTimeout(time.Duration.ofSeconds(20))
    p.start()
    p
  }

  "describeTopic" should {

    "get topic info correctly" in ioTest {
      ZIO.bracket(ZIO.succeed(LbConfigClientImpl(config)))(_.close) { client =>
        for {
          subscriptionsTopic <- client.describeTopic(LbPath(SubscriptionsTopic))
          fallbackTopic <- client.describeTopic(LbPath(s"$VertisBrokerTest/fallback"))
          _ <- check("number of partitions:") {
            subscriptionsTopic.partitionCount shouldBe 2
            fallbackTopic.partitionCount shouldBe 1
          }
          _ <- check("read rules:") {
            subscriptionsTopic.readRules should contain
            ReadRule(LbPath(SubscriptionsTopic), LbPath(s"$VertisBrokerTest/consumer"), AllOriginal)
            fallbackTopic.readRules shouldBe Seq.empty
          }
          _ <- check("instances:") {
            val instances = Seq(
              TopicInstance("iva", "iva"),
              TopicInstance("man", "man"),
              TopicInstance("myt", "myt"),
              TopicInstance("sas", "sas"),
              TopicInstance("vla", "vla")
            )

            subscriptionsTopic.topicInstances.sortBy(instance => instance.toString) shouldBe instances
            fallbackTopic.topicInstances.sortBy(instance => instance.toString) shouldBe instances
          }
        } yield ()
      }
    }

    "fail if topic does not exist" in ioTest {
      ZIO.bracket(ZIO.succeed(LbConfigClientImpl(config)))(_.close) { client =>
        for {
          result <- client.describeTopic(LbPath(s"$VertisBrokerTest/this-topic-does-not-exist")).either
          _ <- check("task failed:") {
            result.isLeft shouldBe true
          }
          exception = result.swap.getOrElse(new NoSuchElementException())
          _ <- check("exception class") {
            assert(exception.isInstanceOf[LbOperationException])
          }
          _ <- check("fail message:") {
            assert(exception.getMessage contains "NOT_FOUND")
          }
        } yield ()
      }
    }

    "fail if the specified path is directory" in ioTest {
      ZIO.bracket(ZIO.succeed(LbConfigClientImpl(config)))(_.close) { client =>
        for {
          result <- client.describeTopic(LbPath(s"$VertisBrokerTest")).either
          _ <- check("task failed:") {
            result.isLeft shouldBe true
          }
          exception = result.swap.getOrElse(new NoSuchElementException())
          _ <- check("exception class") {
            assert(exception.isInstanceOf[LbProtoParseException])
          }
          _ <- check("fail message:") {
            assert(exception.getMessage contains "Logbroker returned a description of DIRECTORY")
          }
        } yield ()
      }
    }

    "fail if connection lags" in ioTest {
      val proxy = toxiproxy.getProxy(config.host, config.port, Optional.empty)
      val proxyConfig = config.copy(host = proxy.getContainerIpAddress, port = proxy.getProxyPort)
      proxy.setConnectionCut(true)
      ZIO.bracket(ZIO.succeed(LbConfigClientImpl(proxyConfig)))(_.close) { client =>
        for {
          result <- client.describeTopic(LbPath(s"$VertisBrokerTest/dev-topic")).either
          _ <- check("task failed:") {
            result.isLeft shouldBe true
          }
        } yield ()
      }
    }
  }

  "executeModifyCommands" should {
    "create directory, topic and consumer correctly" in ioTest {
      val directoryPath = s"$LogbrokerPlayground/vertis-broker-tests"
      val topicPath = s"$directoryPath/test-topic"
      val partitionCount = 3
      val consumerPath = s"$directoryPath/some-consumer"
      val readRule = ReadRule(LbPath(topicPath), LbPath(consumerPath), MirrorToCluster("sas"))

      val createDirectoryRequest = CreateDirectoryRequest(LbPath(directoryPath), DirectoryProperties())
      val createTopicRequest = CreateTopicRequest(LbPath(topicPath), TopicProperties(Some(partitionCount)))
      val createConsumer = CreateConsumerRequest(LbPath(consumerPath), ConsumerProperties())
      val createReadRuleRequest = CreateReadRuleRequest(readRule)

      val deleteCreatedResources = (_: LbConfigClient)
        .executeModifyCommands(
          Seq(
            RemoveReadRuleRequest(readRule),
            RemoveConsumerRequest(LbPath(consumerPath)),
            RemoveTopicRequest(LbPath(topicPath)),
            RemoveDirectoryRequest(LbPath(directoryPath))
          )
        )
        .either

      ZIO.bracket(ZIO.succeed(LbConfigClientImpl(config))) { a =>
        deleteCreatedResources(a) *> a.close
      } { client =>
        for {
          _ <- deleteCreatedResources(client).ignore
          _ <- client.executeModifyCommands(
            Seq(createDirectoryRequest, createTopicRequest, createConsumer, createReadRuleRequest)
          )
          topic <- client.describeTopic(LbPath(topicPath))
          _ <- check("topic partitions:") {
            topic.partitionCount shouldBe partitionCount
          }
          _ <- check("read rule:") {
            topic.readRules shouldBe Seq(readRule)
          }
        } yield ()
      }
    }
  }
}
