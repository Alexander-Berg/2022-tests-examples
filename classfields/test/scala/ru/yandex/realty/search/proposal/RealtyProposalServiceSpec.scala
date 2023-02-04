package ru.yandex.realty.search.proposal

import java.util.Collections.{emptyList, singletonList}
import org.apache.commons.lang3.StringUtils.EMPTY
import org.scalacheck.Gen
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.sites.special.proposals.{Discount, Gift, Installment, RealtyProposalType, Sale}
import ru.yandex.realty.model.sites.{Mortgage, Site}
import ru.yandex.vertis.generators.ProducerProvider.asProducer

class RealtyProposalServiceSpec extends SpecBase {

  private val idGen: Gen[Long] = Gen.posNum[Long]

  private val mortgageGen: Gen[Mortgage] = for {
    id <- idGen
    bankId <- idGen
  } yield {
    val mortgage = new Mortgage(id, bankId, EMPTY)
    mortgage.setMainProposal(true)
    mortgage
  }
  private val saleGen: Gen[Sale] = for {
    id <- idGen
  } yield {
    val sale = new Sale(id, EMPTY)
    sale.setMainProposal(true)
    sale
  }
  private val giftGen: Gen[Gift] = for {
    id <- idGen
  } yield {
    val gift = new Gift(id, EMPTY)
    gift.setMainProposal(true)
    gift
  }
  private val discountGen: Gen[Discount] = for {
    id <- idGen
  } yield {
    val discount = new Discount(id, EMPTY)
    discount.setMainProposal(true)
    discount
  }
  private val installmentGen: Gen[Installment] = for {
    id <- idGen
  } yield {
    val installment = new Installment(id, EMPTY)
    installment.setMainProposal(true)
    installment
  }

  private def siteGen(
    withMortgage: Boolean = false,
    withSale: Boolean = false,
    withGift: Boolean = false,
    withDiscount: Boolean = false,
    withInstallment: Boolean = false
  ): Gen[Site] =
    for {
      siteId <- idGen
      mortgage <- if (withMortgage) {
        mortgageGen
      } else {
        Gen.const(null)
      }
      sale <- if (withSale) {
        saleGen
      } else {
        Gen.const(null)
      }
      gift <- if (withGift) {
        giftGen
      } else {
        Gen.const(null)
      }
      discount <- if (withDiscount) {
        discountGen
      } else {
        Gen.const(null)
      }
      installment <- if (withInstallment) {
        installmentGen
      } else {
        Gen.const(null)
      }
    } yield buildSite(
      siteId, { site =>
        setProposalIfExists(site, mortgage)(site.setSiteMortgages)
        setProposalIfExists(site, sale)(site.setSales)
        setProposalIfExists(site, gift)(site.setGifts)
        setProposalIfExists(site, discount)(site.setDiscounts)
        setProposalIfExists(site, installment)(site.setInstallments)
      }
    )

  private val mainProposalTestCases: Seq[MainProposalTestCase] = Seq(
    MainProposalTestCase(
      description = "return empty main proposal when site do not have main proposals",
      site = siteGen().next,
      expectedResult = Option.empty
    ),
    MainProposalTestCase(
      description = "return main discount when all kind of proposals exist",
      site = siteGen(
        withDiscount = true,
        withSale = true,
        withGift = true,
        withMortgage = true,
        withInstallment = true
      ).next,
      expectedResult = Some(
        RealtyProposalInfo(
          proposalType = RealtyProposalType.DISCOUNT,
          description = EMPTY
        )
      )
    ),
    MainProposalTestCase(
      description = "return main sale when all kind of proposals except discount exist",
      site = siteGen(
        withSale = true,
        withGift = true,
        withMortgage = true,
        withInstallment = true
      ).next,
      expectedResult = Some(
        RealtyProposalInfo(
          proposalType = RealtyProposalType.SALE,
          description = EMPTY
        )
      )
    ),
    MainProposalTestCase(
      description = "return main gift when all kind of proposals except discount and sale exist",
      site = siteGen(
        withGift = true,
        withMortgage = true,
        withInstallment = true
      ).next,
      expectedResult = Some(
        RealtyProposalInfo(
          proposalType = RealtyProposalType.GIFT,
          description = EMPTY
        )
      )
    ),
    MainProposalTestCase(
      description = "return main mortgage when only mortgages and installments exist",
      site = siteGen(
        withMortgage = true,
        withInstallment = true
      ).next,
      expectedResult = Some(
        RealtyProposalInfo(
          proposalType = RealtyProposalType.MORTGAGE,
          description = EMPTY
        )
      )
    ),
    MainProposalTestCase(
      description = "return main proposal when only one proposal exist",
      site = siteGen(
        withMortgage = true
      ).next,
      expectedResult = Some(
        RealtyProposalInfo(
          proposalType = RealtyProposalType.MORTGAGE,
          description = EMPTY
        )
      )
    )
  )

  "RealtyProposalService" should {
    mainProposalTestCases.foreach {
      case MainProposalTestCase(description, site, expectedResult) =>
        description in {
          val realtyProposalService = new RealtyProposalService

          val actualResult = realtyProposalService.getMainProposalLabel(site)

          actualResult shouldEqual (expectedResult)
        }
    }
  }

  private def buildSite(siteId: Long, builder: Site => Unit = (_: Site) => ()): Site = {
    val site = new Site(siteId)
    builder(site)
    site
  }

  private def setProposalIfExists[T](site: Site, proposal: T)(siteProposalSetter: java.util.List[T] => Unit) =
    siteProposalSetter(Option(proposal).map[java.util.List[T]](singletonList).getOrElse(emptyList()))

  private case class MainProposalTestCase(
    description: String,
    site: Site,
    expectedResult: Option[RealtyProposalInfo]
  )

}
