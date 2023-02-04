package ru.yandex.vertis.moderation.flink

import org.junit.Ignore
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.settings.HttpClientConfig
import ru.yandex.vertis.moderation.util.DateTimeUtil

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author potseluev
  */
@Ignore("For manually running")
class HttpFlinkHistoryClientSpec extends SpecBase {

  private val url = "http://moderation-flink-history.vrts-slb.test.vertis.yandex.net"
  private val flink = new HttpFlinkHistoryClient(url, HttpClientConfig())

  "FlinkHistoryClient" should {
    "get jobs correctly" in {
      val jobs = flink.getJobs.futureValue
      jobs should not be empty
    }

    "get last checkpoint correctly" in {
      flink.getLastCheckpoint("594a09b651aadd1a8f521dd1bc698df7").futureValue shouldBe
        None
      flink.getLastCheckpoint("26e9e89ba86a196e5b1314797443c97c").futureValue shouldBe
        Some(
          RecoveryPoint(
            "s3://misc/moderation-flink/flink-checkpoints/26e9e89ba86a196e5b1314797443c97c/chk-4044",
            DateTimeUtil.fromMillis(1553188414648L),
            isDiscarded = false
          )
        )
    }
  }

}
