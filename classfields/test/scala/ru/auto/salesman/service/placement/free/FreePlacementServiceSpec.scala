package ru.auto.salesman.service.placement.free

import com.google.protobuf.Duration
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.api.OffersByParamsFilterOuterClass.OffersByParamsFilter
import ru.auto.salesman.client.broker.BrokerClient
import ru.auto.salesman.client.{PromocoderClient, VosClient}
import ru.auto.salesman.environment.{now, RichDateTime}
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model.broker.MessageId
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{
  AutoruDealer,
  FeatureConstraint,
  FeatureCount,
  FeatureInstance,
  FeatureOrigin,
  FeaturePayload,
  FeatureTypes,
  FeatureUnits,
  ProductId,
  PromocoderUser,
  Slave
}
import ru.auto.salesman.util.offer._
import ru.auto.salesman.offers.FreePlacement.FreePlacementRequest
import ru.auto.salesman.offers.FreePlacementLogOuterClass
import ru.auto.salesman.service.placement.free.errors.OfferNotFoundException
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._

import scala.reflect.ClassTag

class FreePlacementServiceSpec extends BaseSpec with OfferModelGenerators {

  val vosClient = mock[VosClient]
  val promocoderClient = mock[PromocoderClient]
  val brokerClient = mock[BrokerClient]

  val service = new FreePlacementServiceImpl(vosClient, promocoderClient, brokerClient)

  val stubFeature = FeatureInstance(
    id = "id",
    origin = FeatureOrigin("origin"),
    ProductId.alias(Placement),
    user = "id",
    count = FeatureCount(20L, FeatureUnits.Items),
    createTs = now(),
    deadline = now().plusDays(2),
    FeaturePayload(
      FeatureUnits.Items,
      FeatureTypes.Promocode,
      constraint = Some(FeatureConstraint(OfferIdentity("11-id")))
    )
  )

  "FreePlacementService" should {

    "issue promocode for user and offer id from request" in {
      val offer = offerGen().next

      val request = FreePlacementRequest
        .newBuilder()
        .setOfferId(offer.getId)
        .setDuration(20)
        .build()

      (vosClient.getOptOffer _)
        .expects(offer.id, Slave)
        .returningZ(Some(offer))

      (promocoderClient.getFeature _)
        .expects(*)
        .returningZ(None)

      (promocoderClient.createFeatures _)
        .expects(*, *, *)
        .returningZ(List(stubFeature))

      (brokerClient
        .send(
          _: MessageId,
          _: FreePlacementLogOuterClass.FreePlacementLog
        )(_: ClassTag[FreePlacementLogOuterClass.FreePlacementLog]))
        .expects(*, *, *)
        .returningZ(())

      service
        .allowFreePlacement(request)
        .success
        .value
    }

    "issue promocode for latest offer if request is made with vin" in {
      val user1 = dealerRefGen.next
      val user2 = dealerRefGen.next

      val olderOffer = offerGen().next.toBuilder.setUserRef(user1).build()
      val newerOffer = olderOffer.toBuilder
        .setUserRef(user2)
        .setCreated(
          Timestamps.add(
            olderOffer.getCreated,
            Duration.newBuilder().setSeconds(1).setNanos(1).build()
          )
        )
        .build()
      val vin = olderOffer.vin.getOrElse("vin")

      val request = FreePlacementRequest
        .newBuilder()
        .setVin(vin)
        .setDuration(20)
        .build()

      (vosClient.getByParams _)
        .expects(OffersByParamsFilter.newBuilder().addVins(vin).build())
        .returningZ(List(olderOffer, newerOffer))

      (promocoderClient.getFeature _)
        .expects(*)
        .returningZ(None)

      (promocoderClient.createFeatures _)
        .expects(*, PromocoderUser(AutoruDealer(user2)), *)
        .returningZ(List(stubFeature))

      (brokerClient
        .send[FreePlacementLogOuterClass.FreePlacementLog](
          _: MessageId,
          _: FreePlacementLogOuterClass.FreePlacementLog
        )(_: ClassTag[FreePlacementLogOuterClass.FreePlacementLog]))
        .expects(*, *, *)
        .returningZ(())

      service
        .allowFreePlacement(request)
        .success
        .value
    }

    "fail if there is no offer" in {
      val offer = offerGen().next

      val request1 = FreePlacementRequest
        .newBuilder()
        .setOfferId(offer.getId)
        .setDuration(20)
        .build()

      val request2 = FreePlacementRequest
        .newBuilder()
        .setVin(offer.vin.getOrElse("vin"))
        .setDuration(20)
        .build()

      val request3 = FreePlacementRequest
        .newBuilder()
        .setDuration(20)
        .build()

      (vosClient.getOptOffer _)
        .expects(offer.id, Slave)
        .returningZ(None)

      (vosClient.getByParams _)
        .expects(*)
        .returningZ(List.empty)

      val test = (request: FreePlacementRequest) =>
        service
          .allowFreePlacement(request)
          .failure
          .exception shouldBe OfferNotFoundException

      test(request1)
      test(request2)
      test(request3)
    }

    "create feature if there is no feature with same id" in {
      val offer = offerGen().next
      val batchId = s"free_placement_${offer.getId}"
      val user = PromocoderUser(AutoruDealer(offer.getUserRef))
      val featureId = s"placement:api_free_placement_${offer.getId}:u_${user.value}"

      val request = FreePlacementRequest
        .newBuilder()
        .setOfferId(offer.getId)
        .setDuration(20)
        .build()

      (vosClient.getOptOffer _)
        .expects(*, *)
        .returningZ(Some(offer))

      (promocoderClient.getFeature _)
        .expects(featureId)
        .returningZ(None)

      (promocoderClient.createFeatures _)
        .expects(batchId, user, *)
        .returningZ(List(stubFeature))

      (brokerClient
        .send(
          _: MessageId,
          _: FreePlacementLogOuterClass.FreePlacementLog
        )(_: ClassTag[FreePlacementLogOuterClass.FreePlacementLog]))
        .expects(*, *, *)
        .returningZ(())

      service
        .allowFreePlacement(request)
        .success
        .value
    }

    "not create feature if feature with same id is already exist" in {
      val offer = offerGen().next
      val batchId = s"free_placement_${offer.getId}"
      val user = PromocoderUser(AutoruDealer(offer.getUserRef))
      val featureId = s"placement:api_free_placement_${offer.getId}:u_${user.value}"

      val request = FreePlacementRequest
        .newBuilder()
        .setOfferId(offer.getId)
        .setDuration(20)
        .build()

      (vosClient.getOptOffer _)
        .expects(*, *)
        .returningZ(Some(offer))

      (promocoderClient.getFeature _)
        .expects(featureId)
        .returningZ(Some(stubFeature))

      (brokerClient
        .send(
          _: MessageId,
          _: FreePlacementLogOuterClass.FreePlacementLog
        )(_: ClassTag[FreePlacementLogOuterClass.FreePlacementLog]))
        .expects(*, *, *)
        .never()

      (promocoderClient.createFeatures _)
        .expects(batchId, user, *)
        .never()

      service
        .allowFreePlacement(request)
        .success
        .value
    }

    "send proper event to broker" in {
      val now = DateTime.now()
      val offer = offerGen().next

      val request = FreePlacementRequest
        .newBuilder()
        .setOfferId(offer.getId)
        .setDuration(20)
        .build()

      val log = FreePlacementLogOuterClass.FreePlacementLog
        .newBuilder()
        .setOfferId(offer.getId)
        .setUserId(offer.getUserRef)
        .setOperationTimestamp(now.asTimestamp)
        .setTimestamp(now.asTimestamp)
        .build()

      (vosClient.getOptOffer _)
        .expects(offer.id, Slave)
        .returningZ(Some(offer))

      (promocoderClient.getFeature _)
        .expects(*)
        .returningZ(None)

      (promocoderClient.createFeatures _)
        .expects(*, *, *)
        .returningZ(List(stubFeature))

      (brokerClient
        .send(
          _: MessageId,
          _: FreePlacementLogOuterClass.FreePlacementLog
        )(_: ClassTag[FreePlacementLogOuterClass.FreePlacementLog]))
        .expects(MessageId(offer.getId), log, *)
        .returningZ(())

      service
        .allowFreePlacement(request)
        .provideConstantClock(now)
        .success
        .value
    }

    "fail if unable to send event to broker" in {
      val now = DateTime.now()

      val offer = offerGen().next

      val request = FreePlacementRequest
        .newBuilder()
        .setOfferId(offer.getId)
        .setDuration(20)
        .build()

      val log = FreePlacementLogOuterClass.FreePlacementLog
        .newBuilder()
        .setOfferId(offer.getId)
        .setUserId(offer.getUserRef)
        .setOperationTimestamp(now.asTimestamp)
        .setTimestamp(now.asTimestamp)
        .build()

      (vosClient.getOptOffer _)
        .expects(offer.id, Slave)
        .returningZ(Some(offer))

      (promocoderClient.getFeature _)
        .expects(*)
        .returningZ(None)

      (brokerClient
        .send(
          _: MessageId,
          _: FreePlacementLogOuterClass.FreePlacementLog
        )(_: ClassTag[FreePlacementLogOuterClass.FreePlacementLog]))
        .expects(MessageId(offer.getId), log, *)
        .throwingZ(new Exception("Failure"))

      service
        .allowFreePlacement(request)
        .provideConstantClock(now)
        .failure
        .exception
        .getMessage shouldBe "Failure"
    }

  }

}
