package ru.yandex.vertis.moderation.geo

import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.service.geo.RegionGeneralizerService.{
  Country,
  PreSubjectFederation,
  Precision,
  SubjectFederation
}
import ru.yandex.vertis.moderation.service.geo.{RegionGeneralizerService, RegionTreeService}
import ru.yandex.vertis.moderation.service.geo.model.{GeoId, Region}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag

/**
  * Created by neron on 10.02.17.
  */
trait RegionGeneralizerServiceSpec extends SpecBase with GeneratorDrivenPropertyChecks {

  def regionTree: RegionTreeService
  def regionGeneralizer: RegionGeneralizerService

  def subjectFederationGen: Gen[Region]
  def subjectFederationChildGen: Gen[Region]

  case class RegionId(name: String, geoId: GeoId) {
    override def toString: String = s"$name($geoId)"
  }

  object GeoIds {
    val World = RegionId("World", 10000)

    val Russia = RegionId("Russia", 225)

    val CentralFederalDistrict = RegionId("CentralFederalDistrict", 3)

    val MoscowArea = RegionId("MoscowArea", 1)
    val Moscow = RegionId("Moscow", 213)
    val Zelenograd = RegionId("Zelenograd", 216)
    val BalashihaDistrict = RegionId("BalashihaDistrict", 116705)
    val Balashiha = RegionId("Balashiha", 10716)

    val LeningradskayaArea = RegionId("LeningradskayaArea", 10174)
    val Spb = RegionId("Spb", 2)
    val Kolpino = RegionId("Kolpino", 26081)
    val VsevolozhskDistrict = RegionId("VsevolozhskDistrict", 98621)
    val Vsevolozhsk = RegionId("Vsevolozhsk", 10865)

    val KrasnodarArea = RegionId("KrasnodarArea", 10995)
    val SochiDistrict = RegionId("SochiDistrict", 116900)
    val Sochi = RegionId("Sochi", 239)
    val KrasnodarDistrict = RegionId("KrasnodarDistrict", 121158)
    val Krasnodar = RegionId("Krasnodar", 35)

    val RepublicOfCrimea = RegionId("Republic of Crimea", 977)
    val Sevastopol = RegionId("Sevastopol", 959)
    val KrasnogvardeyskyDistrict = RegionId("Krasnogvardeysky District", 24700)
    val Traktovoe = RegionId("Traktovoe", 28500)

    val BadRegion = RegionId("BadRegion", -100)

    val Ukraine = RegionId("Ukraine", 187)
    val OdesaDistrict = RegionId("Odesa District", 20541)
    val LuhanskRegion = RegionId("Luhansk Region", 20540)
    val Kiev = RegionId("Kiev", 143)

    val UnitedStates = RegionId("United States", 84)
    val Irvine = RegionId("Irvine", 113615)

  }

  import GeoIds._

  private val map =
    Map(
      SubjectFederation -> Map(
        Russia -> Russia,
        CentralFederalDistrict -> CentralFederalDistrict,
        MoscowArea -> MoscowArea,
        Moscow -> MoscowArea,
        Zelenograd -> MoscowArea,
        BalashihaDistrict -> MoscowArea,
        Balashiha -> MoscowArea,
        LeningradskayaArea -> LeningradskayaArea,
        Spb -> LeningradskayaArea,
        Kolpino -> LeningradskayaArea,
        VsevolozhskDistrict -> LeningradskayaArea,
        Vsevolozhsk -> LeningradskayaArea,
        KrasnodarArea -> KrasnodarArea,
        SochiDistrict -> KrasnodarArea,
        Sochi -> KrasnodarArea,
        KrasnodarDistrict -> KrasnodarArea,
        Krasnodar -> KrasnodarArea,
        RepublicOfCrimea -> RepublicOfCrimea,
        Sevastopol -> RepublicOfCrimea,
        Traktovoe -> RepublicOfCrimea
      ),
      PreSubjectFederation -> Map(
        Russia -> Russia,
        CentralFederalDistrict -> CentralFederalDistrict,
        MoscowArea -> MoscowArea,
        Moscow -> Moscow,
        Zelenograd -> Moscow,
        BalashihaDistrict -> BalashihaDistrict,
        Balashiha -> BalashihaDistrict,
        LeningradskayaArea -> LeningradskayaArea,
        Spb -> Spb,
        Kolpino -> Spb,
        VsevolozhskDistrict -> VsevolozhskDistrict,
        Vsevolozhsk -> VsevolozhskDistrict,
        KrasnodarArea -> KrasnodarArea,
        SochiDistrict -> SochiDistrict,
        Sochi -> SochiDistrict,
        KrasnodarDistrict -> KrasnodarDistrict,
        Krasnodar -> KrasnodarDistrict,
        RepublicOfCrimea -> RepublicOfCrimea,
        Sevastopol -> Sevastopol,
        Traktovoe -> KrasnogvardeyskyDistrict
      ),
      Country -> Map(
        Russia -> Russia,
        CentralFederalDistrict -> Russia,
        MoscowArea -> Russia,
        Moscow -> Russia,
        Zelenograd -> Russia,
        BalashihaDistrict -> Russia,
        Balashiha -> Russia,
        LeningradskayaArea -> Russia,
        Spb -> Russia,
        Kolpino -> Russia,
        VsevolozhskDistrict -> Russia,
        Vsevolozhsk -> Russia,
        KrasnodarArea -> Russia,
        SochiDistrict -> Russia,
        Sochi -> Russia,
        KrasnodarDistrict -> Russia,
        Krasnodar -> Russia,
        RepublicOfCrimea -> Russia,
        Sevastopol -> Russia,
        Traktovoe -> Russia,
        Ukraine -> Ukraine,
        OdesaDistrict -> Ukraine,
        LuhanskRegion -> Ukraine,
        Kiev -> Ukraine,
        UnitedStates -> UnitedStates,
        Irvine -> UnitedStates
      )
    )

  map.foreach { case (t, rules) =>
    "regionGeneralizer" when {
      s"$t type" should {
        rules.foreach { case (fromRegion, toRegion) =>
          s"generalize $fromRegion to $toRegion" in {
            val generalizedGeoId = regionGeneralizer.generalize(fromRegion.geoId, t).futureValue
            generalizedGeoId shouldEqual toRegion.geoId
          }
        }
      }
    }
  }

  "regionGeneralizer" when {
    "Bad region" should {
      "fail in any generalization" in {
        regionGeneralizer
          .generalize(BadRegion.geoId, PreSubjectFederation)
          .shouldCompleteWithException[NoSuchElementException]
        regionGeneralizer
          .generalize(BadRegion.geoId, SubjectFederation)
          .shouldCompleteWithException[NoSuchElementException]
        regionGeneralizer
          .generalize(BadRegion.geoId, Country)
          .shouldCompleteWithException[NoSuchElementException]
        regionGeneralizer
          .generalize(BadRegion.geoId, Country, strict = true)
          .shouldCompleteWithException[NoSuchElementException]
      }
    }
    "SubjectFederation" should {
      "for every subject federation generalize its childs to this subject federation" in {
        forAll(subjectFederationGen) { regionSF =>
          checkDescendants(regionSF.geoId, SubjectFederation)
        }
      }
      "for every region not lower than subject federation generalize to itself" in {
        forAll(subjectFederationGen) { regionSF =>
          checkAscendants(regionSF.geoId, SubjectFederation)
        }
      }
    }
    "PreSubjectFederation" should {
      "for every region:(subject federation child) generalize its descendants to this region" in {
        forAll(subjectFederationChildGen) { regionSF =>
          checkDescendants(regionSF.geoId, PreSubjectFederation)
        }
      }
      "for every region not lower than (subject federation child) generalize its to itself" in {
        forAll(subjectFederationChildGen) { regionSF =>
          checkAscendants(regionSF.geoId, PreSubjectFederation)
        }
      }
    }
    "Strict option" should {
      "fail if return region with incorrect precision" in {
        regionGeneralizer
          .generalize(Russia.geoId, PreSubjectFederation, strict = true)
          .shouldCompleteWithException[IllegalArgumentException]

        regionGeneralizer
          .generalize(Russia.geoId, PreSubjectFederation, strict = true)
          .shouldCompleteWithException[IllegalArgumentException]

        regionGeneralizer
          .generalize(Russia.geoId, SubjectFederation, strict = true)
          .shouldCompleteWithException[IllegalArgumentException]

        regionGeneralizer
          .generalize(CentralFederalDistrict.geoId, PreSubjectFederation, strict = true)
          .shouldCompleteWithException[IllegalArgumentException]

        regionGeneralizer
          .generalize(CentralFederalDistrict.geoId, SubjectFederation, strict = true)
          .shouldCompleteWithException[IllegalArgumentException]

        regionGeneralizer
          .generalize(World.geoId, SubjectFederation, strict = true)
          .shouldCompleteWithException[IllegalArgumentException]

        regionGeneralizer
          .generalize(World.geoId, Country, strict = true)
          .shouldCompleteWithException[IllegalArgumentException]
      }
      "pass if strict condition is done" in {
        regionGeneralizer.generalize(Sochi.geoId, PreSubjectFederation, strict = true).futureValue
        regionGeneralizer.generalize(Krasnodar.geoId, PreSubjectFederation, strict = true).futureValue

        regionGeneralizer.generalize(KrasnodarDistrict.geoId, PreSubjectFederation, strict = true).futureValue
        regionGeneralizer.generalize(KrasnodarDistrict.geoId, SubjectFederation, strict = true).futureValue

        regionGeneralizer.generalize(Russia.geoId, Country, strict = true).futureValue
      }
    }
  }

  private def checkDescendants(geoId: GeoId, `type`: Precision): Unit = {
    val descendants = regionTree.getDescendants(geoId).futureValue.toSeq
    val sf = descendants.map(d => regionGeneralizer.generalize(d.geoId, `type`))
    val generalizedGeoIds = Future.sequence(sf).futureValue
    generalizedGeoIds.foreach(_ shouldEqual geoId)
  }

  private def checkAscendants(geoId: GeoId, `type`: Precision): Unit = {
    val ascendants = regionTree.getAscendants(geoId).futureValue
    val sf = ascendants.map(a => regionGeneralizer.generalize(a.geoId, `type`).map((a.geoId, _)))
    val generalizedGeoIds = Future.sequence(sf).futureValue
    generalizedGeoIds.foreach(x => x._2 shouldEqual x._1)
  }

}
