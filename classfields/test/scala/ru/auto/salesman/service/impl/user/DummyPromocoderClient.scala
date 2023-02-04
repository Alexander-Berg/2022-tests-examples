package ru.auto.salesman.service.impl.user

import org.joda.time.DateTime
import ru.auto.salesman._
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.model.{
  FeatureCount,
  FeatureInstance,
  FeatureInstanceId,
  FeatureInstanceRequest,
  FeatureOrigin,
  FeatureUnits,
  PromocoderUser
}
import ru.auto.salesman.test.dummy.IgnoredInAssertions
import zio.stm.TMap
import zio.UIO

class DummyPromocoderClient extends PromocoderClient {

  private type BatchId = String

  private val features = TMap
    .empty[(PromocoderUser, BatchId), List[FeatureInstance]]
    .commit
    .unsafeRun()

  def getFeatures(user: PromocoderUser): UIO[List[FeatureInstance]] =
    features.toList
      .map(_.collect { case ((`user`, _), feature) => feature }.flatten)
      .commit

  override def getFeature(featureId: FeatureInstanceId): Task[Option[FeatureInstance]] =
    features.toList
      .map(_.flatMap { case (_, features) => features }.find(_.id == featureId))
      .commit

  def changeFeatureCount(
      featureId: FeatureInstanceId,
      count: FeatureCount
  ): UIO[FeatureInstance] =
    IgnoredInAssertions.featureInstance

  def changeFeatureCountIdempotent(
      featureId: FeatureInstanceId,
      key: String,
      count: FeatureCount
  ): UIO[Unit] =
    IgnoredInAssertions.unit

  def createFeatures(
      batchId: String,
      user: PromocoderUser,
      featureInstances: Iterable[FeatureInstanceRequest]
  ): UIO[List[FeatureInstance]] =
    (features.putIfAbsent(
      user -> batchId,
      featureInstances.map { request =>
        FeatureInstance(
          "test_id",
          FeatureOrigin("test_origin"),
          request.tag,
          user.value,
          FeatureCount(request.count, FeatureUnits.Items),
          DateTime.now(),
          DateTime.now().plusDays(request.lifetime.toDays.toInt),
          request.jsonPayload
        )
      }.toList
    ) *> features.getOrElse(user -> batchId, Nil)).commit

  def deleteFeatures(batchId: String, user: PromocoderUser): UIO[Unit] =
    features.delete(user -> batchId).commit

  val clean: UIO[Unit] = features.removeIf((_, _) => true).commit
}
