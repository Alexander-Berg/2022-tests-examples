package ru.auto.chatbot.app

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import ru.auto.chatbot.app.env.DefaultEnvironment

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-05.
  */
class TestEnvironment extends DefaultEnvironment {
  override lazy val serviceName: String = "test"
  override lazy val componentName: String = "test"
  override val version: String = "test"
  override lazy val hostName: String = "test"
  override lazy val dataCenter: String = "test"
  override lazy val environmentType: String = "development"
  override lazy val opsPort: Int = 9999

  override val dataPath: File = new File("./")

  override lazy val config: Config = ConfigFactory.parseString(configString)

  private lazy val configString =
    """autoru-chat-bot {
      |  bot-user-id = "chatbot:vibiralshik-dev"
      |  zookeeper.connect-string = "zookeeper-legacy-01-vla.test.vertis.yandex.net:2181,zookeeper-legacy-01-sas.test.vertis.yandex.net:2181,zookeeper-legacy-01-myt.test.vertis.yandex.net:2181"
      |
      |  extdata.url = "http://auto2-eds-test-int.slb.vertis.yandex.net"
      |  extdata.port = 80
      |
      |  chat-api.host = "chat-api-auto-server.vrts-slb.test.vertis.yandex.net"
      |  chat-api.port = 80
      |  chat-api.schema = "http"
      |
      |  vos.host = "vos2-autoru-api.vrts-slb.test.vertis.yandex.net"
      |  vos.port = 80
      |  vos.schema = "http"
      |
      |  vin-decoder.host = "vin-decoder-api-01-sas.test.vertis.yandex.net"
      |  vin-decoder.port = 36314
      |  vin-decoder.schema = "http"
      |
      |  yavision.host = "yavision.yandex.net"
      |  yavision.port = 80
      |  yavision.schema = "http"
      |
      |  clustering.host = "user-clustering-api-int.vrts-slb.test.vertis.yandex.net"
      |  clustering.port = 80
      |  clustering.schema = "http"
      |
      |  autoru-api.host = "autoru-api-server-int.vrts-slb.test.vertis.yandex.net"
      |  autoru-api.port = 80
      |  autoru-api.schema = "http"
      |
      |  avatars.host = "avatars.mdst.yandex.net"
      |}
      |
      |db {
      |  default {
      |    driver = "org.postgresql.Driver"
      |    url = "jdbc:postgresql://sas-lu4xwdymmkg5n7gr.db.yandex.net:6432,vla-jhnl9dr4a0eph320.db.yandex.net:6432/autoru-chat-bot?ssl=true&prepareThreshold=0&targetServerType=master&socketTimeout=3"
      |    user = "autoru_chat_bot"
      |    password = "rahphieshauBoh1feiTaiS9tiek0ahw9"
      |  }
      |}
      |
      |auto.s3edr {
      |  s3 {
      |    url = "http://s3.mdst.yandex.net"
      |    bucket = "auto"
      |    auth {
      |      key = "dhwjMq6rncq7lpjbf9J7"
      |      secret = "6dcX6pOIZiYnR70x44pJAE+hf4V1GMno5iDYUbFP"
      |    }
      |  }
      |
      |  key-prefix = "autoru-extdata"
      |}""".stripMargin
}
