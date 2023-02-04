package ru.yandex.realty.telepony

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Ignore, Matchers}
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.http.{HttpEndpoint, RemoteHttpService, TestHttpClient}
import ru.yandex.realty.model.phone.TeleponyInfo
import ru.yandex.realty.telepony.TeleponyClient.Antifraud
import ru.yandex.realty.tracing.Traced

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * author: rmuzhikov
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class HttpTeleponyClientTest
  extends FlatSpec
  with Matchers
  with ScalaFutures
  with TestHttpClient
  with FeaturesStubComponent {
  behavior.of("HttpTeleponyClient")

  implicit private val trace: Traced = Traced.empty

  private val teleponyHost = "telepony-api-int.vrts-slb.test.vertis.yandex.net"
  private val teleponyPort = 80
  private val httpService =
    new RemoteHttpService(
      "telepony",
      HttpEndpoint.apply(teleponyHost, teleponyPort),
      testClient
    )

  val teleponyClient =
    new HttpTeleponyClient(httpService, features)

  it should "getOrCreate new phone" in {
    val objectId = System.nanoTime().toString
    val redirect1 =
      teleponyClient.getOrCreate(
        TeleponyInfo(
          domain = "realty-offers",
          objectId = objectId,
          target = "+79523991438",
          geoId = Some(1),
          ttl = Some(3 minute)
        ),
        antifraud = Some(Antifraud.Enable)
      )

    val redirect2 =
      teleponyClient.getOrCreate(
        TeleponyInfo(
          domain = "realty-offers",
          objectId = objectId,
          target = "+79523991438",
          geoId = Some(1),
          ttl = Some(3 minute)
        )
      )

    redirect1.futureValue.source should equal(redirect2.futureValue.source)
    redirect1.futureValue.objectId should equal(redirect2.futureValue.objectId)
  }

  it should "correct find all calls" in {
    val all = teleponyClient.findCalls("realty-offers", None)
    all.isSuccess should be(true)
    all.get.values.nonEmpty should be(true)
  }

  it should "correct find calls for specified period" in {
    val all = teleponyClient.findCalls(
      "realty-offers",
      Some(DateTime.parse("2017-09-01T00:43:12.925+03:00")),
      Some(DateTime.parse("2017-09-02T00:43:12.925+03:00"))
    )
    all.isSuccess should be(true)
    all.get.values.nonEmpty should be(true)
    println(all.get.values.mkString(","))
  }

  it should "correct find calls for half open period" in {
    val all = teleponyClient.findCalls("realty-offers", Some(DateTime.parse("2017-09-01T00:43:12.925+03:00")))
    all.isSuccess should be(true)
    all.get.values.nonEmpty should be(true)
    println(all.get.values.mkString(","))
  }

  it should "don't find anything for unknown object id" in {
    val none = teleponyClient.findCalls(
      "realty-offers",
      objectIdPrefix = Some(TeleponyObjectIdPrefix.PARTNER),
      objectIdSuffix = Some("fake-partner")
    )
    none.get.values.isEmpty should be(true)
  }

  it should "correct find available phones by geoId" in {
    val count = teleponyClient.findAvailableRedirects("realty-offers", geoId = Some(1))
    count.get should be >= (0)
  }

  it should "correct find available phones by phone" in {
    val countPhone = teleponyClient.findAvailableRedirects("realty-offers", phone = Some("+74957193001"))
    val countGeoId = teleponyClient.findAvailableRedirects("realty-offers", geoId = Some(1))
    countPhone.get should be(countGeoId.get)
  }
}
