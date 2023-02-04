package ru.yandex.realty.clients.watch

import akka.http.scaladsl.model.HttpMethods.{GET, PATCH, POST}
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.model.user.{UserRef, UserRefGenerators}
import ru.yandex.realty.user.notification.DeliveryTypes
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.subscriptions.api.ApiModel

import scala.collection.JavaConverters._

/**
  * Specs on HTTP [[WatchClient]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class DefaultWatchClientSpec
  extends SpecBase
  with AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock
  with UserRefGenerators
  with WatchGenerators {

  private val client = new DefaultWatchClient(httpService)

  "DefaultWatchClient" should {
    "successfully getWatch" in {
      forAll(userRefGen) { owner =>
        forAll(watchGen(owner)) { watch =>
          expectGetWatch(owner)

          httpClient.respondWith(watch)

          client.getWatch(owner).futureValue should be(watch)
        }
      }
    }

    "handle 404 in getWatch" in {
      val owner = userRefGen.next

      expectGetWatch(owner)

      httpClient.respond(StatusCodes.NotFound)

      interceptCause[NoSuchElementException] {
        client.getWatch(owner).futureValue
      }
    }

    "handle 400 in getWatch" in {
      val owner = userRefGen.next

      expectGetWatch(owner)

      httpClient.respond(StatusCodes.BadRequest)

      interceptCause[IllegalArgumentException] {
        client.getWatch(owner).futureValue
      }
    }

    "successfully syncWatch" in {
      forAll(userRefGen, Gen.option(userRefGen), objectsGen, deliveriesGen) { (owner, optDonor, objects, deliveries) =>
        expectSyncWatch(owner, optDonor, objects, deliveries)

        val watch = watchGen(owner).next

        httpClient.respondWith(watch)

        client.syncWatch(owner, optDonor, objects, deliveries).futureValue should be(watch)
      }
    }

    "handle 404 in syncWatch" in {
      val owner = userRefGen.next
      val optDonor = Gen.option(userRefGen).next
      val objects = objectsGen.next
      val deliveries = deliveriesGen.next

      expectSyncWatch(owner, optDonor, objects, deliveries)

      httpClient.respond(StatusCodes.NotFound)

      interceptCause[NoSuchElementException] {
        client.syncWatch(owner, optDonor, objects, deliveries).futureValue
      }
    }

    "handle 400 in syncWatch" in {
      val owner = userRefGen.next
      val optDonor = Gen.option(userRefGen).next
      val objects = objectsGen.next
      val deliveries = deliveriesGen.next

      expectSyncWatch(owner, optDonor, objects, deliveries)

      httpClient.respond(StatusCodes.BadRequest)

      interceptCause[IllegalArgumentException] {
        client.syncWatch(owner, optDonor, objects, deliveries).futureValue
      }
    }

    "successfully setDeliveryEnabled" in {
      forAll(userRefGen, Gen.oneOf(DeliveryTypes.values.toSeq), BasicGenerators.bool) { (owner, delivery, enabled) =>
        expectSetDeliveryEnabled(owner, delivery, enabled)

        val watch = watchGen(owner).next

        httpClient.respondWith(watch)

        client.setDeliveryEnabled(owner, delivery, enabled).futureValue should be(watch)
      }
    }

    "handle 404 in setDeliveryEnabled" in {
      val owner = userRefGen.next
      val delivery = Gen.oneOf(DeliveryTypes.values.toSeq).next
      val enabled = BasicGenerators.bool.next
      expectSetDeliveryEnabled(owner, delivery, enabled)

      httpClient.respond(StatusCodes.NotFound)

      interceptCause[NoSuchElementException] {
        client.setDeliveryEnabled(owner, delivery, enabled).futureValue
      }
    }

    "handle 400 in setDeliveryEnabled" in {
      val owner = userRefGen.next
      val delivery = Gen.oneOf(DeliveryTypes.values.toSeq).next
      val enabled = BasicGenerators.bool.next
      expectSetDeliveryEnabled(owner, delivery, enabled)

      httpClient.respond(StatusCodes.BadRequest)

      interceptCause[IllegalArgumentException] {
        client.setDeliveryEnabled(owner, delivery, enabled).futureValue
      }
    }
  }

  private def expectGetWatch(owner: UserRef): Unit = {
    httpClient.expect(GET, s"/api/3.x/realty/user/${owner.toPlain}/watch")
  }

  private def expectSyncWatch(
    owner: UserRef,
    optDonor: Option[UserRef],
    objects: Set[String],
    deliveries: ApiModel.Deliveries
  ): Unit = {
    val syncParameters =
      ApiModel.SyncWatchParameters
        .newBuilder()
        .addAllObjects(objects.asJava)
        .setDeliveries(deliveries)
        .build()

    optDonor match {
      case Some(donor) =>
        httpClient.expect(POST, s"/api/3.x/realty/user/${owner.toPlain}/watch/sync?donor=${donor.toPlain}")
      case None =>
        httpClient.expect(POST, s"/api/3.x/realty/user/${owner.toPlain}/watch/sync")
    }

    httpClient.expectProto(syncParameters)
  }

  private def expectSetDeliveryEnabled(owner: UserRef, delivery: DeliveryTypes.Value, enabled: Boolean): Unit = {
    httpClient.expect(PATCH, s"/api/3.x/realty/user/${owner.toPlain}/watch/delivery?type=$delivery&enabled=$enabled")
  }

}
