package ru.yandex.vertis.moderation.geo.impl

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.geo.RegionGeneralizerServiceSpec
import ru.yandex.vertis.moderation.service.geo.impl.{RegionGeneralizerServiceImpl, RegionTreeFactory}
import ru.yandex.vertis.moderation.service.geo.model.{Region, RegionType}
import ru.yandex.vertis.moderation.service.geo.{RegionGeneralizerService, RegionTreeService}

import scala.io.Codec

/**
  * Created by neron on 10.02.17.
  */
@RunWith(classOf[JUnitRunner])
class RegionGeneralizerServiceImplSpec extends RegionGeneralizerServiceSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val regionTree: RegionTreeService = RegionTreeFactory.buildFromResource("geobase.xml")

  def regionGeneralizer: RegionGeneralizerService = new RegionGeneralizerServiceImpl(regionTree)

  private val russianRegions = regionTree.getDescendants(GeoIds.Russia.geoId).futureValue.toSeq

  private val russianSF = russianRegions.filter(_.regionType == RegionType.SUBJECT_FEDERATION)

  private val russianSFChilds = {
    val russianSFGeoIdsSet = russianSF.map(_.geoId).toSet
    russianRegions.filter(r => russianSFGeoIdsSet.contains(r.parentId))
  }

  def subjectFederationGen: Gen[Region] = Gen.oneOf(russianSF)
  def subjectFederationChildGen: Gen[Region] = Gen.oneOf(russianSFChilds)

}
