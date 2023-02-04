package billing.cashback.logic.test

import auto.common.clients.promocoder.PromocoderClient
import auto.common.clients.promocoder.PromocoderClient.Features
import auto.common.clients.promocoder.model.{FeatureInstanceId, PromocoderService, RefinedNonEmptyString}
import auto.common.clients.promocoder.model.features.{
  FeatureCount,
  FeatureInstance,
  FeatureInstanceRequest,
  FeatureOrigin,
  FeaturePayload,
  FeatureType,
  FeatureUnit
}
import auto.common.clients.promocoder.model.users.PromocoderUser
import auto.common.clients.promocoder.model.users.UserType.ClientUser
import common.zio.sttp.model.SttpError
import eu.timepit.refined.api.RefType
import org.joda.time.DateTime
import zio.{Has, IO, ZIO, ZLayer}
import PromocoderClientStub._
import common.zio.sttp.model.SttpError.SttpHttpError
import sttp.model.StatusCode

class PromocoderClientStub extends PromocoderClient.Service {

  private var underlyingPromocoderClient: Option[PromocoderClient.Service] = None

  def setUnderlyingPromocoderClient(promocoderClient: PromocoderClient.Service) = {
    underlyingPromocoderClient = Some(promocoderClient)
  }

  def clear() = {
    underlyingPromocoderClient = None
  }

  val client1 = "autoru_client_1"
  val featureId1 = "feature_id_1"
  var client1Balance = 5000

  val client2 = "autoru_client_2"
  val featureId2 = "feature_id_2"
  var client2Balance = 0

  override def createFeatures(
      service: PromocoderService,
      batchId: String,
      user: PromocoderUser,
      featureInstances: Iterable[FeatureInstanceRequest]): IO[SttpError, Features] = {
    underlyingPromocoderClient.get.createFeatures(service, batchId, user, featureInstances)
  }

  override def getFeatures(service: PromocoderService, user: PromocoderUser): IO[SttpError, Features] = {
//    user.userId.value match {
//      case "1" =>
//        IO.succeed(List(feature(user = client1, tag = "loyalty", featureId = featureId1, client1Balance)))
//      case "2" =>
//        IO.succeed(List(feature(user = client2, tag = "loyalty", featureId = featureId2, client2Balance)))
//      case "3" =>
//
//      case "4" =>
//
//      case _ => throw new Exception()
//    }
    underlyingPromocoderClient.get.getFeatures(service, user)
  }

  override def decrementFeatureWithKey(
      service: PromocoderService,
      featureId: FeatureInstanceId,
      count: Long,
      key: String): IO[SttpError, FeatureInstance] = {
//    featureId.value match {
//      case "feature_id_1" =>
//        client1Balance = client1Balance - count.toInt
//        IO.succeed(feature(client1, "loyalty", featureId = featureId1, client1Balance))
//      case "feature_id_2" =>
//        IO.fail(SttpHttpError(service = "test", name = "test", uri = "test", code = StatusCode(402), body = "test"))
//    }
    underlyingPromocoderClient.get.decrementFeatureWithKey(service, featureId, count, key)
  }
}

object PromocoderClientStub {

  val Test: ZLayer[Any, Nothing, Has[PromocoderClient.Service] with Has[PromocoderClientStub]] = {
    val promocoderClientStub = new PromocoderClientStub
    val promocoderClient: PromocoderClient.Service = promocoderClientStub
    val pC = ZIO.succeed(promocoderClient).toLayer
    val pCS = ZIO.succeed(promocoderClientStub).toLayer
    pC >+> pCS
  }

  def feature(user: String, tag: String, featureId: String, deadline: DateTime, balance: Int): FeatureInstance = {
    FeatureInstance(
      id = refinedString(featureId),
      origin = FeatureOrigin("origin_id"),
      tag = refinedString(tag),
      user = refinedString(user),
      count = FeatureCount(balance, FeatureUnit.Money),
      createTs = deadline.minusMonths(1),
      deadline = deadline,
      payload = FeaturePayload
        .validateAndCreate(FeatureUnit.Money, FeatureType.Loyalty, constraint = None, discount = None)
        .getOrElse(throw new Exception("failed to create payload"))
    )
  }

  def refinedString(str: String) = RefType.applyRef[RefinedNonEmptyString](str).getOrElse(throw new Exception(""))

}
