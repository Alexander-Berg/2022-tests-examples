package vertis.logbroker.client.consumer.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.kikimr.persqueue.compression.CompressionCodec

import scala.util.Random

class LbCompressionSpec extends AnyWordSpec with Matchers {

  "LbCompression.decompress" should {
    "not fail when buffer is too small" in {
      val aLotOfBytes = Random.nextBytes(10 * 1024 * 1024)
      val compressed = LbCompression.compress(aLotOfBytes, CompressionCodec.ZSTD)
      val decompressed = LbCompression.decompress(CompressionCodec.ZSTD)(compressed).readAllBytes()
      decompressed should ===(aLotOfBytes)
    }
  }

}
