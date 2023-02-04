package ru.yandex.vos2.autoru.model

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.Inspectors
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.model.UserRef

import scala.util.Success

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 09.12.16
  */
@RunWith(classOf[JUnitRunner])
class VBucketsTest extends AnyFunSuite with Inspectors {

  val testData = Seq(
    ("12bda0", 227, "ec"),
    ("f2bea5", 227, "0a"),
    ("f2baa5", 226, "0f")
  )

  test("generate vBucket from user_ref") {
    val vBucket = VBuckets.get(UserRef.refAid(123))
    vBucket shouldBe 227
  }

  test("encode vBucket with hash") {
    forEvery(testData) {
      case (hash, vBucket, vBucketHex) =>
        VBuckets.encode(hash, vBucket) shouldBe vBucketHex
    }
  }

  test("decode vBucket with hash") {
    forEvery(testData) {
      case (hash, vBucket, vBucketHex) =>
        VBuckets.decode(hash, vBucketHex) shouldBe Success(vBucket)
    }
  }

  test("encode and decode") {
    forEvery(testData) { data =>
      for (i <- 0 until 256) {
        val hash = data._1
        val encoded = VBuckets.encode(hash, i)
        VBuckets.decode(hash, encoded) shouldBe Success(i)
      }
    }
  }

  test("wrong vBucket string") {
    VBuckets.decode("aaaaaa", "zz").toOption shouldBe None
    VBuckets.decode("aaaaaa", "zzz").toOption shouldBe None
  }
}
