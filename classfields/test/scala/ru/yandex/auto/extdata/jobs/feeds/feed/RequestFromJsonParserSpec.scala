package ru.yandex.auto.extdata.jobs.feeds.feed

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport

@RunWith(classOf[JUnitRunner])
class RequestFromJsonParserSpec extends WordSpec with Matchers with MockitoSupport {

  private val request: String =
    "{  \"and\": [{   \"state\": \"USED\"  }, {   \"location\": {    \"region_id\": 1   }  }] }"

  private def props(service: String, onlyForDealers: Set[Long]): FeedProperties = {
    val p = mock[FeedProperties]

    when(p.service).thenReturn(service)
    when(p.request).thenReturn(request)
    when(p.onlyForDealers).thenReturn(onlyForDealers)
    when(p.fileName).thenReturn("file")
    when(p.onlyAuction).thenReturn(false)

    p
  }

  "RequestFromJson" should {
    "not fail with dealers" in {
      for {
        service <- Seq(AUTO.name, COMMERCIAL.name)
        dealers <- Seq(Set.empty[Long], Set(1L), Set(1L, 3L, 4L))
      } RequestFromJsonParser.parse(props(service, dealers))
    }
  }
}
