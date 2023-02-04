package ru.auto.cabinet.service.s3

import org.scalatest.Ignore
import org.scalatest.time.{Seconds, Span}
import ru.auto.cabinet.test.BaseSpec
import ru.yandex.vertis.s3edr.core.s3.{S3Auth, S3ClientFactory, S3Settings}

import scala.concurrent.duration.DurationLong

/** чтобы не зависить от внешней инфраструктуры на CI
  */
@Ignore
class S3ClientWrapperSpec extends BaseSpec {

  val bucket = "testing-auto"
  val name = "testing-auto"

  val s3Settings = S3Settings(
    url = "s3.mds.yandex.net",
    auth = S3Auth(
      key = "",
      secret = ""
    ),
    bucket = bucket,
    region = "yndx",
    connectionTimeout = 5000.millis,
    requestTimeout = 60000.millis,
    numRetries = 3,
    maxConnections = 5
  )

  val wrapper = new S3ClientWrapper(
    S3ClientFactory(s3Settings),
    bucket
  )

  "S3ClientWrapper" should {
    "list files keys in bucket" in {
      wrapper
        .getFileList(prefix = "auto/dealers")
        .futureValue(timeout(Span(5, Seconds))) shouldBe List(
        "auto/dealers/dealers-export.txt",
        "auto/dealers/ya-maps-export")
    }

    "read object" in {
      new String(
        wrapper
          .read(name, "auto/dealers/test.txt")
          .futureValue(timeout(Span(5, Seconds)))) shouldBe "test record"
    }
  }

}
