package ru.yandex.vertis.telepony.service.impl

import java.net.URI

import org.scalatest.Ignore
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.service.impl.s3.S3ClientImpl
import ru.yandex.vertis.telepony.util.Threads
import software.amazon.awssdk.regions.Region
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._

import scala.concurrent.duration._

@Ignore
class S3ClientSpec extends SpecBase {

  import Threads.lightWeightTasksEc

  val s3Client = new S3ClientImpl(
    bucket = "telepony",
    region = Region.of("ru-central1"),
    uri = new URI("https://s3.mdst.yandex.net"),
    accessId = "???",
    secretId = "???"
  )

  "S3Cient" should {
    "put and delete object" in {
      val key = ShortStr.next
      println(s"key: $key")
      val bytes = LongStr.next.getBytes

      s3Client.put(key, bytes).futureValue
      val actualBytes = s3Client.get(key).futureValue
      s3Client.delete(key).futureValue

      actualBytes shouldBe bytes
    }

    "create presigned url" in {
      val key = ShortStr.next
      println(s"key: $key")
      val bytes = LongStr.next.getBytes

      s3Client.put(key, bytes).futureValue
      val url = s3Client.getUrl(key, 1.hours).futureValue
      s3Client.delete(key).futureValue

      url.toString should not be empty
      println(s"url: $url")
    }

    "fail create presigned url for invalid ttl" in {
      val key = ShortStr.next
      s3Client.getUrl(key, 1.minutes).failed.futureValue shouldBe an[IllegalArgumentException]
      s3Client.getUrl(key, 9.hours).failed.futureValue shouldBe an[IllegalArgumentException]
    }
  }

}
