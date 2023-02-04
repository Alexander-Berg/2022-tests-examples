package ru.yandex.realty2.extdataloader.loaders.sites.mortgages

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.message.Mortgages
import ru.yandex.realty.model.message.Mortgages.{Bank, MortgageProgram}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.sites.special.proposals.MortgageSpecialProposal
import ru.yandex.realty.model.sites.{Mortgage, Site}
import ru.yandex.realty.storage.BankStorageImpl
import ru.yandex.realty.{CommonConstants, SpecBase}

import java.util.Collections
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SiteMortgagesEnricherSpec extends SpecBase with ScalaCheckPropertyChecks {

  private val mortgageBankId = 22L
  private val mortgageBank = Bank
    .newBuilder()
    .setId(mortgageBankId)
    .addMortgageProgram(
      MortgageProgram
        .newBuilder()
        .setId(101L)
        .setMortgageType(Mortgages.MortgageType.M_YOUNG_FAMILY)
        .setBankId(mortgageBankId)
        .setProgramName("Семейная")
        .build()
    )
    .addMortgageProgram(
      MortgageProgram
        .newBuilder()
        .setId(102L)
        .setMortgageType(Mortgages.MortgageType.M_STATE_SUPPORT)
        .setBankId(mortgageBankId)
        .setProgramName("Гос поддержка")
        .build()
    )
    .addMortgageProgram(
      MortgageProgram
        .newBuilder()
        .setId(103L)
        .setMortgageType(Mortgages.MortgageType.M_MILITARY)
        .setBankId(mortgageBankId)
        .setProgramName("Военная")
        .build()
    )
    .addMortgageProgram(
      MortgageProgram
        .newBuilder()
        .setId(104L)
        .setMortgageType(Mortgages.MortgageType.M_EASTERN)
        .setBankId(mortgageBankId)
        .setProgramName("Дальневосточная")
        .build()
    )
    .build()
  private val otherBankId = 33L
  private val otherBank = Bank.newBuilder().setId(otherBankId).build()
  private val bankStorage = new BankStorageImpl(Seq(mortgageBank, otherBank))
  private val siteMortgageTestData =
    Table(
      ("description", "site", "expectedMortgages", "expectedBankMortgages"),
      ("empty site", new Site(1), Set[Mortgage](), Set[Mortgage]()),
      ("site with no partnerBanks not PIK no spec offers", new Site(1), Set[Mortgage](), Set[Mortgage]()),
      ("site with no partnerBanks PIK no spec offers", {
        val site = new Site(1)
        site.setBuilders(Collections.singletonList(CommonConstants.PIK_DEVELOPER_ID))
        site
      }, Set[Mortgage](), Set[Mortgage]()),
      ("site with no partnerBanks not PIK with spec offers", {
        val site = new Site(1)
        val mortgageSpecialProposal = new MortgageSpecialProposal(11L, "Ипотека 1,99%")
        mortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_YOUNG_FAMILY)
        site.setMortgageSpecialProposals(Collections.singletonList(mortgageSpecialProposal))
        site
      }, {
        val mortgage = new Mortgage(11L, mortgageBank.getId(), "Ипотека 1,99%")
        mortgage.setMilitaryMortgage(false)
        Set(mortgage)
      }, Set[Mortgage]()),
      ("site with no partnerBanks PIK with spec offers", {
        val site = new Site(1)
        site.setBuilders(Collections.singletonList(CommonConstants.PIK_DEVELOPER_ID))
        val mortgageSpecialProposal = new MortgageSpecialProposal(21L, "Ипотека 4,95%")
        mortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_STATE_SUPPORT)
        site.setMortgageSpecialProposals(Collections.singletonList(mortgageSpecialProposal))
        site
      }, {
        val mortgage = new Mortgage(21L, mortgageBank.getId(), "Ипотека 4,95%")
        mortgage.setMilitaryMortgage(false)
        Set(mortgage)
      }, Set[Mortgage]()),
      ("site with no partnerBanks PIK with military and far eastern spec offers", {
        val site = new Site(1)
        val location = new Location()
        location.setSubjectFederation(Regions.RESPUBLICA_BURIATIA, 123123L)
        site.setLocation(location)
        site.setHasMilitaryMortgage(true)
        site.setBuilders(Collections.singletonList(CommonConstants.PIK_DEVELOPER_ID))
        val militaryMortgageSpecialProposal = new MortgageSpecialProposal(31L, "Военная ипотека")
        militaryMortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        militaryMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_MILITARY)
        val farEasternMortgageSpecialProposal = new MortgageSpecialProposal(41L, "Дальневосточная ипотека")
        farEasternMortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        farEasternMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_EASTERN)
        site.setMortgageSpecialProposals(Seq(militaryMortgageSpecialProposal, farEasternMortgageSpecialProposal).asJava)
        site
      }, {
        val militaryMortgage = new Mortgage(31L, mortgageBank.getId(), "Военная ипотека")
        militaryMortgage.setMilitaryMortgage(true)
        val farEasternMortgage = new Mortgage(41L, mortgageBank.getId(), "Дальневосточная ипотека")
        farEasternMortgage.setMilitaryMortgage(false)
        Set(militaryMortgage, farEasternMortgage)
      }, Set[Mortgage]()),
      ("site with no partnerBanks no PIK with no military with far eastern spec offers", {
        val site = new Site(1)
        val location = new Location()
        location.setSubjectFederation(Regions.RESPUBLICA_BURIATIA, 123123L)
        site.setLocation(location)
        site.setHasMilitaryMortgage(false)
        val militaryMortgageSpecialProposal = new MortgageSpecialProposal(31L, "Военная ипотека")
        militaryMortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        militaryMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_MILITARY)
        val farEasternMortgageSpecialProposal = new MortgageSpecialProposal(41L, "Дальневосточная ипотека")
        farEasternMortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        farEasternMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_EASTERN)
        site.setMortgageSpecialProposals(Seq(militaryMortgageSpecialProposal, farEasternMortgageSpecialProposal).asJava)
        site
      }, {
        val farEasternMortgage = new Mortgage(41L, mortgageBank.getId(), "Дальневосточная ипотека")
        farEasternMortgage.setMilitaryMortgage(false)
        Set(farEasternMortgage)
      }, Set[Mortgage]()),
      ("site with no partnerBanks no PIK with military and no far eastern spec offers", {
        val site = new Site(1)
        val location = new Location()
        location.setSubjectFederation(Regions.KRASNODARSKYJ_KRAI, NodeRgid.KRASNODARSKYJ_KRAI)
        site.setLocation(location)
        site.setHasMilitaryMortgage(true)
        val militaryMortgageSpecialProposal = new MortgageSpecialProposal(31L, "Военная ипотека")
        militaryMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        militaryMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_MILITARY)
        val farEasternMortgageSpecialProposal = new MortgageSpecialProposal(41L, "Дальневосточная ипотека")
        farEasternMortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        farEasternMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_EASTERN)
        site.setMortgageSpecialProposals(Seq(militaryMortgageSpecialProposal, farEasternMortgageSpecialProposal).asJava)
        site
      }, {
        val militaryMortgage = new Mortgage(31L, otherBank.getId(), "Военная ипотека")
        militaryMortgage.setMilitaryMortgage(true)
        Set(militaryMortgage)
      }, Set[Mortgage]()),
      ("site with no partnerBanks without military and far eastern spec offers", {
        val site = new Site(1)
        site.setHasMilitaryMortgage(false)
        val militaryMortgageSpecialProposal = new MortgageSpecialProposal(31L, "Военная ипотека")
        militaryMortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        militaryMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_MILITARY)
        val farEasternMortgageSpecialProposal = new MortgageSpecialProposal(41L, "Дальневосточная ипотека")
        farEasternMortgageSpecialProposal.setBankIds(Collections.singletonList(mortgageBankId))
        farEasternMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_EASTERN)
        val familyMortgageSpecialProposal = new MortgageSpecialProposal(51L, "Семейная ипотека")
        familyMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        familyMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_YOUNG_FAMILY)
        site.setMortgageSpecialProposals(
          Seq(militaryMortgageSpecialProposal, farEasternMortgageSpecialProposal, familyMortgageSpecialProposal).asJava
        )
        site
      }, {
        val familyMortgage = new Mortgage(51L, otherBank.getId(), "Семейная ипотека")
        familyMortgage.setMilitaryMortgage(false)
        Set(familyMortgage)
      }, Set[Mortgage]()),
      ("site with partnerBanks no PIK without spec offers and mortgage programs", {
        val site = new Site(1)
        site.setPartnerBanks(Collections.singletonList(otherBankId))
        site
      }, Set[Mortgage](), Set[Mortgage]()),
      ("site with partnerBanks and mortgage programs", {
        val site = new Site(1)
        site.setPartnerBanks(Seq(mortgageBankId, otherBankId, 123432423L).map(Long.box).asJava)
        site
      }, Set[Mortgage](), {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        Set(familyMortgage, stateMortgage).map(prepareForMortgageProgram)
      }),
      ("site with partnerBanks and mortgage programs with millitary", {
        val site = new Site(1)
        site.setHasMilitaryMortgage(true)
        site.setPartnerBanks(Seq(mortgageBankId, otherBankId, 123432423L).map(Long.box).asJava)
        site
      }, Set[Mortgage](), {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        val militaryMortgage = new Mortgage(103L, mortgageBank.getId(), "Военная")
        militaryMortgage.setMilitaryMortgage(true)
        Set(familyMortgage, stateMortgage, militaryMortgage).map(prepareForMortgageProgram)
      }),
      ("site with partnerBanks and mortgage programs with eastern", {
        val site = new Site(1)
        val location = new Location()
        location.setSubjectFederation(Regions.PRIMORSKIY_KRAY, 8979876L)
        site.setLocation(location)
        site.setPartnerBanks(Seq(mortgageBankId, otherBankId).map(Long.box).asJava)
        site
      }, Set[Mortgage](), {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        val easternMortgage = new Mortgage(104L, mortgageBank.getId(), "Дальневосточная")
        easternMortgage.setMilitaryMortgage(false)
        Set(familyMortgage, stateMortgage, easternMortgage).map(prepareForMortgageProgram)
      }),
      ("site with partnerBanks and mortgage programs with millitary and eastern", {
        val site = new Site(1)
        val location = new Location()
        location.setSubjectFederation(Regions.RESPUBLICA_SAHA, 122223L)
        site.setLocation(location)
        site.setHasMilitaryMortgage(true)
        site.setPartnerBanks(Seq(mortgageBankId, 123432423L).map(Long.box).asJava)
        site
      }, Set[Mortgage](), {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        val militaryMortgage = new Mortgage(103L, mortgageBank.getId(), "Военная")
        militaryMortgage.setMilitaryMortgage(true)
        val easternMortgage = new Mortgage(104L, mortgageBank.getId(), "Дальневосточная")
        easternMortgage.setMilitaryMortgage(false)
        Set(familyMortgage, stateMortgage, militaryMortgage, easternMortgage).map(prepareForMortgageProgram)
      }),
      ("site with partnerBanks and mortgage programs and spec offers filtered by partner bank", {
        val site = new Site(1)
        site.setPartnerBanks(Seq(mortgageBankId).map(Long.box).asJava)
        val mortgageSpecialProposal = new MortgageSpecialProposal(51L, "Ипотека")
        mortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_STANDARD)
        site.setMortgageSpecialProposals(Seq(mortgageSpecialProposal).asJava)
        site
      }, Set[Mortgage](), {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        Set(familyMortgage, stateMortgage).map(prepareForMortgageProgram)
      }),
      ("site with partnerBanks and mortgage programs and spec offers filtered by other partner bank", {
        val site = new Site(1)
        site.setPartnerBanks(Seq(otherBankId).map(Long.box).asJava)
        val mortgageSpecialProposal = new MortgageSpecialProposal(51L, "Ипотека")
        mortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_STANDARD)
        site.setMortgageSpecialProposals(Seq(mortgageSpecialProposal).asJava)
        site
      }, {
        val standardMortgage = new Mortgage(51L, otherBank.getId(), "Ипотека")
        standardMortgage.setMilitaryMortgage(false)
        Set(standardMortgage)
      }, Set[Mortgage]()),
      ("site with partnerBanks and mortgage programs and spec offers", {
        val site = new Site(1)
        site.setPartnerBanks(Seq(mortgageBankId, otherBankId).map(Long.box).asJava)
        val mortgageSpecialProposal = new MortgageSpecialProposal(51L, "Ипотека")
        mortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_STANDARD)
        val millitaryMortgageSpecialProposal = new MortgageSpecialProposal(52L, "Военная ипотека из спец словаря")
        millitaryMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        millitaryMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_MILITARY)
        val easternMortgageSpecialProposal = new MortgageSpecialProposal(53L, "Дальневосточная ипотека из спец словаря")
        easternMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        easternMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_EASTERN)
        site.setMortgageSpecialProposals(
          Seq(mortgageSpecialProposal, millitaryMortgageSpecialProposal, easternMortgageSpecialProposal).asJava
        )
        site
      }, {
        val standardMortgage = new Mortgage(51L, otherBank.getId(), "Ипотека")
        standardMortgage.setMilitaryMortgage(false)
        Set(standardMortgage)
      }, {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        Set(familyMortgage, stateMortgage).map(prepareForMortgageProgram)
      }),
      ("site with partnerBanks and mortgage programs and spec offers for millitary", {
        val site = new Site(1)
        site.setHasMilitaryMortgage(true)
        site.setPartnerBanks(Seq(mortgageBankId, otherBankId).map(Long.box).asJava)
        val mortgageSpecialProposal = new MortgageSpecialProposal(51L, "Ипотека")
        mortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_STANDARD)
        val millitaryMortgageSpecialProposal = new MortgageSpecialProposal(52L, "Военная ипотека из спец словаря")
        millitaryMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        millitaryMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_MILITARY)
        val easternMortgageSpecialProposal = new MortgageSpecialProposal(53L, "Дальневосточная ипотека из спец словаря")
        easternMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        easternMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_EASTERN)
        site.setMortgageSpecialProposals(
          Seq(mortgageSpecialProposal, millitaryMortgageSpecialProposal, easternMortgageSpecialProposal).asJava
        )
        site
      }, {
        val standardMortgage = new Mortgage(51L, otherBank.getId(), "Ипотека")
        standardMortgage.setMilitaryMortgage(false)
        val militaryMortgageFromSpecOffers = new Mortgage(52L, otherBank.getId(), "Военная ипотека из спец словаря")
        militaryMortgageFromSpecOffers.setMilitaryMortgage(true)
        Set(standardMortgage, militaryMortgageFromSpecOffers)
      }, {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        val militaryMortgage = new Mortgage(103L, mortgageBank.getId(), "Военная")
        militaryMortgage.setMilitaryMortgage(true)
        Set(familyMortgage, stateMortgage, militaryMortgage).map(prepareForMortgageProgram)
      }),
      ("site with partnerBanks and mortgage programs and spec offers for millitary and eastern", {
        val site = new Site(1)
        site.setHasMilitaryMortgage(true)
        val location = new Location()
        location.setSubjectFederation(Regions.SAHALINSKAYA_OBLAST, 9875L)
        site.setLocation(location)
        site.setPartnerBanks(Seq(mortgageBankId, otherBankId).map(Long.box).asJava)
        val mortgageSpecialProposal = new MortgageSpecialProposal(51L, "Ипотека")
        mortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_STANDARD)
        val millitaryMortgageSpecialProposal = new MortgageSpecialProposal(52L, "Военная ипотека из спец словаря")
        millitaryMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        millitaryMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_MILITARY)
        val easternMortgageSpecialProposal = new MortgageSpecialProposal(53L, "Дальневосточная ипотека из спец словаря")
        easternMortgageSpecialProposal.setBankIds(Collections.singletonList(otherBankId))
        easternMortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_EASTERN)
        site.setMortgageSpecialProposals(
          Seq(mortgageSpecialProposal, millitaryMortgageSpecialProposal, easternMortgageSpecialProposal).asJava
        )
        site
      }, {
        val standardMortgage = new Mortgage(51L, otherBank.getId(), "Ипотека")
        standardMortgage.setMilitaryMortgage(false)
        val militaryMortgageFromSpecOffers = new Mortgage(52L, otherBank.getId(), "Военная ипотека из спец словаря")
        militaryMortgageFromSpecOffers.setMilitaryMortgage(true)
        val easternMortgageFromSpecOffers =
          new Mortgage(53L, otherBank.getId(), "Дальневосточная ипотека из спец словаря")
        easternMortgageFromSpecOffers.setMilitaryMortgage(false)
        Set(standardMortgage, militaryMortgageFromSpecOffers, easternMortgageFromSpecOffers)
      }, {
        val familyMortgage = new Mortgage(101L, mortgageBank.getId(), "Семейная")
        familyMortgage.setMilitaryMortgage(false)
        val stateMortgage = new Mortgage(102L, mortgageBank.getId(), "Гос поддержка")
        stateMortgage.setMilitaryMortgage(false)
        val militaryMortgage = new Mortgage(103L, mortgageBank.getId(), "Военная")
        militaryMortgage.setMilitaryMortgage(true)
        val easternMortgage = new Mortgage(104L, mortgageBank.getId(), "Дальневосточная")
        easternMortgage.setMilitaryMortgage(false)
        Set(familyMortgage, stateMortgage, militaryMortgage, easternMortgage).map(prepareForMortgageProgram)
      }),
      ("site with special offer with no bank set", {
        val site = new Site(1)
        site.setPartnerBanks(Seq(otherBankId).map(Long.box).asJava)
        val mortgageSpecialProposal = new MortgageSpecialProposal(51L, "Ипотека")
        mortgageSpecialProposal.setMortgageType(Mortgages.MortgageType.M_STANDARD)
        site.setMortgageSpecialProposals(Seq(mortgageSpecialProposal).asJava)
        site
      }, {
        val standardMortgage = new Mortgage(51L, null, "Ипотека")
        standardMortgage.setMilitaryMortgage(false)
        Set(standardMortgage)
      }, Set[Mortgage]())
    )

  "SiteMortgagesEnricher" should {
    "set mortgages for empty sites collection" in {
      SiteMortgagesEnricher.filterMortgageSpecialProposals(Collections.emptyList())
      SiteMortgagesEnricher.setSiteMortgages(Collections.emptyList(), new BankStorageImpl(Seq.empty))
    }
    forAll(siteMortgageTestData) {
      (description: String, site: Site, expectedMortgages: Set[Mortgage], expectedBankMortgages: Set[Mortgage]) =>
        "set mortgages for " + description in {
          SiteMortgagesEnricher.filterMortgageSpecialProposals(Collections.singletonList(site))
          SiteMortgagesEnricher.setSiteMortgages(Collections.singletonList(site), bankStorage)

          site.getSiteMortgages.asScala.toSet shouldBe expectedMortgages
          site.getSiteBankMortgages.asScala.toSet shouldBe expectedBankMortgages
        }
    }
    "set data from testing" in {
      val site = new Site(1L)
      val proposal1 = new MortgageSpecialProposal(2879755L, "Ипотека 5% на покупку апартаментов")
      proposal1.setFullDescription(
        "Субсидированная ипотека 5% на покупку апартаментов. Совместно с ПАО Сбербанк (№1481). Ставка на первый год  - 5%, далее 16.9%. ПВ 20%, срок до 20 лет."
      )
      proposal1.setObjectType("special-rate")
      proposal1.setInitialPayment(20f)
      proposal1.setRate(5)

      val proposal2 = new MortgageSpecialProposal(2921399L, "Ипотека 5% на покупку квартир")
      proposal2.setFullDescription(
        "Субсидированная ипотека 5% на покупку квартир. Совместно с ПАО Сбербанк (№1481).  Ставка на первые два года – 5%, первоначальный взнос 15%. Ставка с третьего года 11.7%. Срок кредита до 20 лет. Сумма кредита до 12000000 руб."
      )
      proposal2.setObjectType("standard-mortgage")
      proposal2.setInitialPayment(15f)
      proposal2.setRate(5)
      proposal2.setProposalEndDate(new org.joda.time.DateTime(1656536400000L))
      site.setMortgageSpecialProposals(java.util.List.of(proposal1, proposal2))

      SiteMortgagesEnricher.filterMortgageSpecialProposals(Collections.singletonList(site))
      SiteMortgagesEnricher.setSiteMortgages(Collections.singletonList(site), new BankStorageImpl(Seq.empty))

      site.getSiteMortgages.size() shouldBe 2
      site.getSiteBankMortgages.size() shouldBe 0
    }
  }

  private def prepareForMortgageProgram(mortgage: Mortgage): Mortgage = {
    mortgage.setMinRate(0f)
    mortgage.setMinFirstPayment(0)
    mortgage.setMinDurationMonths(0)
    mortgage.setMaxDurationMonths(0)
    mortgage.setMinAmount(0)
    mortgage.setMaxAmount(0)
    mortgage
  }

}
