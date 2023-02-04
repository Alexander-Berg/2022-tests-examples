package common.zio.features.testkit

import ru.yandex.vertis.feature.impl.BasicFeatureTypes
import ru.yandex.vertis.feature.model.{Feature, FeatureRegistry, FeatureType, TypedValue}

import java.time.Instant
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

class InMemoryFeatureRegistry extends FeatureRegistry {

  private val features: TrieMap[String, Feature[TypedValue]] = TrieMap.empty

  override def register[V](
      name: String,
      initialValue: V,
      tags: Set[String],
      info: Option[String]
    )(implicit ft: FeatureType[V]): Feature[V] = {
    features.addOne(name -> Feature(name, _ => TypedValue(ft.key, ft.serDe.serialize(initialValue), tags, info)))
    Feature(name, _ => initialValue)
  }

  override def getFeatures: Future[Seq[Feature[TypedValue]]] = Future.successful(features.values.toList)

  override def updateFeature(
      name: String,
      value: String,
      operator: Option[String],
      comment: Option[String]): Future[Unit] = Future.successful {
    val changeEvent = TypedValue.ChangeEvent(Instant.now(), value, operator, comment)
    features.updateWith(name) {
      case Some(feature) =>
        Some(Feature(name, _ => feature.value.copy(value = value, history = feature.value.history :+ changeEvent)))
      case None =>
        Some(Feature(name, _ => TypedValue(BasicFeatureTypes.StringFeatureType.key, value, history = Seq(changeEvent))))
    }
    ()
  }

  override def updateFeatureTags(name: String, tags: Set[String]): Future[Unit] = Future.successful {
    features.updateWith(name)(_.map(feature => Feature(feature.name, _ => feature.value.copy(tags = tags))))
    ()
  }

  override def updateFeatureInfo(name: String, info: Option[String]): Future[Unit] = Future.successful {
    features.updateWith(name)(_.map(feature => Feature(feature.name, _ => feature.value.copy(info = info))))
    ()
  }

  override def deleteFeature(name: String): Future[Boolean] = Future.successful(features.remove(name).nonEmpty)
}
