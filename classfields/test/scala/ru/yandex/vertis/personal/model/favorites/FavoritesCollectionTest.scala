package ru.yandex.vertis.personal.model.favorites

import org.joda.time.DateTime
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import ru.yandex.vertis.personal.model.UserRef
import ru.yandex.vertis.personal.generators.Producer
import ru.yandex.vertis.personal.model.ModelGenerators._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 15.08.16
  */
class FavoritesCollectionTest extends WordSpec {

  "FavoritesCollection" should {

    "upsert item and keep creation date" in {
      val userRef = UserRefGen.next
      val itemId = "i1"

      val created = DateTime.parse("2016-08-13T12:00:00Z")
      val now = DateTime.now

      val item = FavoriteItem(itemId, created, created, None)

      val itemToUpsert = FavoriteItem(itemId, now, now, None)

      val collection = FavoritesCollection(userRef, Seq(item))

      collection.upsert(itemToUpsert) shouldBe FavoritesCollection(
        userRef,
        Seq(FavoriteItem(itemId, created, now, None))
      )
    }

    "upsert multiple items" in {
      val userRef = UserRefGen.next
      val items = FavoriteItemGen.next(10).toSeq
      val collection = FavoritesCollection(userRef, Seq.empty)
      collection.upsert(items).items should be(items)
    }

    "delete multiple items" in {
      val userRef = UserRefGen.next
      val items = FavoriteItemGen.next(10).toSeq
      val collection = FavoritesCollection(userRef, items)
      collection.delete(items.take(2).map(_.id)).items should be(items.drop(2))
    }

    "concatenate two collections" in {
      val userRef = UserRef("user")

      val item1 = FavoriteItem("item1", DateTime.now, DateTime.now, None)
      val item2 = FavoriteItem("item2", DateTime.now, DateTime.now, None)

      val collection1 = FavoritesCollection(userRef, Seq(item1))

      val collection2 = FavoritesCollection(userRef, Seq(item2))

      (collection1 ++ collection2) shouldBe FavoritesCollection(
        userRef,
        Seq(item1, item2)
      )
    }
  }

}
