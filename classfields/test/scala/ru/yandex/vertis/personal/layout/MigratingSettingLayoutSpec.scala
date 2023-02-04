package ru.yandex.vertis.personal.layout

import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.JsonObject
import org.mockito.Mockito
import ru.yandex.vertis.personal.settings.couchbase.MigratingSettingLayout
import ru.yandex.vertis.personal.util.BaseSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.personal.couchbase.{DocumentPrefixes, MultiDomainCollectionLayout}
import ru.yandex.vertis.personal.model.{Domains, SimpleUserRef, UserRef}
import ru.yandex.vertis.personal.model.generic.{GenericItem, GenericMultiDomainCollection}
import ru.yandex.vertis.personal.model.settings.SettingsPayload

import scala.concurrent.duration.DurationInt

class MigratingSettingLayoutSpec extends BaseSpec with MockitoSupport {

  trait TestContext {

    val user: UserRef = SimpleUserRef("user")

    val map = Map("foo" -> "bar", "baz" -> "fast")

    val emptyProperty =
      GenericMultiDomainCollection.empty[SettingsPayload](user)

    val emptyDocument = JsonDocument.create("0", 2, JsonObject.create())

    val payloads: Seq[GenericItem[SettingsPayload]] = map.map {
      case (key, value) =>
        GenericItem(key, SettingsPayload(value))
    }.toSeq

    val collection =
      GenericMultiDomainCollection(user, 1, Map(Domains.Settings -> payloads))

    val newLayoutMock = {
      val m = mock[MultiDomainCollectionLayout[
        GenericItem[SettingsPayload],
        GenericMultiDomainCollection[SettingsPayload]
      ]]

      when(m.documentToProperty(?, ?)).thenReturn(emptyProperty)
      when(m.propertyToDocument(?, ?)).thenReturn(emptyDocument)

      m
    }

    val laylout = new MigratingSettingLayout(newLayoutMock)

  }

  "MigratingSettingLayout" should {
    "convert to empty property" when {
      "document not exists" in new TestContext {
        laylout.documentToProperty(user, None) shouldBe emptyProperty
      }
    }
    "parse document by new layout" when {
      "document contain version" in new TestContext {
        val json = JsonObject.create().put("version", "1")
        val document = JsonDocument.create("0", json)
        laylout.documentToProperty(user, Some(document)) shouldBe emptyProperty
        Mockito.verify(newLayoutMock).documentToProperty(?, ?)
      }
    }
    "parse document as legacy" when {
      "document doesn't contain version" in new TestContext {
        val values = JsonObject.create()
        map.foreach {
          case (key, value) =>
            values.put(key, value)
        }
        val json = JsonObject.create().put("v", values)
        val document = JsonDocument.create("0", json)
        laylout.documentToProperty(user, Some(document)) shouldBe collection
        Mockito.verify(newLayoutMock, Mockito.never()).documentToProperty(?, ?)
      }
    }
    "convert to property by new layout" in new TestContext {
      laylout.propertyToDocument(collection, 1.second) shouldBe emptyDocument
      Mockito.verify(newLayoutMock).propertyToDocument(?, ?)
    }
    "convert user to id" in new TestContext {
      val expected = DocumentPrefixes.Settings.toString + user.toPlain
      laylout.id(user) shouldBe expected
    }
  }

}
