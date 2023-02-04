package ru.yandex.vertis.personal.history

import com.couchbase.client.java.document.json.JsonObject
import org.joda.time.DateTime
import ru.yandex.vertis.personal.model.history.HistoryItem
import ru.yandex.vertis.personal.util.BaseSpec

class HistoryItemConverterSpec extends BaseSpec {
  private val now = new DateTime(2018, 2, 7, 15, 50)

  "HistoryItemConverter" should {
    "convert items to json" in {
      HistoryItemConverter.toJs(HistoryItem("1", now, None)).toString shouldBe
        """{"e":"1","v":1518007800000}"""
      HistoryItemConverter
        .toJs(HistoryItem("1", now, Some("2"), Some(5)))
        .toString shouldBe
        """{"p":"2","c":5,"e":"1","v":1518007800000}"""
    }

    "convert json to items" in {
      HistoryItemConverter.fromJs(
        JsonObject.fromJson("""{"e":"3","v":1518007800000}""")
      ) shouldBe
        HistoryItem("3", now, None)
      HistoryItemConverter.fromJs(
        JsonObject.fromJson("""{"p":"2","c":5,"e":"3","v":1518007800000}""")
      ) shouldBe
        HistoryItem("3", now, Some("2"), Some(5))
    }
  }
}
