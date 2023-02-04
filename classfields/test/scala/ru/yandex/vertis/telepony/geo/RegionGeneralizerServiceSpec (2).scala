package ru.yandex.vertis.telepony.geo

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.geo.model.{Region, RegionType}
import ru.yandex.vertis.telepony.model.{GeoId, PhoneType, PhoneTypes}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by neron on 10.02.17.
  */
trait RegionGeneralizerServiceSpec extends SpecBase with ScalaCheckDrivenPropertyChecks {

  def regionTree: RegionTreeService

  def regionGeneralizer: RegionGeneralizerService

  def subjectFederationGen: Gen[Region]

  def subjectFederationChildGen: Gen[Region]

  case class RegionId(name: String, geoId: GeoId) {
    override def toString: String = s"$name($geoId)"
  }

  object GeoIds {
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
  }

  import GeoIds._

  private val map = Map(
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
    Krasnodar -> KrasnodarArea
  )

  private def phoneTypeName(phoneType: PhoneType): String =
    phoneType match {
      case PhoneTypes.Mobile => "Mobile"
      case PhoneTypes.Local => "Local"
    }

  "regionGeneralizer" when {
    "generalize" should {
      map.foreach {
        case (fromRegion, toRegion) =>
          PhoneTypes.AllKnown.foreach { phoneType =>
            s"$fromRegion to $toRegion for ${phoneTypeName(phoneType)} phone type" in {
              val generalizedGeoId = regionGeneralizer.generalize(fromRegion.geoId, phoneType).futureValue
              generalizedGeoId shouldEqual toRegion.geoId
            }
          }
      }
    }

    "Mobile phone" should {
      "for every subject federation generalize its childs to this subject federation" in {
        forAll(subjectFederationGen) { regionSF =>
          checkDescendants(regionSF.geoId, PhoneTypes.Mobile)
        }
      }
      "for every region not lower than subject federation generalize to itself" in {
        forAll(subjectFederationGen) { regionSF =>
          checkAscendants(regionSF.geoId, PhoneTypes.Mobile)
        }
      }
    }
    "Local phone" should {
      "for every region:(subject federation child) generalize its descendants to this region" in {
        forAll(subjectFederationGen) { regionSF =>
          checkDescendants(regionSF.geoId, PhoneTypes.Local)
        }
      }
      "for every region not lower than (subject federation child) generalize its to itself" in {
        forAll(subjectFederationGen) { regionSF =>
          checkAscendants(regionSF.geoId, PhoneTypes.Local)
        }
      }
    }
    "Generalize to method cannot find geo id" should {
      "return argument geo id" in {
        regionGeneralizer.generalizeTo(Russia.geoId, RegionType.CITY).futureValue shouldEqual None
      }
    }
    "Generalize to" should {
      "return generalized geo id" in {
        val geoId = regionGeneralizer.generalizeTo(Krasnodar.geoId, RegionType.COUNTRY).futureValue
        geoId shouldEqual Some(Russia.geoId)
      }
    }
  }

  private def checkDescendants(geoId: GeoId, phoneType: PhoneType): Unit = {
    val descendants = regionTree.getDescendants(geoId).futureValue.toSeq
    val sf = descendants.map(d => regionGeneralizer.generalize(d.geoId, phoneType))
    val generalizedGeoIds = Future.sequence(sf).futureValue
    generalizedGeoIds.foreach(_ shouldEqual geoId)
  }

  private def checkAscendants(geoId: GeoId, phoneType: PhoneType): Unit = {
    val ascendants = regionTree.getAscendants(geoId).futureValue
    val sf = ascendants.map(a => regionGeneralizer.generalize(a.geoId, phoneType).map((a.geoId, _)))
    val generalizedGeoIds = Future.sequence(sf).futureValue
    generalizedGeoIds.foreach(x => x._2 shouldEqual x._1)
  }

}
