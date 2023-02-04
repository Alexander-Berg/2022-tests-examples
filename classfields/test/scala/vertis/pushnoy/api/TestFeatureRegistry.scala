package vertis.pushnoy.api

import ru.yandex.vertis.feature.model.{Feature, FeatureRegistry, FeatureType, TypedValue}

import scala.concurrent.Future

/** @author kusaeva
  */
class TestFeatureRegistry extends FeatureRegistry {

  override def register[V](
      name: String,
      initialValue: V,
      tags: Set[String],
      info: Option[String]
    )(implicit evidence$1: FeatureType[V]): Feature[V] = ???

  override def getFeatures: Future[Seq[Feature[TypedValue]]] = ???

  override def updateFeature(
      name: String,
      value: String,
      operator: Option[String],
      comment: Option[String]): Future[Unit] = ???

  override def updateFeatureTags(name: String, tags: Set[String]): Future[Unit] = ???

  override def updateFeatureInfo(name: String, info: Option[String]): Future[Unit] = ???

  override def deleteFeature(name: String): Future[Boolean] = ???
}
