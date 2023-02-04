package vertis.broker.yops

import common.yt.Yt.Attribute.ModificationTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import vertis.broker.yops.tasks.util.YopsUtils._
import java.time.{Instant, LocalDate, Period}
import scala.concurrent.duration._

/** @author kusaeva
  */
class YopsUtilsSpec extends AnyWordSpec with Matchers {

  "YopsUtils" should {
    "correctly check elapsed silence period" in {
      val elapsed = ModificationTime.decoder.decodeUnsafe(YTree.stringNode("2020-01-20T16:09:07.403048Z"))
      isSilenceElapsed(elapsed, 1.hour) shouldBe true
    }

    "correctly check non-elapsed silence period" in {
      val notElapsed = ModificationTime.decoder.decodeUnsafe(
          YTree.stringNode(
            Instant
              .now()
              .minusSeconds(60)
              .toString
          )
        )
      isSilenceElapsed(notElapsed, 2.minute) shouldBe false
    }

    "check if table has tiny chunks and needs concatenation correctly" in {
      val desiredChunkSizeBytes = 128L * 1024 * 1024

      val tiny = hasTinyChunks(324, 8946389L, desiredChunkSizeBytes)
      val small = hasTinyChunks(25, 5945785L, desiredChunkSizeBytes)
      val bigTable = hasTinyChunks(806, 6974871180L, desiredChunkSizeBytes)
      val single = hasTinyChunks(1, 6809174L, desiredChunkSizeBytes)
      withClue("tiny:")(tiny shouldBe true)
      withClue("fat:")(small shouldBe true)
      withClue("bigTable:")(bigTable shouldBe true)
      withClue("single:")(single shouldBe false)
    }

    "check if day is alive" in {
      val now = LocalDate.now()
      val maxAgeDays = 7
      for (i <- 0 until maxAgeDays) {
        isAlivePeriod(now.minusDays(i.toLong), maxAgeDays) shouldBe true
      }
      for (i <- maxAgeDays until (maxAgeDays + 3)) {
        isAlivePeriod(now.minusDays(i.toLong), maxAgeDays) shouldBe false
      }
    }

    "check if day should be sorted" in {
      val now = LocalDate.now()
      val daysToSpawn = 2
      for (i <- 0 until daysToSpawn) {
        dontSortYet(now.minusDays(i.toLong), Some(Period.ofDays(daysToSpawn))) shouldBe true
      }
      for (i <- daysToSpawn until (daysToSpawn + 3)) {
        dontSortYet(now.minusDays(i.toLong), Some(Period.ofDays(daysToSpawn))) shouldBe false
      }
    }
  }
}
