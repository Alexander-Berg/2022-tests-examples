package ru.yandex.vertis.broker.controller.lb

import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import org.scalatest.Ignore
import ru.yandex.vertis.broker.controller.conf.LogbrokerDeliveryConfig
import vertis.logbroker.cfgclient.config.LbConfigClientConfig
import vertis.logbroker.cfgclient.model.LbPath
import vertis.logbroker.cfgclient.model.directory.RemoveDirectoryRequest
import vertis.logbroker.cfgclient.model.topic.RemoveTopicRequest
import vertis.logbroker.cfgclient.{LbConfigClient, LbConfigClientImpl}
import zio.test.Assertion._
import zio.test.{assertCompletes, assertM, DefaultRunnableSpec, ZSpec}

import java.util.NoSuchElementException

/**
  */
object LbClientHelperManualSpec extends DefaultRunnableSpec {

  val LogbrokerPlayground = "logbroker-playground"

  val staff = "kusaeva"

  val TestConsumerPath = s"$LogbrokerPlayground/$staff/test-consumer"

  private lazy val TOKEN = {
    val optToken = sys.env.get("LB_TOKEN")
    if (optToken.isEmpty) {
      throw new NoSuchElementException("Specify Logbroker token in LB_TOKEN environment variable")
    }
    optToken.get
  }

  private lazy val config =
    LbConfigClientConfig("cm.logbroker.yandex.net", 1111, TOKEN, staff)

  private val consumer = s"$LogbrokerPlayground/$staff/test-consumer"

  private lazy val clientConfig =
    LbConfigClientConfig("cm.logbroker.yandex.net", 1111, "FAKE_TOKEN", staff)

  private val lbConfig =
    LogbrokerDeliveryConfig(LogbrokerPlayground, consumer, "abc", "responsible", partitionsCount = 1, clientConfig)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("LbClientHelper")(
      /** testing on real logbroker, deletes created resources after work */
      testM("find non-existent dirs") {
        val rootPath = s"$LogbrokerPlayground/$staff/test"

        val topicPath = s"$rootPath/test_dir1/test_dir2/test_dir3/topic"

        val paths =
          Seq(s"$rootPath/test_dir1", s"$rootPath/test_dir1/test_dir2", s"$rootPath/test_dir1/test_dir2/test_dir3")
            .map(LbPath.make)
        LbConfigClientImpl.make(config).use { client =>
          val helper = new LbClientHelper(client, lbConfig)
          assertM(helper.findNonExistentDirs(Seq(LbPath(topicPath).parent)).map(_.toSet))(equalTo(paths.toSet))
        }
      }.provideCustomLayer(Prometheus.live ++ Logging.live),
      /** testing on real logbroker, deletes created resources after work */
      testM("create all dirs correctly") {
        val rootPath = s"$LogbrokerPlayground/$staff/test"

        val topicPath = s"$rootPath/test_dir1/test_dir2/test_dir3/topic"

        val deleteResources = (_: LbConfigClient)
          .executeModifyCommands(
            RemoveTopicRequest(LbPath(topicPath)) +:
              Seq(s"$rootPath/test_dir1/test_dir2/test_dir3", s"$rootPath/test_dir1/test_dir2", s"$rootPath/test_dir1")
                .map(p => RemoveDirectoryRequest(LbPath(p)))
          )
          .either

        LbConfigClientImpl
          .create(config)
          .bracket(c => deleteResources(c) *> c.close) { client =>
            val helper = new LbClientHelper(client, lbConfig)
            helper.createTopic(LbPath(topicPath), Some(lbConfig.consumer)).as(assertCompletes)
          }
      }.provideCustomLayer(Prometheus.live ++ Logging.live)
    )

}
