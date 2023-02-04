package ru.yandex.vertis.subscriptions.service

import java.util.NoSuchElementException
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.subscriptions.{SlowAsyncSpec, SpecBase}
import ru.yandex.vertis.subscriptions.model.ModelGenerators._
import ru.yandex.vertis.subscriptions.model.delivery.{Deliveries, DeliveryTypes}
import ru.yandex.vertis.subscriptions.model.{ModelGenerators, Watch, WatchPatch}

/**
  * Spec base on [[WatchService]].
  *
  * @author dimas
  */
trait WatchServiceSpecBase extends SpecBase with SlowAsyncSpec {

  def watchService: WatchService

  "WatchService" should {
    "initially create watch" in {
      create()
    }

    "update watched object and retain delivery intact for exists watch" in {
      val watch = create()

      val patch = ModelGenerators.watchPatch.next
      val deliveries = ModelGenerators.deliveries.next
      watchService.update(watch.owner, patch, deliveries).futureValue should be(watch.apply(patch))
    }

    "delete watched object" in {
      val watch = create()
      val toDelete = watch.objects.head
      watchService.deleteObject(watch.owner, toDelete).futureValue should be(watch - toDelete)
    }

    "delete watch in case of absence of watches objects and deliveries" in {
      val watch = create()

      val patch = WatchPatch.removing(watch.objects)
      val deliveries = Deliveries.Empty

      watchService.update(watch.owner, patch, deliveries).futureValue should be(watch.apply(patch))

      watchService
        .deleteDeliveries(watch.owner, DeliveryTypes.All)
        .futureValue should be(watch.apply(patch).copy(deliveries = Deliveries.Empty))

      intercept[NoSuchElementException] {
        cause(watchService.get(watch.owner).futureValue)
      }
    }

    "preserve watch in case of absence of watches objects but with deliveries" in {
      val watch = create()

      val patch = WatchPatch.removing(watch.objects)
      val deliveries = ModelGenerators.notEmptyDeliveries.next
      watchService.update(watch.owner, patch, deliveries).futureValue should be(watch.apply(patch))

      watchService.get(watch.owner).futureValue should be(watch.apply(patch))
    }

    "update deliveries" in {
      val watch = create()
      val deliveries = ModelGenerators.deliveries.next
      watchService.updateDelivery(watch.owner, deliveries).futureValue should be(watch.copy(deliveries = deliveries))
    }

    "update particular delivery" in {
      val watch = create()
      val delivery = ModelGenerators.deliveryType.next
      val enabled = BasicGenerators.bool.next
      watchService.updateDelivery(watch.owner, delivery, enabled).futureValue should be(
        watch.updateDelivery(delivery, enabled)
      )
    }

    "delete email delivery" in {
      testDeleteDelivery(Set(DeliveryTypes.Email))
    }

    "delete push delivery" in {
      testDeleteDelivery(Set(DeliveryTypes.Push))
    }

    "delete multiple deliveries" in {
      testDeleteDelivery(Set(DeliveryTypes.Email, DeliveryTypes.Push))
    }

    "delete watch" in {
      val watch = create()
      watchService.delete(watch.owner).futureValue should be(())
      intercept[NoSuchElementException] {
        cause(watchService.get(watch.owner).futureValue)
      }
    }

    "move watches" in {
      val watch = create()

      val owner = ModelGenerators.owner.next
      intercept[NoSuchElementException] {
        cause(watchService.get(owner).futureValue)
      }
      val deliveries = ModelGenerators.deliveries.next

      watchService.move(watch.owner, owner, deliveries).futureValue should be(())

      intercept[NoSuchElementException] {
        cause(watchService.get(watch.owner).futureValue)
      }

      watchService.get(owner).futureValue should be(watch.copy(owner = owner, deliveries = deliveries))
    }

    "sync watches with donor" in {
      val watch = create()

      val owner = ModelGenerators.owner.next
      intercept[NoSuchElementException] {
        cause(watchService.get(owner).futureValue)
      }
      val objects = BasicGenerators.set(1, 10, BasicGenerators.readableString(5, 5)).next
      val deliveries = ModelGenerators.deliveries.next

      watchService.sync(owner, objects, deliveries, Some(watch.owner)).futureValue should be(
        Watch(owner, objects, deliveries)
      )

      intercept[NoSuchElementException] {
        cause(watchService.get(watch.owner).futureValue)
      }

      watchService.get(owner).futureValue should be(Watch(owner, objects, deliveries))
    }

    "sync watches preserving deliveries" in {
      val watch = create()

      val objects = BasicGenerators.set(1, 10, BasicGenerators.readableString(5, 5)).next
      val deliveries = ModelGenerators.deliveries.next

      watchService.sync(watch.owner, objects, deliveries, None).futureValue should be(watch.copy(objects = objects))

      watchService.get(watch.owner).futureValue should be(watch.copy(objects = objects))
    }

    "remove watch while sync with empty objects and empty delivery" in {
      val owner = ModelGenerators.owner.next

      val deliveries = Deliveries.Empty

      watchService.sync(owner, Set.empty, deliveries, None).futureValue should be(Watch(owner, Set.empty, deliveries))

      intercept[NoSuchElementException] {
        cause(watchService.get(owner).futureValue)
      }
    }

  }

  private def create(): Watch = {
    val owner = ModelGenerators.owner.next
    val patch = ModelGenerators.watchPatch.next
    val deliveries = ModelGenerators.notEmptyDeliveries.next

    val watch = watchService
      .update(owner, patch, deliveries)
      .futureValue
    watch should be(Watch(owner, patch.add, deliveries))
    watch
  }

  private def testDeleteDelivery(deliveryTypes: Set[DeliveryTypes.Value]): Unit = {
    val watch = create()
    val withoutDeliveries = watch
      .copy(deliveries = watch.deliveries.delete(deliveryTypes))
    watchService.deleteDeliveries(watch.owner, deliveryTypes).futureValue should be(withoutDeliveries)
  }

}
