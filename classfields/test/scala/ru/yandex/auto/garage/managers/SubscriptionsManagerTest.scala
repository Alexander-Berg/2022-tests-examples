package ru.yandex.auto.garage.managers

import auto.carfax.common.clients.public_api.PublicApiLenta
import auto.carfax.common.utils.tracing.Traced
import junit.framework.TestCase.assertEquals
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.lenta.ApiModel.Subscription
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.VehicleInfo.AutoruCatalogData
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.{GarageCard, VehicleInfo}
import ru.yandex.auto.vin.decoder.model.UserRef
import ru.yandex.auto.vin.decoder.proto.TtxSchema
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._

class SubscriptionsManagerTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfterEach {

  private val publicApiLentaMock = mock[PublicApiLenta]
  implicit val t: Traced = Traced.empty

  override def beforeEach(): Unit = {
    reset(publicApiLentaMock)
  }

  private val user = UserRef.anon("123")
  private val manager = new SubscriptionsManager(publicApiLentaMock)

  private val cardWithGen = GarageCard
    .newBuilder()
    .setVehicleInfo(
      VehicleInfo
        .newBuilder()
        .setTtx(
          TtxSchema.CommonTtx
            .newBuilder()
            .setMark("VOLKSWAGEN")
            .setModel("TERAMONT")
        )
        .setCatalog(AutoruCatalogData.newBuilder().setSuperGenId(22970558L))
    )
    .build()

  private val cardWithModel = GarageCard
    .newBuilder()
    .setVehicleInfo(
      VehicleInfo
        .newBuilder()
        .setTtx(
          TtxSchema.CommonTtx
            .newBuilder()
            .setMark("BMW")
            .setModel("5ER")
        )
    )
    .build()

  private val cardWithMark = GarageCard
    .newBuilder()
    .setVehicleInfo(
      VehicleInfo
        .newBuilder()
        .setTtx(
          TtxSchema.CommonTtx
            .newBuilder()
            .setMark("HYUNDAI")
        )
    )
    .build()

  "SubscriptionsManager" should {

    "add subscription tags" when {

      "card with mark, model, gen" in {
        when(publicApiLentaMock.addSubscriptionTags(?, ?)(?)).thenAnswer { a =>
          val subscription = a.getArgument[Subscription](1)
          assertEquals(
            Set("VOLKSWAGEN", "VOLKSWAGEN#TERAMONT", "VOLKSWAGEN#TERAMONT#22970558"),
            subscription.getIncludeTagsList.asScala.toSet
          )
          Future.successful(List.empty)
        }
        Await.result(manager.subscribe(0, user, cardWithGen), 2.seconds)
        verify(publicApiLentaMock, times(1)).addSubscriptionTags(?, ?)(?)
      }

      "card with mark, model" in {
        when(publicApiLentaMock.addSubscriptionTags(?, ?)(?)).thenAnswer { a =>
          val subscription = a.getArgument[Subscription](1)
          assertEquals(Set("BMW", "BMW#5ER"), subscription.getIncludeTagsList.asScala.toSet)
          Future.successful(List.empty)
        }
        Await.result(manager.subscribe(0, user, cardWithModel), 2.seconds)
        verify(publicApiLentaMock, times(1)).addSubscriptionTags(?, ?)(?)
      }

      "card with mark" in {
        when(publicApiLentaMock.addSubscriptionTags(?, ?)(?)).thenAnswer { a =>
          val subscription = a.getArgument[Subscription](1)
          assertEquals(Set("HYUNDAI"), subscription.getIncludeTagsList.asScala.toSet)
          Future.successful(List.empty)
        }
        Await.result(manager.subscribe(0, user, cardWithMark), 2.seconds)
        verify(publicApiLentaMock, times(1)).addSubscriptionTags(?, ?)(?)
      }

    }

    "not add subscription tags " when {

      "card without mark, model, gen" in {
        val card = GarageCard.newBuilder().build()
        Await.result(manager.subscribe(0, user, card), 2.seconds)
        verify(publicApiLentaMock, never()).addSubscriptionTags(?, ?)(?)
      }

    }

    "delete subscription tags" when {

      "card with mark, model, gen" in {
        when(publicApiLentaMock.deleteSubscriptionTags(?, ?)(?)).thenAnswer { a =>
          val subscription = a.getArgument[Subscription](1)
          assertEquals(
            Set("VOLKSWAGEN", "VOLKSWAGEN#TERAMONT", "VOLKSWAGEN#TERAMONT#22970558"),
            subscription.getIncludeTagsList.asScala.toSet
          )
          Future.successful(List.empty)
        }
        Await.result(manager.unsubscribe(1, user, cardWithGen), 2.seconds)
        verify(publicApiLentaMock, times(1)).deleteSubscriptionTags(?, ?)(?)
      }

      "card with mark, model" in {
        when(publicApiLentaMock.deleteSubscriptionTags(?, ?)(?)).thenAnswer { a =>
          val subscription = a.getArgument[Subscription](1)
          assertEquals(Set("BMW", "BMW#5ER"), subscription.getIncludeTagsList.asScala.toSet)
          Future.successful(List.empty)
        }
        Await.result(manager.unsubscribe(1, user, cardWithModel), 2.seconds)
        verify(publicApiLentaMock, times(1)).deleteSubscriptionTags(?, ?)(?)
      }

      "card with mark" in {
        when(publicApiLentaMock.deleteSubscriptionTags(?, ?)(?)).thenAnswer { a =>
          val subscription = a.getArgument[Subscription](1)
          assertEquals(Set("HYUNDAI"), subscription.getIncludeTagsList.asScala.toSet)
          Future.successful(List.empty)
        }
        Await.result(manager.unsubscribe(1, user, cardWithMark), 2.seconds)
        verify(publicApiLentaMock, times(1)).deleteSubscriptionTags(?, ?)(?)
      }

    }

    "not delete subscription tags " when {

      "card without mark, model, gen" in {
        val card = GarageCard.newBuilder().build()
        Await.result(manager.unsubscribe(0, user, card), 2.seconds)
        verify(publicApiLentaMock, never()).deleteSubscriptionTags(?, ?)(?)
      }

    }

    "resubscribe" when {

      "cards with mark, model, gen" in {
        when(publicApiLentaMock.setSubscription(?, ?)(?)).thenAnswer { a =>
          val subscription = a.getArgument[Subscription](1)
          assertEquals(
            Set("VOLKSWAGEN", "VOLKSWAGEN#TERAMONT", "VOLKSWAGEN#TERAMONT#22970558", "BMW", "BMW#5ER"),
            subscription.getIncludeTagsList.asScala.toSet
          )
          Future.successful(List.empty)
        }
        Await.result(manager.resubscribe(user, Seq(cardWithGen, cardWithModel)), 2.seconds)
        verify(publicApiLentaMock, times(1)).setSubscription(?, ?)(?)
      }

    }

  }
}
