package ru.yandex.vertis.billing.model_core

import java.io.EOFException
import java.util.zip.ZipException

import com.google.common.base.Charsets
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.util.GzipUtil
import ru.yandex.vertis.billing.util.GzipUtil.{compress, decompress}

import scala.util.{Failure, Success}

/**
  * Specs on [[GzipUtil]] compressions
  *
  * @author alesavin
  */
class CompressionUtilSpec extends AnyWordSpec with Matchers {

  val data = "Hello, world!!!"

  val b0 = 1L << 63
  val b1 = 1L << 62
  val rel = 1
  info(s"Rel: $rel")
  info(s"Rel: ${rel & ~b0}")
  info(s"Rel: ${((rel & ~b0) >> 1) | b1}")

  "CompressionUtil" should {
    "compress and decompress" in {
      val dc = compress(data.getBytes(Charsets.UTF_8)).get
      val d = decompress(dc).get
      new String(d) should be(data)
    }
    "compress empty" in {
      compress(Array[Byte]()) match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "fail on decompress empty" in {
      decompress(Array[Byte]()) match {
        case Failure(e: EOFException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "fail on decompress" in {
      decompress(data.getBytes(Charsets.UTF_8)) match {
        case Failure(e: ZipException) => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
