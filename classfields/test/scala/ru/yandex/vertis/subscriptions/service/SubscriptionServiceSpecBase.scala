package ru.yandex.vertis.subscriptions.service

import java.util.NoSuchElementException

import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.protobuf._
import ru.yandex.vertis.subscriptions.model.LegacyProtoFormats._
import ru.yandex.vertis.subscriptions.model.delivery.{Deliveries, DeliveryTypes}
import ru.yandex.vertis.subscriptions.model.owner.{Owner, OwnerGenerators}
import ru.yandex.vertis.subscriptions.model.{Identity, ModelGenerators, Subscription}
import ru.yandex.vertis.subscriptions.{SlowAsyncSpec, SpecBase}

/**
  * Specs on [[SubscriptionService]].
  *
  * @author dimas
  */
trait SubscriptionServiceSpecBase extends SpecBase with SlowAsyncSpec {

  def service: SubscriptionService

  "SubscriptionService" should {
    "create and get subscription by id" in {
      val created = create()
      service.get(created.owner, created.id).futureValue should be(created)
    }

    "return raw subscription in getRaw" in {
      val created = create()
      val createdProto = asProto(created)
      val loadedSub = service.getRaw(created.owner, created.id).futureValue
      //we don't replace user with linked one
      val fixedSub = loadedSub.toBuilder.setUser(createdProto.getUser).build()
      fixedSub should be(createdProto)
    }

    "create and get subscription by request" in {
      val created = create()
      service.get(created.owner, Identity.Indirect(created)).futureValue should be(created)
    }

    "create and list single subscription" in {
      val created = create()
      service.list(created.owner).futureValue should be(Seq(created))
    }

    "create and list many subscriptions" in {
      val created = createMany(5)
      service.list(created.head.owner).futureValue.toList should be(created.reverse)
    }

    "update subscription delivery by id" in {
      val created = create()
      val deliveries = ModelGenerators.notEmptyDeliveries.next.withoutSensitiveTargets
      val updated = service.updateDelivery(created.owner, created.id, deliveries).futureValue
      updated should be(created.copy(deliveries = deliveries))
    }

    "update subscription delivery by request" in {
      val created = create()
      val deliveries = ModelGenerators.notEmptyDeliveries.next.withoutSensitiveTargets
      val updated = service.updateDelivery(created.owner, Identity.Indirect(created), deliveries).futureValue
      updated should be(created.copy(deliveries = deliveries))
    }

    "delete entire subscription in case of empty delivery" in {
      val created = create()
      val updated = service.updateDelivery(created.owner, created.id, Deliveries.Empty).futureValue
      updated should be(created.copy(deliveries = Deliveries.Empty))
      intercept[NoSuchElementException] {
        cause(service.get(created.owner, created.id).futureValue)
      }
    }

    "delete subscription email delivery by id" in {
      testDeleteDelivery(DeliveryTypes.Email, directIdentity = true)
    }

    "delete subscription email delivery by request" in {
      testDeleteDelivery(DeliveryTypes.Email, directIdentity = false)
    }

    "delete subscription push delivery by id" in {
      testDeleteDelivery(DeliveryTypes.Push, directIdentity = true)
    }

    "delete subscription push delivery by request" in {
      testDeleteDelivery(DeliveryTypes.Push, directIdentity = false)
    }

    "delete subscription in case of last delivery deletion" in {
      val subscription = create()
      val deliveries = ModelGenerators.fullDeliveries.next.withoutSensitiveTargets
      service.updateDelivery(subscription.owner, subscription.id, deliveries).futureValue
      service.deleteDelivery(subscription.owner, subscription.id, DeliveryTypes.Email).futureValue
      service.deleteDelivery(subscription.owner, subscription.id, DeliveryTypes.Push).futureValue
      intercept[NoSuchElementException] {
        cause(service.get(subscription.owner, subscription.id).futureValue)
      }
    }

    "delete subscription by identity" in {
      val subscription = create()
      service.delete(subscription.owner, subscription.id).futureValue
      intercept[NoSuchElementException] {
        cause(service.get(subscription.owner, subscription.id).futureValue)
      }
    }

    "move subscriptions from one owner to another" in {
      val source = createMany(5)
      val sourceOwner = source.head.owner
      val dest = createMany(2)
      val destOwner = dest.head.owner

      service.move(sourceOwner, destOwner).futureValue

      service.list(sourceOwner).futureValue should be(Seq.empty)

      val merged = service.list(destOwner).futureValue
      merged.size should be(source.size + dest.size)
      merged.foreach { s =>
        s.owner should be(destOwner)
      }
    }
  }

  private def create(): Subscription = {
    val owner = OwnerGenerators.owner.next
    create(owner)
  }

  private def createMany(n: Int): List[Subscription] = {
    require(n >= 1, "Unable to create < 1 subscription")
    val head = create()
    val tail = (1 until n).map { _ =>
      create(head.owner)
    }
    head :: tail.toList
  }

  private def create(owner: Owner): Subscription = {
    val sourceBase = ModelGenerators.createSubscriptionParameters.next
    val source = sourceBase.copy(deliveries = sourceBase.deliveries.withoutSensitiveTargets)
    val created = service.create(owner, source).futureValue
    created.owner should be(owner)
    created
  }

  private def testDeleteDelivery(delivery: DeliveryTypes.Value, directIdentity: Boolean): Unit = {
    val subscription = create()
    val id = if (directIdentity) Identity.Direct(subscription) else Identity.Indirect(subscription)
    val deliveries = ModelGenerators.fullDeliveries.next.withoutSensitiveTargets
    val withFullDelivery = service.updateDelivery(subscription.owner, id, deliveries).futureValue
    val withoutDelivery = service.deleteDelivery(subscription.owner, id, delivery).futureValue
    val expected = withFullDelivery.copy(deliveries = withFullDelivery.deliveries.delete(delivery))
    withoutDelivery should be(expected)
    service.get(subscription.owner, id).futureValue should be(expected)
  }

}
