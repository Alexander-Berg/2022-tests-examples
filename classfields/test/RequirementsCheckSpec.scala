package ru.yandex.vertis.shark.controller.requirements

import cats.syntax.option._
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.zio_baker.model.GeobaseId
import ru.yandex.vertis.shark.Mock.resourceRegionsDictionaryLayer
import ru.yandex.vertis.shark.model.CheckRequirements
import ru.yandex.vertis.shark.model.CheckRequirements._
import ru.yandex.vertis.shark.controller.TestSyntax._
import ru.yandex.vertis.shark.model.{
  Arbitraries,
  AutoruCreditApplication,
  ConsumerCreditProduct,
  CreditApplication,
  CreditProduct,
  Tag
}
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.proto.model.Block.IncomeBlock.IncomeProof
import ru.yandex.vertis.shark.proto.model.CreditProduct.BorrowerConditions.EmploymentType
import ru.yandex.vertis.shark.util.GeobaseUtils.MoscowGeobaseId
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResourceSpecBase._
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object RequirementsCheckSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  private val requirementsCheckLayer = resourceRegionsDictionaryLayer >>> RequirementsCheck.live

  private def borrowerConditions =
    CreditProduct
      .BorrowerConditions(
        employmentTypes = Seq(EmploymentType.EMPLOYEE),
        ageRange = CreditProduct.BorrowerConditions
          .AgeRange(
            from = 20.taggedWith[Tag.YearAmount].some,
            to = 30.taggedWith[Tag.YearAmount].some
          )
          .some,
        minLastExperienceMonths = 10.taggedWith[Tag.MonthAmount],
        allExperienceMonths = 14.taggedWith[Tag.MonthAmount],
        incomeAfterExpenses = 50000L.taggedWith[Tag.MoneyRub],
        proofs = Seq(IncomeProof.BY_2NDFL),
        incomeWithoutExpenses = 50000L.taggedWith[Tag.MoneyRub],
        checkAgeWithoutTerm = false,
        exactlyRequirements = false,
        exactlyRequirementsTerm = false,
        geobaseIds = Seq.empty,
        score = None
      )

  private def sampleCreditProduct(): ConsumerCreditProduct =
    Arbitraries
      .generate[ConsumerCreditProduct]
      .sample
      .get
      .copy(rateLimit = None)

  private def sampleCreditApplication(): AutoruCreditApplication =
    Arbitraries.generate[AutoruCreditApplication].sample.get

  private case class CheckGeoTestCase(
      description: String,
      geobaseIds: Seq[GeobaseId],
      creditProduct: CreditProduct,
      expected: Boolean)

  private case class CheckMatchingTestCase(
      description: String,
      geobaseIds: Seq[GeobaseId],
      creditApplication: CreditApplication,
      creditProduct: CreditProduct,
      expected: CheckRequirements)

  private val checkGeoTestCases: Seq[CheckGeoTestCase] = {
    val excludedGeobaseIds = Seq(
      CrimeaRegionId,
      ChechenRegionId,
      KarachayCherkessRegionId
    )
    val creditProduct = sampleCreditProduct().copy(
      geobaseIds = Seq(RussiaRegionId),
      excludedGeobaseIds = excludedGeobaseIds
    )
    Seq(
      CheckGeoTestCase(
        description = "Check correct region existence",
        geobaseIds = Seq(
          MoscowRegionId,
          SpbRegionId,
          CrimeaRegionId
        ),
        creditProduct = creditProduct,
        expected = true
      ),
      CheckGeoTestCase(
        description = "Check excluded regions only",
        geobaseIds = Seq(
          CrimeaRegionId,
          ChechenRegionId,
          KarachayCherkessRegionId
        ),
        creditProduct = creditProduct,
        expected = false
      ),
      CheckGeoTestCase(
        description = "Check regions inside excluded",
        geobaseIds = Seq(
          YaltaRegionId,
          GroznyRegionId,
          CherkesskRegionId
        ),
        creditProduct = creditProduct,
        expected = false
      ),
      CheckGeoTestCase(
        description = "Check specific correct region existence",
        geobaseIds = Seq(
          MoscowRegionId,
          SpbRegionId,
          CrimeaRegionId
        ),
        creditProduct = sampleCreditProduct().copy(
          geobaseIds = Seq.empty,
          excludedGeobaseIds = excludedGeobaseIds,
          specificBorrowerConditions = Seq(
            borrowerConditions.copy(
              geobaseIds = Seq(RussiaRegionId)
            )
          )
        ),
        expected = true
      )
    )
  }

  private val checkGeoTests = checkGeoTestCases.map {
    case CheckGeoTestCase(description, geobaseIds, creditProduct, expected) =>
      testM(description) {
        assertM(RequirementsCheck.checkGeo(geobaseIds, creditProduct))(equalTo(expected))
      }
  }

  private val checkRequirementsTestCases: Seq[CheckMatchingTestCase] = {
    val sampleRequirements = CreditApplication.Requirements(
      maxAmount = 1000000L.taggedWith[Tag.MoneyRub],
      initialFee = 111112L.taggedWith[Tag.MoneyRub],
      termMonths = 36.taggedWith[Tag.MonthAmount],
      geobaseIds = Seq.empty
    )
    val creditProduct = sampleCreditProduct().copy(
      geobaseIds = Seq(MoscowGeobaseId),
      amountRange = CreditProduct.AmountRange(10_000L.taggedWith.some, 2_000_000L.taggedWith.some),
      interestRateRange = CreditProduct.InterestRateRange(8f.taggedWith.some, 10f.taggedWith.some),
      termMonthsRange = CreditProduct.TermMonthsRange(6.taggedWith.some, 60.taggedWith.some),
      minInitialFeeRate = 10f.taggedWith[Tag.Rate],
      borrowerConditions = None
    )
    Seq(
      CheckMatchingTestCase(
        description = "all matching",
        geobaseIds = Seq(MoscowGeobaseId),
        creditApplication = sampleCreditApplication().copy(
          requirements = sampleRequirements.some
        ),
        creditProduct = creditProduct,
        expected = CheckRequirements.forTest(Matched, term = true, geo = true)
      ),
      CheckMatchingTestCase(
        description = "partial term matching",
        geobaseIds = Seq(MoscowGeobaseId),
        creditApplication = sampleCreditApplication().copy(
          requirements = sampleRequirements.copy(termMonths = 66.taggedWith).some
        ),
        creditProduct = creditProduct,
        expected = CheckRequirements.forTest(
          PartialMatched(requirements = sampleRequirements.copy(termMonths = 60.taggedWith)),
          term = true,
          geo = true
        )
      )
    )
  }

  private val checkRequirementsTests = checkRequirementsTestCases.map {
    case CheckMatchingTestCase(description, geobaseIds, creditApplication, creditProduct, expected) =>
      testM(description) {
        assertM(RequirementsCheck.check(geobaseIds, creditApplication, creditProduct))(equalTo(expected))
      }
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("RequirementsCheckSpec")(
      suite("checkGeo")(checkGeoTests: _*),
      suite("checkRequirements")(checkRequirementsTests: _*)
    ).provideLayerShared(requirementsCheckLayer)
}
