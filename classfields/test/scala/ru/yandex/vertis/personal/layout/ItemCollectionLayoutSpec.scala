package ru.yandex.vertis.personal.layout

import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import org.joda.time.DateTime
import ru.yandex.vertis.personal.couchbase.{DocumentLayout, DocumentPrefixes, ItemCollectionLayout}
import ru.yandex.vertis.personal.history.HistoryItemConverter
import ru.yandex.vertis.personal.model.history.{HistoryCollection, HistoryItem}
import ru.yandex.vertis.personal.model.{SimpleUserRef, UserRef}
import ru.yandex.vertis.personal.util.BaseSpec

import scala.concurrent.duration.DurationInt

class ItemCollectionLayoutSpec extends BaseSpec {

  trait TestContext {

    val user: UserRef = SimpleUserRef("user")

    val prefix = DocumentPrefixes.History

    val id = prefix.toString + user.toPlain

    val converter = HistoryItemConverter

    val layout = new ItemCollectionLayout(
      HistoryItemConverter,
      HistoryCollection.apply,
      DocumentPrefixes.History
    )

    val values = Seq(
      HistoryItem("0", DateTime.now(), Some("a")),
      HistoryItem("1", DateTime.now(), Some("b"), Some(1)),
      HistoryItem("2", DateTime.now(), Some("c"), Some(3))
    )

    val emptyProperty = HistoryCollection(user, Seq.empty)

    val property = HistoryCollection(user, values)

    val ttl = 1.second

    val emptyJson = JsonObject
      .create()
      .put("u", user.toPlain)
      .put("i", JsonArray.create())

    val emptyDocument = JsonDocument.create(
      id,
      DocumentLayout.getExpireSinceNow(ttl),
      emptyJson
    )

    val json = {
      val array = values.foldLeft(JsonArray.create()) {
        case (j, i) =>
          j.add(converter.toJs(i))
      }
      JsonObject
        .create()
        .put("u", user.toPlain)
        .put("i", array)
    }

    val document = JsonDocument.create(
      id,
      DocumentLayout.getExpireSinceNow(ttl),
      json
    )

  }

  "ItemCollectionLayout" should {
    "convert to empty property" when {
      "document not exists" in new TestContext {
        layout.documentToProperty(user, None) shouldBe emptyProperty
      }
      "pass document without values" in new TestContext {
        layout.documentToProperty(user, Some(emptyDocument)) shouldBe emptyProperty
      }
    }
    "convert to non empty property" when {
      "pass non empty document" in new TestContext {
        layout.documentToProperty(user, Some(document)) shouldBe property
      }
    }
    "convert to empty document" when {
      "pass empty property" in new TestContext {
        layout.propertyToDocument(emptyProperty, ttl) shouldBe emptyDocument
      }
    }
    "convert to non empty document" when {
      "pass non empty property" in new TestContext {
        layout.propertyToDocument(property, ttl) shouldBe document
      }
    }
    "convert user to id" in new TestContext {
      layout.id(user) shouldBe id
    }
  }

}
