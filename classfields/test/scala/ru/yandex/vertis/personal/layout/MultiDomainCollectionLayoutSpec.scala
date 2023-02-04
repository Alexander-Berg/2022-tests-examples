package ru.yandex.vertis.personal.layout

import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import org.joda.time.DateTime
import ru.yandex.vertis.personal.couchbase.{DocumentLayout, DocumentPrefixes, MultiDomainCollectionLayout}
import ru.yandex.vertis.personal.favorites.couchbase.FavoriteItemConverter
import ru.yandex.vertis.personal.model.{Domains, SimpleUserRef, UserRef}
import ru.yandex.vertis.personal.model.favorites.{FavoriteItem, MultiDomainFavoritesCollection}
import ru.yandex.vertis.personal.util.BaseSpec

import scala.concurrent.duration.DurationInt

class MultiDomainCollectionLayoutSpec extends BaseSpec {

  trait TestContext {

    val user: UserRef = SimpleUserRef("user")

    val prefix = DocumentPrefixes.Favorites

    val id = prefix.toString + user.toPlain

    val converter = FavoriteItemConverter

    val layout = new MultiDomainCollectionLayout(
      converter,
      MultiDomainFavoritesCollection.apply,
      prefix
    )

    val version = 1

    val emptyProperty = MultiDomainFavoritesCollection(user, version, Map.empty)

    val valuesMap = Map(
      Domains.Searches -> Seq(
        FavoriteItem("0", DateTime.now(), DateTime.now(), Some("baz"))
      ),
      Domains.Cards -> Seq(
        FavoriteItem("1", DateTime.now(), DateTime.now(), Some("foo"))
      ),
      Domains.Credits -> Seq(
        FavoriteItem("2", DateTime.now(), DateTime.now(), Some("foozz"))
      )
    )

    val property = MultiDomainFavoritesCollection(user, version, valuesMap)

    val ttl = 1.second

    val emptyJson = JsonObject
      .create()
      .put("u", user.toPlain)
      .put("version", version)

    val emptyDocument = JsonDocument.create(
      id,
      DocumentLayout.getExpireSinceNow(ttl),
      emptyJson
    )

    val document = {
      val json = JsonObject
        .create()
        .put("u", user.toPlain)
        .put("version", version)

      property.items.foldLeft(json) {
        case (j, (domain, items)) =>
          val array = JsonArray.create()
          items.map(converter.toJs).foreach(array.add)
          j.put(domain.toString, array)
      }

      JsonDocument.create(id, DocumentLayout.getExpireSinceNow(ttl), json)
    }

  }

  "SettingsLayout" should {
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
