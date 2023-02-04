package ru.yandex.vertis.telepony.dummy

import ru.yandex.vertis.telepony.model.{GeoId, PhoneType}
import ru.yandex.vertis.telepony.service.GeoFallbackRulesService

class DummyGeoFallbackRulesService extends GeoFallbackRulesService {
  override def mixFallbackAttributes(attributes: Seq[(GeoId, PhoneType)]): Seq[(GeoId, PhoneType)] = attributes

  override def mixFallbackGeoCandidates(geoCandidates: Seq[GeoId], phoneType: PhoneType): Seq[GeoId] = geoCandidates
}
