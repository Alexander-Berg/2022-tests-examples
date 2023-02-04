package ru.yandex.vertis.uploader.clients.mds

import akka.http.scaladsl.model.ContentTypes
import org.scalatest.WordSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import ru.yandex.library.ticket_parser2.{TvmApiSettings, TvmClient}
import ru.yandex.vertis.baker.components.http.client.HttpClient
import ru.yandex.vertis.baker.components.http.client.config.HttpClientConfig
import ru.yandex.vertis.baker.components.http.client.tvm.{TvmClientWrapper, TvmClientWrapperImpl}
import ru.yandex.vertis.baker.lifecycle.DefaultApplication
import ru.yandex.vertis.baker.util.ConfigUtils._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.uploader.util.RandomUtils

import java.io.File
import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 30.03.17
  */
class MdsClientTest extends WordSpec with ScalaFutures with IntegrationPatience {
  private val app = new DefaultApplication {}

  implicit private val components: TestComponents = new TestComponents(app)

  implicit private val tvmClient: TvmClientWrapper = {
    val tvmConf = app.env.serviceConfig.getConfig("tvm")
    val selfClientId = tvmConf.getInt("self.id")
    val secret = tvmConf.getString("self.secret")
    val serviceIds = Seq(
      tvmConf.getInt("mds.avatars.id"),
      tvmConf.getInt("mds.id")
    )
    val settings: TvmApiSettings = TvmApiSettings
      .create()
      .setSelfClientId(selfClientId)
      .enableServiceTicketsFetchOptions(secret, serviceIds.toArray)
    new TvmClientWrapperImpl(new TvmClient(settings))
  }

  private val mdsClient: MdsClient = {
    val mdsTvmId = app.env.serviceConfig.getInt("tvm.mds.id")
    val conf = app.env.serviceConfig.getConfig("mds.write")
    val httpConfig =
      HttpClientConfig.newBuilder("mds").withTimeout(30.seconds).withHostPortFrom(conf).withTvmFor(mdsTvmId).build
    val httpClient = HttpClient.newBuilder(httpConfig).build
    new MdsClient(
      httpClient,
      app.env.serviceConfig.getString("mds.read.url.private"),
      app.env.serviceConfig.getString("mds.read.url.public"),
      app.env.serviceConfig.getMap("mds.keys"),
      app.env.serviceConfig.getStringList("mds.private-namespaces").asScala.toList
    )
  }

  implicit private val trace: Traced = Traced.empty

  "MdsClient" should {
    "upload file" ignore {
      val res = mdsClient.upload(
        namespace = "parts",
        ContentTypes.`application/json`,
        new File(TestUtils.filePath).toPath,
        RandomUtils.nextHexString(32)
      )
      println(res.futureValue)
    }
  }

  "MdsAvatarsClient" should {
    "delete uploaded file" ignore {
      val name = RandomUtils.nextHexString(32)
      val resp = mdsClient.upload(
        namespace = "parts",
        ContentTypes.`application/json`,
        new File(TestUtils.filePath).toPath,
        name
      )
      val result = resp.futureValue
      val delRes = mdsClient.delete(namespace = "parts", groupId = result.groupId, name)
      println(delRes.futureValue)
    }
  }

}
