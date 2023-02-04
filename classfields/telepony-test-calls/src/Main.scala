package ru.yandex.vertis.etc.telepony.telepony_test_calls

import com.typesafe.config.Config
import common.yt.YtConfig
import common.yt.live.YtLive
import common.zio.akka.Akka
import common.zio.app.BaseApp
import common.zio.app.BaseApp.BaseEnvironment
import common.zio.config.Configuration
import common.zio.config.Configuration.Configuration
import common.zio.doobie.MkTransactor
import common.zio.pureconfig.Pureconfig
import common.zio.vertis_scheduler.VertisSchedulerStarter
import common.zio.vertis_scheduler.VertisSchedulerStarter.{VertisSchedulerConfig, VertisSchedulerStarter}
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.YdbConfig
import common.zookeeper.Zookeeper
import ru.yandex.vertis.etc.telepony.telepony_test_calls.TestCallsTask.{TestCallsConfig, TestCallsEnv, TestCallsTask}
import ru.yandex.vertis.telepony.settings.{BrokerSettings, CuratorSettings, DualMysqlConfig, DustSettings, KafkaSettings, RecordSettings, S3Settings, StatusSettings}
import zio.{Has, Tag, ZIO, ZLayer}
import zio.magic._

object Main extends BaseApp {

    override type Env = BaseEnvironment with VertisSchedulerStarter with TestCallsTask

    override def makeEnv: ZLayer[BaseEnvironment, Throwable, Env] = {
        ZLayer.fromSomeMagic[BaseEnvironment, Env](
            Zookeeper.live,
            Akka.live,
            Pureconfig.loadLayer[VertisSchedulerConfig]("vertis-scheduler"),
            VertisSchedulerStarter.live,
            Pureconfig.loadLayer[YtConfig]("yt"),
            YtLive.configuration,
            YtLive.http,
            MkTransactor("yql").toLayer,
            loadConf("telepony.shared.mysql", DualMysqlConfig.apply),
            Pureconfig.loadLayer[TestCallsConfig]("test-calls-config"),
            Ydb.config,
            Ydb.live,
            loadConf("telepony.domain.default.status", StatusSettings.apply),
            loadConf("telepony.s3-mds", S3Settings.apply),
            loadConf("telepony.domain.default.record", RecordSettings.apply),
            loadConf("telepony.zookeeper", CuratorSettings.fromConfig),
            loadConf("telepony.broker", BrokerSettings.apply),
            loadConf("telepony.kafka", KafkaSettings.apply),
            loadConf("telepony.dust", DustSettings.apply),
            TestCallsTask.layer
        )
    }

    private def loadConf[C: Tag](path: String, makeConf: Config => C): ZLayer[Configuration, Nothing, Has[C]] =
        Configuration.config.flatMap(c => ZIO.succeed(makeConf(c.getConfig(path)))).toLayer

    override def program: ZIO[Env, Throwable, Any] = {
        for {
            testCallsTask <- ZIO.access[TestCallsTask](_.get.task)
            _ <- VertisSchedulerStarter.startAndRun(testCallsTask)
        } yield ()
    }
}
