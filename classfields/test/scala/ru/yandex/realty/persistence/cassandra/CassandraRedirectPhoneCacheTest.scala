package ru.yandex.realty.persistence.cassandra

import org.joda.time.{DateTime, Duration => JodaDuration}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Ignore, Matchers}
import ru.yandex.realty.application.IndexerCassandraSession
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.model.phone.PhoneRedirect

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * author: rmuzhikov
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class CassandraRedirectPhoneCacheTest extends FlatSpec with Matchers with IndexerCassandraSession {

  override def indexerCassandraSessionConfig: CassandraSessionConfig =
    CassandraSessionConfig(
      nodes = "2a02:6b8:c02:5c0:0:1459:1dfc:2",
      routes = "sas->DC1,fol->DC1,iva->DC1,myt->DC1,ugr->DC1,development->DC1",
      username = "realty",
      password = "realty",
      keyspace = "realty",
      readTimeoutMs = 10000,
      fetchSize = Some(64)
    )

  lazy val redirectPhoneCache = new CassandraRedirectPhoneCache(indexerCassandraSession)

  behavior.of("CassandraRedirectPhoneCache")

  it should "not contain new offerId" in {
    redirectPhoneCache.getRedirects(System.nanoTime().toString, None) should be(empty)
  }

  it should "save redirects" in {
    val offerId = System.nanoTime().toString
    val phone = "+78001234567"
    val tag = Some("tag")
    val redirect = buildPhoneRedirect(offerId, "+78007654321", phone)
    redirectPhoneCache.putRedirects(offerId, tag, Map(phone -> redirect))
    val cachedRedirect = redirectPhoneCache.getRedirects(offerId, tag)
    cachedRedirect should have size 1
    cachedRedirect.get(phone) should be(Some(redirect))
    redirectPhoneCache.removeAll()

    redirectPhoneCache.putRedirect(phone, tag, phone, redirect)
    val cachedRedirect2 = redirectPhoneCache.getRedirect(phone, tag, phone)
    cachedRedirect2 should be(Some(redirect))
    redirectPhoneCache.removeAll()
  }

  it should "add new redirects to existing" in {
    val offerId = System.nanoTime().toString
    val phone1 = "+78001234567"
    val redirect1 = buildPhoneRedirect(offerId, "+78007654321", phone1)
    val phone2 = "+79001234567"
    val redirect2 = buildPhoneRedirect(offerId, "+79007654321", phone2)
    val phone3 = "+79991234567"
    val redirect3 = buildPhoneRedirect(offerId, "+79997654321", phone3)
    redirectPhoneCache.putRedirects(offerId, None, Map(phone1 -> redirect1))
    redirectPhoneCache.putRedirects(offerId, None, Map(phone2 -> redirect2))
    redirectPhoneCache.putRedirect(offerId, None, phone3, redirect3)
    val cachedRedirect = redirectPhoneCache.getRedirects(offerId, None)

    cachedRedirect should have size 3
    cachedRedirect.get(phone1) should be(Some(redirect1))
    cachedRedirect.get(phone2) should be(Some(redirect2))
    cachedRedirect.get(phone3) should be(Some(redirect3))
    redirectPhoneCache.getRedirect(offerId, None, phone3) should be(Some(redirect3))

    redirectPhoneCache.removeAll()
  }

  it should "replace redirect" in {
    val offerId = System.nanoTime().toString
    val phone = "+78001234567"
    val redirect1 = buildPhoneRedirect(offerId, "+78007654321", phone)
    val redirect2 = buildPhoneRedirect(offerId, "+79007654321", phone)
    val redirect3 = buildPhoneRedirect(offerId, "+79997654321", phone)
    redirectPhoneCache.putRedirects(offerId, None, Map(phone -> redirect1))
    redirectPhoneCache.putRedirects(offerId, None, Map(phone -> redirect2))
    val cachedRedirect = redirectPhoneCache.getRedirects(offerId, None)
    cachedRedirect should have size 1
    cachedRedirect.get(phone) should be(Some(redirect2))

    redirectPhoneCache.putRedirect(offerId, None, phone, redirect3)
    redirectPhoneCache.getRedirect(offerId, None, phone) should be(Some(redirect3))

    redirectPhoneCache.removeAll()
  }

  it should "add redirect with ttl" in {
    val ttl = 1 second
    val offerId = System.nanoTime().toString
    val phone1 = "+78001234567"
    val phone2 = "+79001234567"
    val phone3 = "+79991234567"
    val redirect1 = buildPhoneRedirect(offerId, "+78007654321", phone1)
    val redirect2 = buildPhoneRedirect(offerId, "+79007654321", phone2)
    val redirect3 = buildPhoneRedirect(offerId, "+79997654321", phone3)
    redirectPhoneCache.putRedirects(offerId, None, Map(phone1 -> redirect1), Some(ttl))
    redirectPhoneCache.putRedirects(offerId, None, Map(phone2 -> redirect2))
    redirectPhoneCache.putRedirect(offerId, None, phone3, redirect3, Some(ttl))

    Thread.sleep(ttl.toMillis)

    val cachedRedirect = redirectPhoneCache.getRedirects(offerId, None)
    cachedRedirect should have size 1
    cachedRedirect.get(phone2) should be(Some(redirect2))
    redirectPhoneCache.removeAll()
  }

  private def buildPhoneRedirect(objectId: String, source: String, target: String): PhoneRedirect = {
    PhoneRedirect(
      "realty-offers",
      id = System.nanoTime().toString,
      tag = None,
      objectId = objectId,
      createTime = new DateTime(),
      deadline = Some(new DateTime().plus(JodaDuration.standardDays(1))),
      source = source,
      target = target,
      phoneType = None,
      geoId = None,
      ttl = None
    )
  }
}
