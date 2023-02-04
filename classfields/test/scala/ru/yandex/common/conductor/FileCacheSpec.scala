package ru.yandex.common.conductor

import java.io.File

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.util.Success

/**
 * Specs on [[FileCache]]
 */
class FileCacheSpec
  extends WordSpec
  with Matchers {

  val TTL = 1.second

  val cache = new FileCache(new File("/tmp"), TTL, "TestData")

  "FileCache" should {
    "write and read single line" in {
      cache.write("foo", Iterable("bar")) should be(Success(()))
      cache.read("foo") should be(Success(Some(Iterable("bar"))))
    }

    "write and read multiple lines" in {
      val lines = (1 to 100).map(_.toString)
      cache.write("baz", lines) should be(Success(()))
      cache.read("baz") should be(Success(Some(lines)))
    }

    "read None from non-exist file" in {
      cache.read("bar") should be(Success(None))
    }

    "read None after TTL expiration" in {
      cache.write("foo", Iterable("bar")) should be(Success(()))
      Thread.sleep(TTL.toMillis)
      cache.read("foo") should be(Success(None))
    }
  }

}
