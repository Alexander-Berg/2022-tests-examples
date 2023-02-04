package ru.yandex.vos2.autoru.services.youtube

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.baker.env.Env
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus
import ru.yandex.vos2.autoru.TestEnvProvider
import ru.yandex.vos2.autoru.services.MockYoutubeClientHelper
import ru.yandex.vos2.util.HttpBlockingPool.Instance

/**
  * Created by andrey on 8/25/16.
  */
@RunWith(classOf[JUnitRunner])
class YoutubeClientTest extends AnyFunSuite with MockYoutubeClientHelper {

  val env: Env = new Env(TestEnvProvider)

  test("testCheckVideoAvailable") {
    val youtubeConfig = env.serviceConfig.getConfig("youtube")
    val proxyConfig = env.serviceConfig.getConfig("ipv4.proxy")
    val youtubeClient = new DefaultYoutubeClient(
      youtubeConfig.getString("host"),
      youtubeConfig.getInt("port"),
      proxyConfig,
      None,
      "api-key"
    ) {
      override protected val client = new Instance(mockHttpClient(200, videoExistAnswer))
    }
    assert(youtubeClient.hostname == "www.googleapis.com")

    assert(youtubeClient.checkVideoAvailable("AamdEYGZwFA") == YoutubeVideoStatus.AVAILABLE)
  }

  test("testClientAnswerUnavailable") {
    val youtubeConfig = env.serviceConfig.getConfig("youtube")
    val proxyConfig = env.serviceConfig.getConfig("ipv4.proxy")
    val youtubeClient = new DefaultYoutubeClient(
      youtubeConfig.getString("host"),
      youtubeConfig.getInt("port"),
      proxyConfig,
      None,
      "api-key"
    ) {
      override protected val client = new Instance(mockHttpClient(200, videoNotExistAnswer))
    }
    assert(youtubeClient.checkVideoAvailable("AamdEYGZwFA") == YoutubeVideoStatus.UNAVAILABLE)
  }

  test("testClientAnswer500") {
    val youtubeConfig = env.serviceConfig.getConfig("youtube")
    val proxyConfig = env.serviceConfig.getConfig("ipv4.proxy")
    val youtubeClient = new DefaultYoutubeClient(
      youtubeConfig.getString("host"),
      youtubeConfig.getInt("port"),
      proxyConfig,
      None,
      "api-key"
    ) {
      override protected val client = new Instance(mockHttpClient(500))
    }
    assert(youtubeClient.checkVideoAvailable("AamdEYGZwFA") == YoutubeVideoStatus.UNKNOWN)
  }

  test("testClientAnswerException") {
    val youtubeConfig = env.serviceConfig.getConfig("youtube")
    val proxyConfig = env.serviceConfig.getConfig("ipv4.proxy")
    val youtubeClient = new DefaultYoutubeClient(
      youtubeConfig.getString("host"),
      youtubeConfig.getInt("port"),
      proxyConfig,
      None,
      "api-key"
    ) {
      override protected val client = new Instance(mockHttpClient(200, videoExistAnswer))
    }
    try {
      youtubeClient.checkVideoAvailable("AamdEYGZwFA")
      fail()
    } catch {
      case _: Exception =>
    }
  }
}
