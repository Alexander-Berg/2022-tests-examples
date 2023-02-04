package ru.yandex.vertis.personal.model.history

import org.joda.time.DateTime
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import ru.yandex.vertis.personal.model.UserRef

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 31.08.16
  */
class HistoryCollectionSpec extends WordSpec {

  "HistoryCollection" should {
    val user = UserRef("user")

    val now = DateTime.now()
    val item1 = HistoryItem("i1", now, None)
    val item2 = HistoryItem("i2", now.minusMinutes(5), None)
    val item3 = HistoryItem("i3", now.minusMinutes(10), None)

    val collection = HistoryCollection(user, Seq(item1, item2, item3))

    "add item increase addCount value" in {
      collection.add(item1, 100) shouldBe HistoryCollection(
        user,
        Seq(
          item1.copy(addCount = Some(2)),
          item2,
          item3
        )
      )

      collection
        .add(item2, 100)
        .add(item2, 100)
        .add(item2, 100) shouldBe HistoryCollection(
        user,
        Seq(
          item1,
          item2.copy(addCount = Some(4)),
          item3
        )
      )
    }

    "don't erase addCount value on update" in {
      val updatedItem = item1.copy(payload = Some("123"))
      collection.add(item1, 100).update(updatedItem) shouldBe HistoryCollection(
        user,
        Seq(
          updatedItem.copy(addCount = Some(2)),
          item2,
          item3
        )
      )
    }

    "update item" in {
      val updated = HistoryItem(item2.id, DateTime.now, Some("123"))

      collection.update(updated) shouldBe HistoryCollection(
        user,
        Seq(
          item1,
          HistoryItem(item2.id, item2.visitTime, updated.payload),
          item3
        )
      )
    }

    "delete item" in {
      collection.delete("i1") shouldBe HistoryCollection(
        user,
        Seq(item2, item3)
      )
    }
  }
}
