package ru.yandex.vertis.parsing.auto.clients.scrapinghub

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.junit.runner.RunWith
import org.scalatest.{FunSuite, OptionValues}
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.clients.s3.FileStorage
import ru.yandex.vertis.parsing.clients.bucket.DefaultBucketClient
import ru.yandex.vertis.parsing.util.IO
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

import java.util.Date

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DefaultBucketClientTest extends FunSuite with MockitoSupport with OptionValues {
  private val mockedS3 = mock[FileStorage]
  private val mockedIO = mock[IO]

  private val files = Seq(
    "30-fresh.json",
    "31-full.json",
    "32-full.json",
    "33-fresh.json",
    "34-full.json",
    "35-fresh.json",
    "36-fresh.json",
    "37-full.json"
  ).map(key => {
    val e = new S3ObjectSummary
    e.setKey(key)
    e.setLastModified(new Date())
    e
  })

  stub(mockedS3.list(_: String, _: Option[String], _: Int)(_: Traced)) {
    case (_, after, maxKeys, _) =>
      after match {
        case Some(file) =>
          val withAfter = files.dropWhile(_.getKey != file)
          if (withAfter.nonEmpty) withAfter.slice(1, maxKeys + 1)
          else withAfter
        case None => files.take(maxKeys)
      }
  }

  implicit private val trace: Traced = TracedUtils.empty

  private val client = new DefaultBucketClient(mockedS3, mockedIO)

  test("nextFullDump") {
    assert(client.nextFullDump("drom-auto", None).value.key == "31-full.json")
    assert(client.nextFullDump("drom-auto", Some("30-fresh.json")).value.key == "31-full.json")
    assert(client.nextFullDump("drom-auto", Some("31-full.json")).value.key == "32-full.json")
    assert(client.nextFullDump("drom-auto", Some("32-full.json")).value.key == "34-full.json")
    assert(client.nextFullDump("drom-auto", Some("33-fresh.json")).value.key == "34-full.json")
    assert(client.nextFullDump("drom-auto", Some("34-full.json")).value.key == "37-full.json")
    assert(client.nextFullDump("drom-auto", Some("35-fresh.json")).value.key == "37-full.json")
    assert(client.nextFullDump("drom-auto", Some("36-fresh.json")).value.key == "37-full.json")
    assert(client.nextFullDump("drom-auto", Some("37-full.json")).isEmpty)
    assert(client.nextFullDump("drom-auto", Some("abc")).isEmpty)
  }

  test("nextFreshDump") {
    assert(client.nextFreshDump("drom-auto", None).value.key == "30-fresh.json")
    assert(client.nextFreshDump("drom-auto", Some("30-fresh.json")).value.key == "33-fresh.json")
    assert(client.nextFreshDump("drom-auto", Some("31-full.json")).value.key == "33-fresh.json")
    assert(client.nextFreshDump("drom-auto", Some("32-full.json")).value.key == "33-fresh.json")
    assert(client.nextFreshDump("drom-auto", Some("33-fresh.json")).value.key == "35-fresh.json")
    assert(client.nextFreshDump("drom-auto", Some("34-full.json")).value.key == "35-fresh.json")
    assert(client.nextFreshDump("drom-auto", Some("35-fresh.json")).value.key == "36-fresh.json")
    assert(client.nextFreshDump("drom-auto", Some("36-fresh.json")).isEmpty)
    assert(client.nextFreshDump("drom-auto", Some("37-full.json")).isEmpty)
    assert(client.nextFreshDump("drom-auto", Some("abc")).isEmpty)
  }
}
