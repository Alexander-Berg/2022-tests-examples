package ru.yandex.realty.telepony

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.apache.http.impl.client.HttpClientBuilder
import org.scalatest.{FlatSpec, Ignore, Matchers}

import scala.language.postfixOps

/**
  * author: rmuzhikov
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class HttpPhoneUnifierClientTest extends FlatSpec with Matchers {
  behavior.of("HttpPhoneUnifierClient")

  val httpClient = HttpClientBuilder.create().useSystemProperties().build()
  val phoneUnifierClient = new HttpPhoneUnifierClient("hydra-01-myt.test.vertis.yandex.net", 35530, httpClient)

  it should "unify raw phone" in {
    val rawPhone = "+79523991438"
    val phoneInfo = phoneUnifierClient.unify(rawPhone)
    phoneInfo.phone should equal(rawPhone)
    phoneInfo.geoId should equal(2)
    phoneInfo.phoneType should equal("Mobile")
  }

}
