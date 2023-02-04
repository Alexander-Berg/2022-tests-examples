package ru.yandex.vertis.telepony.geo.impl

import org.scalacheck.Gen
import ru.yandex.vertis.telepony.IntegrationSpecTemplate
import ru.yandex.vertis.telepony.geo.model.{Region, RegionType}
import ru.yandex.vertis.telepony.geo.{RegionGeneralizerService, RegionGeneralizerServiceSpec}

/**
  * Created by neron on 10.02.17.
  */
class RegionGeneralizerServiceImplIntSpec extends RegionGeneralizerServiceSpec with IntegrationSpecTemplate {

  def regionGeneralizer: RegionGeneralizerService = regionGeneralizerService

  private val russianRegions = regionTree.getDescendants(GeoIds.Russia.geoId).futureValue.toSeq

  private val russianSF = russianRegions.filter(_.regionType == RegionType.SUBJECT_FEDERATION)

  private val russianSFChilds = {
    val russianSFGeoIdsSet = russianSF.map(_.geoId).toSet
    russianRegions.filter(r => russianSFGeoIdsSet.contains(r.parentId))
  }

  def subjectFederationGen: Gen[Region] = Gen.oneOf(russianSF)

  def subjectFederationChildGen: Gen[Region] = Gen.oneOf(russianSFChilds)

}
