package ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.converter

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.joda.time.format.DateTimeFormat
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.enablers.Emptiness.emptinessOfGenTraversable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import ru.auto.api.vin.VinReportModel.{InsuranceType, OwnerType}
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.ScrapinghubRsaReportType
import ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.model.{RsaResponse, RsaResponseRawModel}
import ru.yandex.auto.vin.decoder.proto.VinHistory.Insurance.InsurancePurpose
import ru.yandex.auto.vin.decoder.proto.VinHistory.InsuranceStatus
import ru.yandex.auto.vin.decoder.rsa.common.RsaCommons

import scala.jdk.CollectionConverters.ListHasAsScala

class RsaRawToPreparedConverterSpec extends AnyWordSpecLike with Matchers {

  implicit val t: Traced = Traced.empty

  val converter = new RsaRawToPreparedConverter

  "RsaRawToPreparedConverter" must {

    "correctly convert SH RSA current insurances raw model to VinInfoHistory" in {
      val vinInfo = converter.convert(currentInsurancesModel).futureValue
      val insurances = vinInfo.getInsurancesList.asScala
      val identifiers = vinInfo.getVehicleIdentifiers

      identifiers.getVin shouldBe "W1K2130131A830536"
      identifiers.getLicensePlate shouldBe empty

      (insurances should have).length(3)

      insurances.head.getSerial shouldBe "ХХХ"
      insurances.head.getNumber shouldBe "0149894097"
      insurances.head.getInsuranceStatus shouldBe InsuranceStatus.ACTIVE
      insurances.head.getInsurerName shouldBe "ООО \"СК \"Согласие\""
      insurances.head.getOwnerInfo.getName shouldBe "К***** МИХАИЛ ИЛЬИЧ"
      // insurances.head.getOwnerInfo.getDateOfBirth shouldBe 170370000000L
      insurances.head.getOwnerInfo.getOwnerType shouldBe OwnerType.Type.PERSON
      insurances.head.getInsurantInfo.getName shouldBe "А***** ИВАН Михайлович"
      // insurances.head.getInsurantInfo.getDateOfBirth shouldBe 867355200000L
      insurances.head.getInsurantInfo.getOwnerType shouldBe OwnerType.Type.PERSON
      insurances.head.getIsRestrict.getValue shouldBe true
      insurances.head.getInsuranceType shouldBe InsuranceType.OSAGO
      insurances.head.getCity shouldBe "Дагестан Респ"
      insurances.head.getInsurancePurpose shouldBe InsurancePurpose.TAXI

      insurances(1).getSerial shouldBe "ХХХ"
      insurances(1).getNumber shouldBe "0150515194"
      insurances(1).getInsuranceStatus shouldBe InsuranceStatus.ACTIVE
      insurances(1).getInsurerName shouldBe "ООО \"СК \"Согласие\""
      insurances(1).getOwnerInfo.getName shouldBe "З***** Александр Геннадиевич"
      // insurances(1).getOwnerInfo.getDateOfBirth shouldBe 253918800000L
      insurances(1).getOwnerInfo.getOwnerType shouldBe OwnerType.Type.PERSON
      insurances(1).getIsRestrict.getValue shouldBe true
      insurances(1).getInsuranceType shouldBe InsuranceType.OSAGO
      insurances(1).getCity shouldBe "Дагестан Респ"
      insurances(1).getInsurancePurpose shouldBe InsurancePurpose.PERSONAL

      insurances(2).getSerial shouldBe "МММ"
      insurances(2).getNumber shouldBe "5039230435"
      insurances(2).getInsuranceStatus shouldBe InsuranceStatus.ACTIVE
      insurances(2).getInsurerName shouldBe "СПАО \"Ингосстрах\""
      insurances(2).getOwnerInfo.getName shouldBe "ОБЩЕСТВОСОГРАНИЧЕННОИОТВЕТСТВЕННОСТЬЮЩЕБЕНЬОНЛАИН"
      insurances(2).getOwnerInfo.getInn shouldBe "7839481040"
      insurances(2).getOwnerInfo.getOwnerType shouldBe OwnerType.Type.LEGAL

      insurances(2).getIsRestrict.getValue shouldBe false
      insurances(2).getInsuranceType shouldBe InsuranceType.OSAGO
      insurances(2).getCity shouldBe "г Санкт-Петербург"
      insurances(2).getInsurancePurpose shouldBe InsurancePurpose.UNKNOWN_PURPOSE

    }

    "correctly convert SH RSA current insurances raw model to VinInfoHistory if identifiers are wrong" in {
      val vinInfo = converter.convert(currentInsurancesWrongVinModel).futureValue
      val insurances = vinInfo.getInsurancesList.asScala
      val identifiers = vinInfo.getVehicleIdentifiers

      identifiers.getVin shouldBe empty
      identifiers.getLicensePlate shouldBe empty

      (insurances should have).length(3)

      insurances.head.getSerial shouldBe "ХХХ"
      insurances.head.getNumber shouldBe "0149894097"
      insurances.head.getInsuranceStatus shouldBe InsuranceStatus.ACTIVE
      insurances.head.getInsurerName shouldBe "ООО \"СК \"Согласие\""
      insurances.head.getOwnerInfo.getName shouldBe "К***** МИХАИЛ ИЛЬИЧ"
      // insurances.head.getOwnerInfo.getDateOfBirth shouldBe 170370000000L
      insurances.head.getOwnerInfo.getOwnerType shouldBe OwnerType.Type.PERSON
      insurances.head.getIsRestrict.getValue shouldBe true
      insurances.head.getInsuranceType shouldBe InsuranceType.OSAGO
      insurances.head.getCity shouldBe "Дагестан Респ"
      insurances.head.getInsurancePurpose shouldBe InsurancePurpose.TAXI

      insurances(1).getSerial shouldBe "ХХХ"
      insurances(1).getNumber shouldBe "0150515194"
      insurances(1).getInsuranceStatus shouldBe InsuranceStatus.ACTIVE
      insurances(1).getInsurerName shouldBe "ООО \"СК \"Согласие\""
      insurances(1).getOwnerInfo.getName shouldBe "З***** Александр Геннадиевич"
      // insurances(1).getOwnerInfo.getDateOfBirth shouldBe 253918800000L
      insurances(1).getOwnerInfo.getOwnerType shouldBe OwnerType.Type.PERSON
      insurances(1).getIsRestrict.getValue shouldBe true
      insurances(1).getInsuranceType shouldBe InsuranceType.OSAGO
      insurances(1).getCity shouldBe "Дагестан Респ"
      insurances(1).getInsurancePurpose shouldBe InsurancePurpose.PERSONAL

      insurances(2).getSerial shouldBe "МММ"
      insurances(2).getNumber shouldBe "5039230435"
      insurances(2).getInsuranceStatus shouldBe InsuranceStatus.ACTIVE
      insurances(2).getInsurerName shouldBe "СПАО \"Ингосстрах\""
      insurances(2).getOwnerInfo.getName shouldBe "ОБЩЕСТВОСОГРАНИЧЕННОИОТВЕТСТВЕННОСТЬЮЩЕБЕНЬОНЛАИН"
      insurances(2).getOwnerInfo.getInn shouldBe "7839481040"
      insurances(2).getOwnerInfo.getOwnerType shouldBe OwnerType.Type.LEGAL
      insurances(2).getIsRestrict.getValue shouldBe false
      insurances(2).getInsuranceType shouldBe InsuranceType.OSAGO
      insurances(2).getCity shouldBe "г Санкт-Петербург"
      insurances(2).getInsurancePurpose shouldBe InsurancePurpose.UNKNOWN_PURPOSE
    }

    "correctly convert SH RSA insurance details raw model to VinInfoHistory" in {
      val vinInfo = converter.convert(insuranceDetailsModel).futureValue
      val insurances = vinInfo.getInsurancesList.asScala
      val identifiers = vinInfo.getVehicleIdentifiers

      identifiers.getVin shouldBe "XW8ZZZ3CZ9G001083"
      identifiers.getLicensePlate shouldBe "O846HK102"

      (insurances should have).length(1)

      insurances.head.getSerial shouldBe "МММ"
      insurances.head.getNumber shouldBe "5024887026"
      insurances.head.getInsuranceStatus shouldBe InsuranceStatus.EXPIRED
      insurances.head.getInsurerName shouldBe "\"Совкомбанк страхование\" (АО)"
      insurances.head.getIsRestrict.getValue shouldBe true
      insurances.head.getInsuranceType shouldBe InsuranceType.OSAGO
      insurances.head.getCity shouldBe "Башкортостан Респ"
      insurances.head.getPolicyStatus shouldBe "Выдан страхователю"
      insurances.head.getFrom shouldBe DateTimeFormat.forPattern("dd.MM.yyyy").parseMillis("04.10.2019")
      insurances.head.getInsurancePurpose shouldBe InsurancePurpose.TAXI
    }

    "correctly convert SH RSA empty current insurances raw model to VinInfoHistory" in {

      val converter = new RsaRawToPreparedConverter
      val vinInfo = converter.convert(emptyCurrentInsurancesModel).futureValue
      val insurances = vinInfo.getInsurancesList.asScala
      val identifiers = vinInfo.getVehicleIdentifiers

      identifiers.getVin shouldBe empty
      identifiers.getLicensePlate shouldBe empty

      insurances shouldBe empty

      vinInfo.getEventType shouldBe EventType.SH_RSA_CURRENT_INSURANCES
      vinInfo.getVin shouldBe "W1K2130131A830536"
      vinInfo.getGroupId shouldBe ""
    }

    "throw illegal argument if purpose of usage is unmapped" in {
      assertThrows[IllegalArgumentException] {
        val converter = new RsaRawToPreparedConverter
        converter.convert(currentInsurancesModelWithUnmappedPurpose)
      }
    }
  }

  "parse owner info from raw" should {
    "return personal info" in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "Ш***** ЕВГЕНИЙ ФЁДОРОВИЧ 11.04.1984"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "Ш***** ЕВГЕНИЙ ФЁДОРОВИЧ")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info (part name) " in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "А***** АЗИЗБЕК 18.04.1989"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "А***** АЗИЗБЕК")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info (double surname) " in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "М***** ИБРАГИМ ТАХИР ОГЛЫ 07.04.1979"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "М***** ИБРАГИМ ТАХИР ОГЛЫ")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info name with -" in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "М***** Седа Сайд-Магомедовна 01.04.1984"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "М***** Седа Сайд-Магомедовна")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info (redundant point)" in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "Ф***** ДЖОЭЛЬ . 14.05.1997"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "Ф***** ДЖОЭЛЬ")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info (without first letter)" in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "***** Сергей Юрьевич 28.08.1963"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "***** Сергей Юрьевич")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info (with dot between fio and year)" in {
      val vin = VinCode("JTJBAMCA402027754")
      val raw = "В***** Александр Анатольевич. 17.08.1975"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "В***** Александр Анатольевич")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info forall with ?" in {
      val vin = VinCode("XTA21723090051917")
      val raw = "?***** ??????? ?????????? 21.09.1974"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "?***** ??????? ??????????")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info with 0 instead of O and transform 0 to О" in {
      val vin = VinCode("XTA21723090051917")
      val raw = "М***** ДМИТРИЙ 0 04.02.1973"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "М***** ДМИТРИЙ О")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return personal info in english" in {
      val vin = VinCode("XTA21723090051917")
      val raw = "T***** SUREN 16.01.1965"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "T***** SUREN")
      assert(res.get.getOwnerType == OwnerType.Type.PERSON)
    }
    "return legal info" in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "ОООЩЕБЕНЬОНЛАИН, ИНН 7839481040"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "ОООЩЕБЕНЬОНЛАИН")
      assert(res.get.getOwnerType == OwnerType.Type.LEGAL)
    }
    "return legal info (without INN)" in {
      val vin = VinCode("JTEES42A702225576")
      val raw = "ОСООСТАТУСЛИЗИНГ,"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "ОСООСТАТУСЛИЗИНГ")
      assert(res.get.getOwnerType == OwnerType.Type.LEGAL)
    }
    "return legal info (without INN) 2" in {
      val vin = VinCode("JTEBR3FJ40K011566")
      val raw = "ПРЕДСТАВИТЕЛЬСТВОКОМПАНИИСОГРАНИЧЕННОИОТВЕТСТВЕННОСТЬЮНИПРОМЕДИКАЛЮРОП,"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "ПРЕДСТАВИТЕЛЬСТВОКОМПАНИИСОГРАНИЧЕННОИОТВЕТСТВЕННОСТЬЮНИПРОМЕДИКАЛЮРОП")
      assert(res.get.getOwnerType == OwnerType.Type.LEGAL)
    }
    "return legal info (without INN) 3" in {
      val vin = VinCode("ZVW30-1054916")
      val raw = "ОООГАРМОНИЯ2021,"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "ОООГАРМОНИЯ2021")
      assert(res.get.getOwnerType == OwnerType.Type.LEGAL)
    }
    "return legal info (without INN) with ИНН 0" in {
      val vin = VinCode("WK0S0002400119601")
      val raw = "ОООВЕКТОР, ИНН 0"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.nonEmpty)
      assert(res.get.getName == "ОООВЕКТОР")
      assert(res.get.getOwnerType == OwnerType.Type.LEGAL)
    }
    "return none (very strange owner)" in {
      val vin = VinCode("WK0S0002400119601")
      val raw = "-***** Ооо Щебень.онлайн 01.01.2000"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.isEmpty)
    }
    "return none" in {
      val vin = VinCode("W1K2130131A830536")
      val raw = "Сведения отсутствуют"
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.isEmpty)
    }
    "return none (raw is empty or with space)" in {
      val vin = VinCode("W1K2130131A830536")
      val raw = " "
      val res = RsaCommons.parseOwnerInfoFromRaw(raw, vin)

      assert(res.isEmpty)
    }

    "correctly convert SH RSA empty current insurances with error message raw model to VinInfoHistory" in {

      val converter = new RsaRawToPreparedConverter
      val vinInfo = converter.convert(emptyCurrentInsurancesModelWithErrorDetails).futureValue
      val insurances = vinInfo.getInsurancesList.asScala
      val identifiers = vinInfo.getVehicleIdentifiers

      identifiers.getVin shouldBe empty
      identifiers.getLicensePlate shouldBe empty

      insurances shouldBe empty

      vinInfo.getEventType shouldBe EventType.SH_RSA_CURRENT_INSURANCES
      vinInfo.getVin shouldBe "TMAK581GFLJ009589"
      vinInfo.getGroupId shouldBe ""
    }

  }

  private val currentInsurancesRaw = ResourceUtils.getStringFromResources("/sh_rsa/current_insurances.json")

  private val currentInsurancesWrongVinRaw =
    ResourceUtils.getStringFromResources("/sh_rsa/current_insurances_wrong_vin.json")
  private val insuranceDetailsRaw = ResourceUtils.getStringFromResources("/sh_rsa/insurance_details.json")
  private val emptyCurrentInsurancesRaw = ResourceUtils.getStringFromResources("/sh_rsa/empty_current_insurances.json")

  private val emptyCurrentInsurancesWithErrorRaw =
    ResourceUtils.getStringFromResources("/sh_rsa/empty_current_insurances_with_error_msg.json")

  private val currentInsuranceWithUnmappedPurpose =
    ResourceUtils.getStringFromResources("/sh_rsa/current_insurances_unmapped_purpose.json")

  private lazy val currentInsurancesModelWithUnmappedPurpose = RsaResponseRawModel(
    currentInsuranceWithUnmappedPurpose,
    "200",
    VinCode("W1K2130131A830536"),
    "",
    ScrapinghubRsaReportType.CurrentInsurances,
    Json.parse(currentInsuranceWithUnmappedPurpose).as[RsaResponse]
  )

  private lazy val currentInsurancesModel = RsaResponseRawModel(
    currentInsurancesRaw,
    "200",
    VinCode("W1K2130131A830536"),
    "",
    ScrapinghubRsaReportType.CurrentInsurances,
    Json.parse(currentInsurancesRaw).as[RsaResponse]
  )

  private lazy val currentInsurancesWrongVinModel = RsaResponseRawModel(
    currentInsurancesWrongVinRaw,
    "200",
    VinCode("W0L0JBF19X1138160"),
    "",
    ScrapinghubRsaReportType.CurrentInsurances,
    Json.parse(currentInsurancesWrongVinRaw).as[RsaResponse]
  )

  private lazy val insuranceDetailsModel = RsaResponseRawModel(
    insuranceDetailsRaw,
    "200",
    VinCode("XW8ZZZ3CZ9G001083"),
    "МММ 5024887026",
    ScrapinghubRsaReportType.InsuranceDetails,
    Json.parse(insuranceDetailsRaw).as[RsaResponse]
  )

  private lazy val emptyCurrentInsurancesModel = RsaResponseRawModel(
    emptyCurrentInsurancesRaw,
    "200",
    VinCode("W1K2130131A830536"),
    "",
    ScrapinghubRsaReportType.CurrentInsurances,
    Json.parse(emptyCurrentInsurancesRaw).as[RsaResponse]
  )

  private lazy val emptyCurrentInsurancesModelWithErrorDetails = RsaResponseRawModel(
    emptyCurrentInsurancesWithErrorRaw,
    "200",
    VinCode("TMAK581GFLJ009589"),
    "",
    ScrapinghubRsaReportType.CurrentInsurances,
    Json.parse(emptyCurrentInsurancesWithErrorRaw).as[RsaResponse]
  )
}
