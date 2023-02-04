package vsquality.migration.featureregistry

import ru.yandex.vertis.feature.model.{FeatureType, FeatureTypes, SerDe}

object CustomTypes extends FeatureTypes {

  implicit case object CustomTypeFeatureType extends FeatureType[CustomType] {
    override protected def id: String = "CustomType"

    override def serDe: SerDe[CustomType] = SerDe(
      v => s"${v.id}_${v.name}",
      str => {
        val split = str.split("_").array
        CustomType(split(0).toInt, split(1))
      }
    )
  }
  override def featureTypes: Iterable[FeatureType[_]] = Iterable(CustomTypeFeatureType)
}

case class CustomType(id: Int, name: String)
