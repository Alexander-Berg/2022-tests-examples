package ru.yandex.vertis.uploader.clients.mds

import akka.http.scaladsl.model.ContentTypes
import org.scalatest.WordSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import ru.yandex.library.ticket_parser2.{TvmApiSettings, TvmClient}
import ru.yandex.vertis.baker.components.http.client.HttpClient
import ru.yandex.vertis.baker.components.http.client.config.HttpClientConfig
import ru.yandex.vertis.baker.components.http.client.tvm.{TvmClientWrapper, TvmClientWrapperImpl}
import ru.yandex.vertis.baker.lifecycle.DefaultApplication
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.uploader.util.RandomUtils
import scala.concurrent.duration._
import java.io.File

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 30.03.17
  */
class MdsAvatarsClientTest extends WordSpec with ScalaFutures with IntegrationPatience {
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

  private val mdsAvatarsClient: MdsAvatarsClient = {
    val mdsAvatarsTvmId = app.env.serviceConfig.getInt("tvm.mds.avatars.id")
    val conf = app.env.serviceConfig.getConfig("avatars.write")
    val httpConfig = HttpClientConfig
      .newBuilder("avatars")
      .withTimeout(30.seconds)
      .withHostPortFrom(conf)
      .withTvmFor(mdsAvatarsTvmId)
      .build
    val httpClient = HttpClient.newBuilder(httpConfig).build
    new MdsAvatarsClient(httpClient, app.env.serviceConfig.getString("avatars.read.url"))
  }

  implicit private val trace: Traced = Traced.empty

  "MdsAvatarsClient" should {
    "upload file" ignore {
      val res = mdsAvatarsClient.upload(
        namespace = "sto",
        ContentTypes.`application/json`,
        new File(TestUtils.filePath).toPath,
        RandomUtils.nextHexString(32),
        None
      )
      println(res.futureValue)
    }
  }

  "MdsAvatarsClient" should {
    "delete uploaded file" ignore {
      val name = RandomUtils.nextHexString(32)
      val resp = mdsAvatarsClient.upload(
        namespace = "sto",
        ContentTypes.`application/json`,
        new File(TestUtils.filePath).toPath,
        name,
        None
      )
      val result = resp.futureValue
      val delRes = mdsAvatarsClient.delete(namespace = "sto", groupId = result.groupId, name)
      println(delRes.futureValue)
    }
  }

}
