package ru.yandex.vertis.shark.controller.impl

import cats.syntax.option._
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.Mock.resourceRegionsDictionaryLayer
import ru.yandex.vertis.shark.model.CheckRequirements._
import ru.yandex.vertis.shark.controller._
import ru.yandex.vertis.shark.controller.TestSyntax._
import ru.yandex.vertis.shark.controller.requirements.RequirementsCheck
import ru.yandex.vertis.shark.controller.CreditProductCalculator.CreditProductWithSuitable
import ru.yandex.vertis.shark.controller.credit_product_rate_counter.testkit.CreditProductRateCounterMock
import ru.yandex.vertis.shark.converter.protobuf.Implicits._
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.Block.{BirthDateBlock, ExpensesBlock, IncomeBlock}
import ru.yandex.vertis.shark.model.BlockDependency._
import ru.yandex.vertis.shark.model.CreditProduct.BorrowerConditions.AgeRange
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.CreditProduct.BorrowerConditions
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.proto.model.Block.BlockType
import ru.yandex.vertis.shark.proto.model.Block.IncomeBlock.IncomeProof
import ru.yandex.vertis.shark.proto.model.CreditProduct.BorrowerConditions.EmploymentType
import ru.yandex.vertis.shark.proto.model.Entity.PhoneEntity.PhoneType
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.shark.util.GeobaseUtils
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoSyntax._
import ru.yandex.vertis.zio_baker.zio.resource.impl.RegionsResourceSpecBase._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.mock.{mockable, Expectation}
import zio.Has

import java.time.LocalDate
import java.time.Instant

object CreditProductCalculatorSpec extends DefaultRunnableSpec {

  import org.scalacheck.magnolia._

  private val creditProductRateCounterMock = CreditProductRateCounterMock
    .ActualCount(
      anything,
      Expectation.value(10.taggedWith[Tag.RequestRate])
    )
    .optional

  private val requirementsCheckLayer = resourceRegionsDictionaryLayer >>> RequirementsCheck.live

  private val calculatorLayer = resourceRegionsDictionaryLayer ++ creditProductRateCounterMock ++
    requirementsCheckLayer ++ TestClock.any >>> CreditProductCalculator.live

  private val today = LocalDate.of(1970, 1, 1)

  private def sampleConsumerCreditProduct(): ConsumerCreditProduct =
    Arbitraries
      .generate[ConsumerCreditProduct]
      .sample
      .get
      .copy(rateLimit = None)

  private def sampleAutoCreditProduct(): AutoCreditProduct =
    Arbitraries
      .generate[AutoCreditProduct]
      .sample
      .get
      .copy(rateLimit = None)

  private def sampleIFrameCreditProduct(): IFrameCreditProduct =
    Arbitraries
      .generate[IFrameCreditProduct]
      .sample
      .get
      .copy(rateLimit = None)

  private def sampleCreditApplication(): AutoruCreditApplication =
    Arbitraries.generate[AutoruCreditApplication].sample.get

  private def sampleOkbStatementAgreementBlock(): Block.OkbStatementAgreementBlock =
    Arbitraries.generate[Block.OkbStatementAgreementBlock].sample.get
  private def sampleControlWordBlock(): Block.ControlWordBlock = Arbitraries.generate[Block.ControlWordBlock].sample.get
  private def sampleNameBlock(): Block.NameBlock = Arbitraries.generate[Block.NameBlock].sample.get
  private def samplePassportRfBlock(): Block.PassportRfBlock = Arbitraries.generate[Block.PassportRfBlock].sample.get
  private def sampleEmployeeEmploymentBlock() = Arbitraries.generate[Block.EmployeeEmploymentBlock].sample.get
  private def sampleSelfEmployedEmploymentBlock() = Arbitraries.generate[Block.SelfEmployedEmploymentBlock].sample.get

  private def sampleRelatedPersonsBlock(): Block.RelatedPersonsBlock =
    Arbitraries.generate[Block.RelatedPersonsBlock].sample.get

  private def sampleApplicationOffer() = Arbitraries.generate[AutoruCreditApplication.Offer].sample.get

  private val okbStatementAgreementBlock = sampleOkbStatementAgreementBlock()
  private val controlWordBlock = sampleControlWordBlock()
  private val nameBlock = sampleNameBlock()
  private val passportRfBlock = samplePassportRfBlock()
  private val relatedPersonsBlock = sampleRelatedPersonsBlock()
  private val SortedPriorityTags: Set[proto.CreditProduct.PriorityTag] = proto.CreditProduct.PriorityTag.values.toSet

  sealed trait TestCase {

    def toSpec: ZSpec[Has[CreditProductCalculator.Service], Throwable]
  }

  private case class DependenciesTestCase(
      description: String,
      creditProducts: Seq[CreditProduct],
      expected: CreditProductDependencies)
    extends TestCase {

    override def toSpec: ZSpec[Has[CreditProductCalculator.Service], Throwable] =
      testM(description) {
        assertM(CreditProductCalculator.dependencies(creditProducts))(equalTo(expected))
      }
  }

  private case class SuitableTestCase(
      description: String,
      creditApplication: CreditApplication,
      creditProduct: CreditProduct,
      expected: Suitable)
    extends TestCase {

    override def toSpec: ZSpec[Has[CreditProductCalculator.Service], Throwable] =
      testM(description) {
        assertM(CreditProductCalculator.suitable(creditApplication, creditProduct, rateLimiter = true)) {
          equalTo(expected)
        }
      }
  }

  private case class CheckObjectTestCase(
      description: String,
      objectPayload: Option[CommonApplicationObjectPayload],
      creditProduct: CreditProduct,
      expected: Boolean)
    extends TestCase {

    override def toSpec: ZSpec[Has[CreditProductCalculator.Service], Throwable] =
      testM(description) {
        assertM(CreditProductCalculator.checkObject(objectPayload, creditProduct))(equalTo(expected))
      }
  }

  private case class PriorityTagsTestCase(
      description: String,
      data: PriorityTagsData,
      expected: Set[proto.CreditProduct.PriorityTag])
    extends TestCase {

    override def toSpec: ZSpec[Has[CreditProductCalculator.Service], Throwable] =
      testM(description) {
        assertM(CreditProductCalculator.priorityTags(data))(equalTo(expected))
      }
  }

  private case class SortCreditProductsTestCase(
      description: String,
      creditProducts: Seq[CreditProduct],
      suitables: Seq[Suitable],
      priorityTags: Set[proto.CreditProduct.PriorityTag],
      scores: Seq[YandexProductScore.ProductEntity],
      expected: Seq[CreditProduct])
    extends TestCase {

    override def toSpec: ZSpec[Has[CreditProductCalculator.Service], Throwable] =
      testM(description) {
        assertM(CreditProductCalculator.sortCreditProducts(creditProducts, suitables, priorityTags, scores)) {
          equalTo(expected)
        }
      }
  }

  private case class CalculateActivationTestCase(
      description: String,
      creditApplication: CreditApplication,
      creditProducts: Seq[CreditProduct],
      expected: Seq[CreditProductWithSuitable])
    extends TestCase {

    override def toSpec: ZSpec[Has[CreditProductCalculator.Service], Throwable] =
      testM(description) {
        assertM(CreditProductCalculator.calculateActivation(creditApplication, creditProducts))(equalTo(expected))
      }
  }

  private val dependenciesTestCases: Seq[DependenciesTestCase] = Seq(
    DependenciesTestCase(
      description = "Block always allowed and required",
      creditProducts = Seq(
        sampleConsumerCreditProduct().copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            ),
            BlockDependency(
              blockType = BlockType.GENDER,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            )
          )
        ),
        sampleConsumerCreditProduct().copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            )
          )
        )
      ),
      expected = CreditProductDependencies(
        creditApplicationInfoBlockDependencies = Seq(
          BlockDependency(
            blockType = BlockType.OKB_STATEMENT_AGREEMENT,
            allowedCondition = None,
            requiredCondition = AlwaysCondition.some
          ),
          BlockDependency(
            blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
            allowedCondition = AlwaysCondition.some,
            requiredCondition = None
          )
        ),
        borrowerPersonProfileBlockDependencies = Seq(
          BlockDependency(
            blockType = BlockType.NAME,
            allowedCondition = None,
            requiredCondition = AlwaysCondition.some
          ),
          BlockDependency(
            blockType = BlockType.GENDER,
            allowedCondition = AlwaysCondition.some,
            requiredCondition = None
          )
        )
      )
    ),
    DependenciesTestCase(
      description = "Block allowed and required with byBlockValueCondition and alwaysCondition",
      creditProducts = Seq(
        sampleConsumerCreditProduct().copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(proto.Block.ControlWordBlock()))
              ).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(proto.Block.ControlWordBlock()))
              ).some
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition =
                ByBlockValueCondition(Seq(proto.Block.BlockEntity().withGender(proto.Block.GenderBlock()))).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.PASSPORT_RF,
              allowedCondition = None,
              requiredCondition =
                ByBlockValueCondition(Seq(proto.Block.BlockEntity().withGender(proto.Block.GenderBlock()))).some
            )
          )
        ),
        sampleConsumerCreditProduct().copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.PASSPORT_RF,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            )
          )
        )
      ),
      expected = CreditProductDependencies(
        creditApplicationInfoBlockDependencies = Seq(
          BlockDependency(
            blockType = BlockType.OKB_STATEMENT_AGREEMENT,
            allowedCondition = AlwaysCondition.some,
            requiredCondition = None
          ),
          BlockDependency(
            blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
            allowedCondition = None,
            requiredCondition = AlwaysCondition.some
          )
        ),
        borrowerPersonProfileBlockDependencies = Seq(
          BlockDependency(
            blockType = BlockType.NAME,
            allowedCondition = AlwaysCondition.some,
            requiredCondition = None
          ),
          BlockDependency(
            blockType = BlockType.PASSPORT_RF,
            allowedCondition = None,
            requiredCondition = AlwaysCondition.some
          )
        )
      )
    ),
    DependenciesTestCase(
      description = "Merging byBlockValueCondition's",
      creditProducts = Seq(
        sampleConsumerCreditProduct().copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(proto.Block.ControlWordBlock()))
              ).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(proto.Block.ControlWordBlock()))
              ).some
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition =
                ByBlockValueCondition(Seq(proto.Block.BlockEntity().withGender(proto.Block.GenderBlock()))).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.PASSPORT_RF,
              allowedCondition = None,
              requiredCondition =
                ByBlockValueCondition(Seq(proto.Block.BlockEntity().withGender(proto.Block.GenderBlock()))).some
            )
          )
        ),
        sampleConsumerCreditProduct().copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = ByBlockValueCondition(
                Seq(
                  proto.Block.BlockEntity().withAdvertStatementAgreement(proto.Block.AdvertStatementAgreementBlock())
                )
              ).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withOkbStatementAgreement(proto.Block.OkbStatementAgreementBlock()))
              ).some
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition =
                ByBlockValueCondition(Seq(proto.Block.BlockEntity().withBirthDate(proto.Block.BirthDateBlock()))).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.PASSPORT_RF,
              allowedCondition = None,
              requiredCondition =
                ByBlockValueCondition(Seq(proto.Block.BlockEntity().withBirthDate(proto.Block.BirthDateBlock()))).some
            )
          )
        )
      ),
      expected = CreditProductDependencies(
        creditApplicationInfoBlockDependencies = Seq(
          BlockDependency(
            blockType = BlockType.OKB_STATEMENT_AGREEMENT,
            allowedCondition = ByBlockValueCondition(
              Seq(
                proto.Block.BlockEntity().withControlWord(proto.Block.ControlWordBlock()),
                proto.Block.BlockEntity().withAdvertStatementAgreement(proto.Block.AdvertStatementAgreementBlock())
              )
            ).some,
            requiredCondition = None
          ),
          BlockDependency(
            blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
            allowedCondition = None,
            requiredCondition = ByBlockValueCondition(
              Seq(
                proto.Block.BlockEntity().withControlWord(proto.Block.ControlWordBlock()),
                proto.Block.BlockEntity().withOkbStatementAgreement(proto.Block.OkbStatementAgreementBlock())
              )
            ).some
          )
        ),
        borrowerPersonProfileBlockDependencies = Seq(
          BlockDependency(
            blockType = BlockType.NAME,
            allowedCondition = ByBlockValueCondition(
              Seq(
                proto.Block.BlockEntity().withGender(proto.Block.GenderBlock()),
                proto.Block.BlockEntity().withBirthDate(proto.Block.BirthDateBlock())
              )
            ).some,
            requiredCondition = None
          ),
          BlockDependency(
            blockType = BlockType.PASSPORT_RF,
            allowedCondition = None,
            requiredCondition = ByBlockValueCondition(
              Seq(
                proto.Block.BlockEntity().withGender(proto.Block.GenderBlock()),
                proto.Block.BlockEntity().withBirthDate(proto.Block.BirthDateBlock())
              )
            ).some
          )
        )
      )
    )
  )

  private lazy val suitableTestCases: Seq[SuitableTestCase] = {
    val requirements = CreditApplication.Requirements(
      maxAmount = 1000000L.taggedWith[Tag.MoneyRub],
      initialFee = 111112L.taggedWith[Tag.MoneyRub],
      termMonths = 36.taggedWith[Tag.MonthAmount],
      geobaseIds = Seq.empty
    )
    val creditProduct = sampleConsumerCreditProduct().copy(
      geobaseIds = Seq(GeobaseUtils.MoscowGeobaseId),
      amountRange = CreditProduct.AmountRange(10_000L.taggedWith.some, 2_000_000L.taggedWith.some),
      interestRateRange = CreditProduct.InterestRateRange(8f.taggedWith.some, 10f.taggedWith.some),
      termMonthsRange = CreditProduct.TermMonthsRange(6.taggedWith.some, 60.taggedWith.some),
      minInitialFeeRate = 10f.taggedWith[Tag.Rate],
      borrowerConditions = None
    )
    val autoCreditProduct = sampleAutoCreditProduct().copy(
      geobaseIds = Seq(GeobaseUtils.MoscowGeobaseId),
      amountRange = CreditProduct.AmountRange(10_000L.taggedWith.some, 2_000_000L.taggedWith.some),
      interestRateRange = CreditProduct.InterestRateRange(8f.taggedWith.some, 10f.taggedWith.some),
      termMonthsRange = CreditProduct.TermMonthsRange(6.taggedWith.some, 60.taggedWith.some),
      minInitialFeeRate = 10f.taggedWith[Tag.Rate],
      borrowerConditions = None
    )
    val borrowerConditions = CreditProduct.BorrowerConditions(
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

    Seq(
      SuitableTestCase(
        description = "Without required blocks",
        creditApplication = {
          val ca = sampleCreditApplication()
          ca.copy(
            requirements = requirements.some,
            info = None,
            borrowerPersonProfile = None
          )
        },
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Required by AlwaysCondition blocks",
        creditApplication = {
          val ca = sampleCreditApplication()
          ca.copy(
            requirements = requirements.some,
            info = CreditApplication.Info
              .forTest(okbStatementAgreement = okbStatementAgreementBlock.some, advertStatementAgreementBlock = None)
              .some,
            borrowerPersonProfile = PersonProfileImpl
              .forTest(
                name = nameBlock.some,
                gender = None,
                relatedPersons = relatedPersonsBlock.some
              )
              .some
          )
        },
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            ),
            BlockDependency(
              blockType = BlockType.GENDER,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            )
          )
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq(BlockType.ADVERT_STATEMENT_AGREEMENT),
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq(BlockType.GENDER),
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Required by ByBlockValueCondition blocks",
        creditApplication = {
          val ca = sampleCreditApplication()
          ca.copy(
            requirements = requirements.some,
            info = CreditApplication.Info
              .forTest(
                okbStatementAgreement = okbStatementAgreementBlock.some,
                advertStatementAgreementBlock = None,
                controlWord = controlWordBlock.some
              )
              .some,
            borrowerPersonProfile = PersonProfileImpl
              .forTest(
                name = nameBlock.some,
                gender = None,
                passportRf = passportRfBlock.some,
                relatedPersons = relatedPersonsBlock.some
              )
              .some
          )
        },
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(controlWordBlock.toProtoMessage))
              ).some
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = None,
              requiredCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(controlWordBlock.toProtoMessage))
              ).some
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition = None,
              requiredCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withPassportRf(passportRfBlock.toProtoMessage))
              ).some
            ),
            BlockDependency(
              blockType = BlockType.GENDER,
              allowedCondition = None,
              requiredCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withPassportRf(passportRfBlock.toProtoMessage))
              ).some
            )
          )
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq(BlockType.ADVERT_STATEMENT_AGREEMENT),
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq(BlockType.GENDER),
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Allowed by AlwaysCondition blocks",
        creditApplication = {
          val ca = sampleCreditApplication()
          ca.copy(
            requirements = requirements.some,
            info = CreditApplication.Info
              .forTest(okbStatementAgreement = okbStatementAgreementBlock.some, advertStatementAgreementBlock = None)
              .some,
            borrowerPersonProfile = PersonProfileImpl
              .forTest(
                name = nameBlock.some,
                gender = None,
                relatedPersons = relatedPersonsBlock.some
              )
              .some
          )
        },
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.GENDER,
              allowedCondition = AlwaysCondition.some,
              requiredCondition = None
            )
          )
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq(BlockType.ADVERT_STATEMENT_AGREEMENT)
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq(BlockType.GENDER)
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Allowed by ByBlockValueCondition blocks",
        creditApplication = {
          val ca = sampleCreditApplication()
          ca.copy(
            requirements = requirements.some,
            info = CreditApplication.Info
              .forTest(
                okbStatementAgreement = okbStatementAgreementBlock.some,
                advertStatementAgreementBlock = None,
                controlWord = controlWordBlock.some
              )
              .some,
            borrowerPersonProfile = PersonProfileImpl
              .forTest(
                name = nameBlock.some,
                gender = None,
                passportRf = passportRfBlock.some,
                relatedPersons = relatedPersonsBlock.some
              )
              .some
          )
        },
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.OKB_STATEMENT_AGREEMENT,
              allowedCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(controlWordBlock.toProtoMessage))
              ).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.ADVERT_STATEMENT_AGREEMENT,
              allowedCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withControlWord(controlWordBlock.toProtoMessage))
              ).some,
              requiredCondition = None
            )
          ),
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.NAME,
              allowedCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withPassportRf(passportRfBlock.toProtoMessage))
              ).some,
              requiredCondition = None
            ),
            BlockDependency(
              blockType = BlockType.GENDER,
              allowedCondition = ByBlockValueCondition(
                Seq(proto.Block.BlockEntity().withPassportRf(passportRfBlock.toProtoMessage))
              ).some,
              requiredCondition = None
            )
          )
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq(BlockType.ADVERT_STATEMENT_AGREEMENT)
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq(BlockType.GENDER)
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Not suitable by geo",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(geobaseIds = Seq.empty).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq(SpbRegionId),
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.forTest(geo = false),
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable with overridden requirements by below amount",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements
            .copy(
              geobaseIds = Seq(MoscowRegionId),
              maxAmount = 1000L.taggedWith[Tag.MoneyRub]
            )
            .some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.forTest(matching =
            PartialMatched(
              requirements
                .copy(
                  geobaseIds = Seq(MoscowRegionId),
                  maxAmount = 10_000L.taggedWith[Tag.MoneyRub],
                  initialFee = 12_457L.taggedWith[Tag.MoneyRub]
                )
            )
          ),
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable with overridden requirements by above amount",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements
            .copy(
              geobaseIds = Seq(MoscowRegionId),
              maxAmount = 3_000_000L.taggedWith[Tag.MoneyRub],
              initialFee = 1_000_000L.taggedWith[Tag.MoneyRub]
            )
            .some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.forTest(matching =
            PartialMatched(
              requirements
                .copy(
                  geobaseIds = Seq(MoscowRegionId),
                  maxAmount = 2_000_000L.taggedWith[Tag.MoneyRub],
                  initialFee = 2_000_000L.taggedWith[Tag.MoneyRub]
                )
            )
          ),
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable with overridden requirements by initial fee",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements
            .copy(
              geobaseIds = Seq(MoscowRegionId),
              maxAmount = 2_000_000L.taggedWith[Tag.MoneyRub],
              initialFee = 0L.taggedWith[Tag.MoneyRub]
            )
            .some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.forTest(matching =
            PartialMatched(
              requirements
                .copy(
                  geobaseIds = Seq(MoscowRegionId),
                  maxAmount = 1_777_777L.taggedWith[Tag.MoneyRub],
                  initialFee = 222_223L.taggedWith[Tag.MoneyRub]
                )
            )
          ),
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Not suitable by term",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements
            .copy(
              geobaseIds = Seq(MoscowRegionId),
              termMonths = 3.taggedWith[Tag.MonthAmount]
            )
            .some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.forTest(NotMatched, term = false),
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      /*
      SuitableTestCase(
        description = "Auto.ru not suitable without relatedPerson",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          borrowerPersonProfile = PersonProfileImpl.forTest().some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq(
            BlockDependency(
              blockType = BlockType.PHONES,
              allowedCondition = None,
              requiredCondition = AlwaysCondition.some
            )
          )
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq(BlockType.PHONES, BlockType.RELATED_PERSONS),
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
       */
      SuitableTestCase(
        description = "Auto.ru suitable with additional phones",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable with borrower conditions",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 20.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleEmployeeEmploymentBlock()
                .copy(
                  lastExperienceMonths = 11.taggedWith[Tag.MonthAmount]
                )
                .some,
              birthDate = BirthDateBlock(today.minusYears(25)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
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
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Not suitable with borrower conditions",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 20.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleEmployeeEmploymentBlock()
                .copy(
                  lastExperienceMonths = 9.taggedWith[Tag.MonthAmount]
                )
                .some,
              birthDate = BirthDateBlock(today.minusYears(30)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(55000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
            .BorrowerConditions(
              employmentTypes = Seq(EmploymentType.SELF_EMPLOYED),
              ageRange = CreditProduct.BorrowerConditions
                .AgeRange(
                  from = 20.taggedWith[Tag.YearAmount].some,
                  to = 30.taggedWith[Tag.YearAmount].some
                )
                .some,
              minLastExperienceMonths = 10.taggedWith[Tag.MonthAmount],
              allExperienceMonths = 14.taggedWith[Tag.MonthAmount],
              incomeAfterExpenses = 50000L.taggedWith[Tag.MoneyRub],
              proofs = Seq(IncomeProof.BY_3NDFL),
              incomeWithoutExpenses = 50000L.taggedWith[Tag.MoneyRub],
              checkAgeWithoutTerm = false,
              exactlyRequirements = false,
              exactlyRequirementsTerm = false,
              geobaseIds = Seq.empty,
              score = None
            )
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.forTest(
            employment = false,
            age = false,
            minLastExperienceMonths = false,
            incomeAfterExpenses = false,
            proofs = false
          ),
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable self employed with empty age range",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 20.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleSelfEmployedEmploymentBlock().some,
              birthDate = BirthDateBlock(today.minusYears(25)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
            .BorrowerConditions(
              employmentTypes = Seq(EmploymentType.SELF_EMPLOYED),
              ageRange = None,
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
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable self employed with empty from age range",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 20.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleSelfEmployedEmploymentBlock().some,
              birthDate = BirthDateBlock(today.minusYears(25)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
            .BorrowerConditions(
              employmentTypes = Seq(EmploymentType.SELF_EMPLOYED),
              ageRange = AgeRange(None, 99.taggedWith[Tag.YearAmount].some).some,
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
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable self employed with empty to age range",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 20.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleSelfEmployedEmploymentBlock().some,
              birthDate = BirthDateBlock(today.minusYears(25)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
            .BorrowerConditions(
              employmentTypes = Seq(EmploymentType.SELF_EMPLOYED),
              ageRange = AgeRange(2.taggedWith[Tag.YearAmount].some, None).some,
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
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable age range with term don't pass",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 55.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleSelfEmployedEmploymentBlock().some,
              birthDate = BirthDateBlock(today.minusYears(64)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
            .BorrowerConditions(
              employmentTypes = Seq(EmploymentType.SELF_EMPLOYED),
              ageRange = AgeRange(
                20.taggedWith[Tag.YearAmount].some,
                65.taggedWith[Tag.YearAmount].some
              ).some,
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
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed.copy(age = false),
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable age range without term",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 55.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleSelfEmployedEmploymentBlock().some,
              birthDate = BirthDateBlock(today.minusYears(64)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
            .BorrowerConditions(
              employmentTypes = Seq(EmploymentType.SELF_EMPLOYED),
              ageRange = AgeRange(
                from = 20.taggedWith[Tag.YearAmount].some,
                to = 65.taggedWith[Tag.YearAmount].some
              ).some,
              minLastExperienceMonths = 10.taggedWith[Tag.MonthAmount],
              allExperienceMonths = 14.taggedWith[Tag.MonthAmount],
              incomeAfterExpenses = 50000L.taggedWith[Tag.MoneyRub],
              proofs = Seq(IncomeProof.BY_2NDFL),
              incomeWithoutExpenses = 50000L.taggedWith[Tag.MoneyRub],
              checkAgeWithoutTerm = true,
              exactlyRequirements = false,
              exactlyRequirementsTerm = false,
              geobaseIds = Seq.empty,
              score = None
            )
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable exactly requirements not match below max amount",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements
            .copy(
              termMonths = 55.taggedWith[Tag.MonthAmount],
              maxAmount = 1000L.taggedWith[Tag.MoneyRub]
            )
            .some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleSelfEmployedEmploymentBlock().some,
              birthDate = BirthDateBlock(today.minusYears(64)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = CreditProduct
            .BorrowerConditions(
              employmentTypes = Seq(EmploymentType.SELF_EMPLOYED),
              ageRange = AgeRange(
                from = 20.taggedWith[Tag.YearAmount].some,
                to = 65.taggedWith[Tag.YearAmount].some
              ).some,
              minLastExperienceMonths = 10.taggedWith[Tag.MonthAmount],
              allExperienceMonths = 14.taggedWith[Tag.MonthAmount],
              incomeAfterExpenses = 50000L.taggedWith[Tag.MoneyRub],
              proofs = Seq(IncomeProof.BY_2NDFL),
              incomeWithoutExpenses = 50000L.taggedWith[Tag.MoneyRub],
              checkAgeWithoutTerm = true,
              exactlyRequirements = true,
              exactlyRequirementsTerm = true,
              geobaseIds = Seq.empty,
              score = None
            )
            .some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.forTest(matching = NotMatched),
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable with specific borrower conditions",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements
            .copy(
              termMonths = 20.taggedWith[Tag.MonthAmount],
              geobaseIds = Seq(GroznyRegionId)
            )
            .some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              phones = Block
                .PhonesBlock(
                  Seq(
                    Entity.PhoneEntity(PhoneArb.arbitrary.sample.get, PhoneType.ADDITIONAL.some)
                  )
                )
                .some,
              employment = sampleEmployeeEmploymentBlock()
                .copy(
                  lastExperienceMonths = 11.taggedWith[Tag.MonthAmount]
                )
                .some,
              birthDate = BirthDateBlock(today.minusYears(25)).some,
              income = IncomeBlock(100000L.taggedWith[Tag.MoneyRub], IncomeProof.BY_2NDFL).some,
              expenses = ExpensesBlock(45000L.taggedWith[Tag.MoneyRub]).some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq.empty,
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = None,
          specificBorrowerConditions = Seq(
            borrowerConditions.copy(
              geobaseIds = Seq(ChechenRegionId)
            ),
            borrowerConditions.copy(
              geobaseIds = Seq(MoscowRegionId)
            )
          )
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = true,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit.Passed,
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Not suitable by rate limit",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(geobaseIds = Seq(MoscowRegionId)).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq(MoscowRegionId),
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          rateLimit = CreditProduct.RateLimit(maxClaimsPer1d = 9.taggedWith[Tag.RequestRate].some).some
        ),
        expected = Suitable(
          creditProductId = creditProduct.id,
          passed = false,
          checkRequirements = CheckRequirements.Passed,
          info = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          borrowerPersonProfile = MissingBlocks(
            required = Seq.empty,
            allowed = Seq.empty
          ),
          checkBorrower = CheckBorrower.Passed,
          checkRateLimit = CheckRateLimit(maxClaimsPer1d = false),
          checkObject = Seq.empty
        )
      ),
      SuitableTestCase(
        description = "Suitable with fulfilled score conditions",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 20.taggedWith[Tag.MonthAmount]).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq(
            EmptyYandexScore.copy(
              paymentSegment = 3.some, // Below requirement
              approvalSegment = 5.some // Exactly matches requirement
            )
          )
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  maxYandexScorePaymentSegment = 4.some,
                  maxYandexScoreApprovalSegment = 5.some
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          checkBorrower = CheckBorrower.forTest(
            score = CheckBorrower.CheckScore.Passed.some
          )
        )
      ),
      SuitableTestCase(
        description = "Suitable with unfulfilled score conditions (payment)",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq(
            EmptyYandexScore.copy(
              paymentSegment = 5.some, // Above requirement
              approvalSegment = 5.some // Exactly matches requirement
            )
          )
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  maxYandexScorePaymentSegment = 4.some,
                  maxYandexScoreApprovalSegment = 5.some
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          passed = false,
          checkBorrower = CheckBorrower.forTest(score =
            CheckBorrower.CheckScore.Passed
              .copy(
                maxYandexScorePaymentSegment = false
              )
              .some
          )
        )
      ),
      SuitableTestCase(
        description = "Suitable with unfulfilled score conditions (approval)",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq(
            EmptyYandexScore.copy(
              paymentSegment = 3.some, // Below requirement
              approvalSegment = 6.some // Above requirement
            )
          )
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  maxYandexScorePaymentSegment = 4.some,
                  maxYandexScoreApprovalSegment = 5.some
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          passed = false,
          checkBorrower = CheckBorrower.forTest(score =
            CheckBorrower.CheckScore.Passed
              .copy(
                maxYandexScoreApprovalSegment = false
              )
              .some
          )
        )
      ),
      SuitableTestCase(
        description = "Suitable with partially fulfilled score conditions",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(termMonths = 20.taggedWith[Tag.MonthAmount]).some,
          scores = Seq(
            EmptyYandexScore.copy(
              paymentSegment = 3.some, // Below requirement
              approvalSegment = none // Missing, even though the requirement is present
            )
          ),
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  maxYandexScorePaymentSegment = 4.some,
                  maxYandexScoreApprovalSegment = 5.some
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          checkBorrower = CheckBorrower.Passed.copy(score =
            // Missing score in the application is considered a match
            CheckBorrower.CheckScore.Passed.some
          )
        )
      ),
      SuitableTestCase(
        description = "Suitable with score conditions and missing score",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          scores = Seq.empty,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some
        ),
        creditProduct = creditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  maxYandexScorePaymentSegment = 4.some,
                  maxYandexScoreApprovalSegment = 5.some
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          checkBorrower = CheckBorrower.forTest(
            score = CheckBorrower.CheckScore.Passed.some
          )
        )
      ),
      SuitableTestCase(
        description = "Suitable by object",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          offers = Seq(
            sampleApplicationOffer().copy(
              sellerType = proto.AutoSellerType.PRIVATE.some
            )
          )
        ),
        creditProduct = autoCreditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          objectPayload = CreditProduct.ObjectPayload
            .Auto(
              sellerType = proto.AutoSellerType.PRIVATE.some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          creditProductId = autoCreditProduct.id,
          checkObject = Seq(CheckObject.Auto(sellerType = true))
        )
      ),
      SuitableTestCase(
        description = "Not suitable by object",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          offers = Seq(
            // The first offer matches the requirement, the second one does not.
            sampleApplicationOffer().copy(
              sellerType = proto.AutoSellerType.PRIVATE.some
            ),
            sampleApplicationOffer().copy(
              sellerType = proto.AutoSellerType.COMMERCIAL.some
            )
          )
        ),
        creditProduct = autoCreditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          objectPayload = CreditProduct.ObjectPayload
            .Auto(
              sellerType = proto.AutoSellerType.PRIVATE.some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          creditProductId = autoCreditProduct.id,
          passed = false,
          checkObject = Seq(CheckObject.Auto(sellerType = true), CheckObject.Auto(sellerType = false))
        )
      ),
      SuitableTestCase(
        description = "Suitable by object (seller type not specified)",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          offers = Seq(
            sampleApplicationOffer().copy(
              sellerType = proto.AutoSellerType.PRIVATE.some
            )
          )
        ),
        creditProduct = autoCreditProduct.copy(
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          objectPayload = CreditProduct.ObjectPayload
            .Auto(
              sellerType = None
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          creditProductId = autoCreditProduct.id,
          checkObject = Seq(CheckObject.Auto(sellerType = true))
        )
      ),
      SuitableTestCase(
        description = "Suitable with missing Yandex score & no product score requirements",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(geobaseIds = Seq(MoscowRegionId)).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq.empty
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq(MoscowRegionId),
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  yandexScoreRequired = true
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          checkBorrower = CheckBorrower.forTest(score = CheckBorrower.CheckScore.Passed.some)
        )
      ),
      SuitableTestCase(
        description = "Not suitable because of missing payment segment score field",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(geobaseIds = Seq(MoscowRegionId)).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq(
            EmptyYandexScore.copy(
              approvalSegment = 3.some
            )
          )
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq(MoscowRegionId),
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  maxYandexScorePaymentSegment = 5.some,
                  yandexScoreRequired = true
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          passed = false,
          checkBorrower = CheckBorrower.forTest(
            score = CheckBorrower.CheckScore.Passed
              .copy(
                yandexScoreRequired = false
              )
              .some
          )
        )
      ),
      SuitableTestCase(
        description = "Not suitable because of missing approval segment score field",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(geobaseIds = Seq(MoscowRegionId)).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq(
            EmptyYandexScore.copy(
              paymentSegment = 3.some
            )
          )
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq(MoscowRegionId),
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  maxYandexScoreApprovalSegment = 5.some,
                  yandexScoreRequired = true
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          passed = false,
          checkBorrower = CheckBorrower.forTest(
            score = CheckBorrower.CheckScore.Passed
              .copy(
                yandexScoreRequired = false
              )
              .some
          )
        )
      ),
      SuitableTestCase(
        description = "Suitable with required yandex score",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(geobaseIds = Seq(MoscowRegionId)).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq(
            EmptyYandexScore.copy(
              paymentSegment = 3.some,
              approvalSegment = 3.some
            )
          )
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq(MoscowRegionId),
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  yandexScoreRequired = true
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          creditProductId = creditProduct.id,
          checkBorrower = CheckBorrower.forTest(
            score = CheckBorrower.CheckScore.Passed
              .copy(
                yandexScoreRequired = true
              )
              .some
          )
        )
      ),
      SuitableTestCase(
        description = "Suitable with required yandex product score",
        creditApplication = sampleCreditApplication().copy(
          requirements = requirements.copy(geobaseIds = Seq(MoscowRegionId)).some,
          borrowerPersonProfile = PersonProfileImpl
            .forTest(
              relatedPersons = relatedPersonsBlock.some
            )
            .some,
          scores = Seq(
            EmptyYandexProductScore.copy(
              productEntities = Seq(YandexProductScore.ProductEntity(creditProduct.id, 0.5f))
            )
          )
        ),
        creditProduct = creditProduct.copy(
          geobaseIds = Seq(MoscowRegionId),
          creditApplicationInfoBlockDependencies = Seq.empty,
          borrowerPersonProfileBlockDependencies = Seq.empty,
          borrowerConditions = EmptyBorrowerConditions
            .copy(
              score = EmptyScoreConditions
                .copy(
                  minYandexProductScore = 0.4f.some,
                  yandexScoreRequired = true
                )
                .some
            )
            .some
        ),
        expected = creditProduct.toPassedSuitable.copy(
          creditProductId = creditProduct.id,
          checkBorrower = CheckBorrower.forTest(
            score = CheckBorrower.CheckScore.Passed
              .copy(
                yandexScoreRequired = true
              )
              .some
          )
        )
      )
    )
  }

  private val checkObjectTestCases: Seq[CheckObjectTestCase] = {
    val product = Arbitraries.generate[AutoCreditProduct].sample.get
    Seq(
      CheckObjectTestCase(
        "check correct seller type",
        CommonApplicationObjectPayload.Auto(proto.AutoSellerType.PRIVATE.some, None, None, None, None).some,
        product.copy(objectPayload = CreditProduct.ObjectPayload.Auto(proto.AutoSellerType.PRIVATE.some).some),
        expected = true
      ),
      CheckObjectTestCase(
        "check incorrect seller type",
        CommonApplicationObjectPayload.Auto(proto.AutoSellerType.COMMERCIAL.some, None, None, None, None).some,
        product.copy(objectPayload = CreditProduct.ObjectPayload.Auto(proto.AutoSellerType.PRIVATE.some).some),
        expected = false
      ),
      CheckObjectTestCase(
        "check unspecified seller type",
        CommonApplicationObjectPayload.Auto(None, None, None, None, None).some,
        product.copy(objectPayload = CreditProduct.ObjectPayload.Auto(proto.AutoSellerType.PRIVATE.some).some),
        expected = true
      ),
      CheckObjectTestCase(
        "check no object payload",
        None,
        product.copy(objectPayload = CreditProduct.ObjectPayload.Auto(proto.AutoSellerType.PRIVATE.some).some),
        expected = true
      ),
      CheckObjectTestCase(
        "check no seller type condition for product",
        CommonApplicationObjectPayload.Auto(proto.AutoSellerType.COMMERCIAL.some, None, None, None, None).some,
        product.copy(objectPayload = CreditProduct.ObjectPayload.Auto(None).some),
        expected = true
      ),
      CheckObjectTestCase(
        "check no object payload for product",
        CommonApplicationObjectPayload.Auto(proto.AutoSellerType.COMMERCIAL.some, None, None, None, None).some,
        product.copy(objectPayload = None),
        expected = true
      ),
      CheckObjectTestCase(
        "check not an auto credit",
        CommonApplicationObjectPayload.Auto(proto.AutoSellerType.COMMERCIAL.some, None, None, None, None).some,
        Arbitraries.generate[ConsumerCreditProduct].sample.get,
        expected = true
      )
    )
  }

  private val priorityTagsTestCases = Seq(
    PriorityTagsTestCase(
      "empty data",
      data = PriorityTagsData(amount = none),
      expected = Set.empty
    ),
    PriorityTagsTestCase(
      "below 1M",
      data = PriorityTagsData(amount = 1_000_000L.taggedWith[Tag.MoneyRub].some),
      expected = Set(proto.CreditProduct.PriorityTag.AMOUNT_BELOW_1M)
    ),
    PriorityTagsTestCase(
      "above 1M",
      data = PriorityTagsData(amount = 1_000_001L.taggedWith[Tag.MoneyRub].some),
      expected = Set(proto.CreditProduct.PriorityTag.AMOUNT_ABOVE_1M)
    )
  )

  private val sortCreditProductsTestCases = {
    val creditProducts =
      Seq.fill(3)(sampleConsumerCreditProduct()).withoutPriorityTags.withoutPriority.withoutInterestRateRange
    val equalSuitables = creditProducts.map(_.toPassedSuitable)
    val sortedSuitables = Seq(
      creditProducts(0).toPassedSuitable.copy(
        passed = false
      ),
      creditProducts(1).toPassedSuitable.copy(
        checkRequirements = CheckRequirements(
          matching = PartialMatched(
            requirements = Arbitraries.generate[CreditApplication.Requirements].sample.get
          ),
          term = Arbitraries.generate[Boolean].sample.get,
          geo = Arbitraries.generate[Boolean].sample.get
        )
      ),
      creditProducts(2).toPassedSuitable
    )
    val equalScores = creditProducts.map(_.toZeroScore)
    val sortedScores =
      for (i <- creditProducts.indices)
        yield YandexProductScore.ProductEntity(creditProductId = creditProducts(i).id, score = i.toFloat)
    Seq(
      SortCreditProductsTestCase(
        description = "Sort by suitables",
        creditProducts = creditProducts,
        suitables = sortedSuitables,
        priorityTags = SortedPriorityTags,
        scores = equalScores,
        expected = creditProducts.reverse
      ),
      SortCreditProductsTestCase(
        description = "Sort by scores",
        creditProducts = creditProducts,
        suitables = equalSuitables,
        priorityTags = SortedPriorityTags,
        scores = sortedScores,
        expected = creditProducts.reverse
      ),
      SortCreditProductsTestCase(
        description = "Sort by priority tags",
        creditProducts = creditProducts.withPriorityTags,
        suitables = equalSuitables,
        priorityTags = SortedPriorityTags,
        scores = equalScores,
        expected = creditProducts.withPriorityTags.reverse
      ),
      SortCreditProductsTestCase(
        description = "Sort by priority",
        creditProducts = creditProducts.withPriority,
        suitables = equalSuitables,
        priorityTags = SortedPriorityTags,
        scores = equalScores,
        expected = creditProducts.withPriority.reverse
      ),
      SortCreditProductsTestCase(
        description = "Sort by interest rate range",
        creditProducts = creditProducts.withInterestRateRange,
        suitables = equalSuitables,
        priorityTags = SortedPriorityTags,
        scores = equalScores,
        expected = creditProducts.withInterestRateRange.reverse
      ),
      SortCreditProductsTestCase(
        description = "Don't sort",
        creditProducts = creditProducts,
        suitables = equalSuitables,
        priorityTags = SortedPriorityTags,
        scores = equalScores,
        expected = creditProducts
      )
    )
  }

  private val calculateActivationTestCases = {
    val requirements = CreditApplication.Requirements(
      maxAmount = 1000000L.taggedWith[Tag.MoneyRub],
      initialFee = 100000L.taggedWith[Tag.MoneyRub],
      termMonths = 36.taggedWith[Tag.MonthAmount],
      geobaseIds = Seq(MoscowRegionId)
    )
    val creditApplication = sampleCreditApplication().copy(requirements = requirements.some)
    val iframes = Seq
      .fill(3)(sampleIFrameCreditProduct())
      .withoutSending
      .withSuitable
      .withoutPriorityTags
      .withPriority(initial = 10)
    val others = Seq.fill(3)(sampleConsumerCreditProduct()).withSuitable.withoutPriorityTags.withPriority
    Seq(
      CalculateActivationTestCase(
        description = "Don't calculates (without credit products)",
        creditApplication = creditApplication,
        creditProducts = Seq.empty,
        expected = Seq.empty
      ),
      CalculateActivationTestCase(
        description = "Calculates (suitable iframes)",
        creditApplication = creditApplication,
        creditProducts = iframes.take(1) ++ iframes.drop(1).withSending ++ others,
        expected = (iframes.take(1) ++ iframes.drop(1).withSending.reverse ++ others.takeRight(1)).map(_.withSuitable)
      ),
      CalculateActivationTestCase(
        description = "Calculates (suitable non-iframes)",
        creditApplication = creditApplication,
        creditProducts = others,
        expected = others.reverse.take(3).map(_.withSuitable)
      ),
      CalculateActivationTestCase(
        description = "Calculates (suitable products)",
        creditApplication = creditApplication,
        creditProducts = iframes.take(1) ++ iframes.takeRight(1).withSending ++ others,
        expected = (iframes.take(1) ++ iframes.takeRight(1).withSending ++ others.reverse.take(2)).map(_.withSuitable)
      )
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("CreditProductCalculatorImpl")(
      suite("dependencies")(dependenciesTestCases.map(_.toSpec): _*),
      suite("suitable")(suitableTestCases.map(_.toSpec): _*),
      suite("checkObject")(checkObjectTestCases.map(_.toSpec): _*),
      suite("priorityTags")(priorityTagsTestCases.map(_.toSpec): _*),
      suite("sortCreditProducts")(sortCreditProductsTestCases.map(_.toSpec): _*),
      suite("calculateActivation")(calculateActivationTestCases.map(_.toSpec): _*)
    ).provideLayerShared(calculatorLayer)

  private lazy val now: Instant = Instant.now

  private val EmptyYandexScore: YandexScore =
    YandexScore(
      paymentSegment = None,
      approvalSegment = None,
      approvalAutoSegment = None,
      bnplSegment = None,
      paymentWeight = None,
      approvalWeight = None,
      approvalAutoWeight = None,
      bnplWeight = None,
      timestamp = now,
      sourceHash = None
    )

  private val EmptyYandexProductScore: YandexProductScore =
    YandexProductScore(
      productEntities = Seq.empty,
      timestamp = now,
      sourceHash = None
    )

  private val EmptyScoreConditions: BorrowerConditions.Score =
    CreditProduct.BorrowerConditions.Score(
      maxYandexScorePaymentSegment = None,
      maxYandexScoreApprovalSegment = None,
      maxYandexScoreApprovalAutoSegment = None,
      maxYandexScoreBnplSegment = None,
      minYandexProductScore = None,
      yandexScoreRequired = false
    )

  private val EmptyBorrowerConditions =
    CreditProduct.BorrowerConditions(
      employmentTypes = Seq.empty,
      ageRange = None,
      checkAgeWithoutTerm = false,
      minLastExperienceMonths = 0.taggedWith[Tag.MonthAmount],
      allExperienceMonths = 0.taggedWith[Tag.MonthAmount],
      incomeAfterExpenses = 0L.taggedWith[Tag.MoneyRub],
      incomeWithoutExpenses = 0L.taggedWith[Tag.MoneyRub],
      proofs = Seq.empty,
      exactlyRequirements = false,
      exactlyRequirementsTerm = false,
      geobaseIds = Seq.empty,
      score = None
    )

  implicit private class RichCreditProduct(val value: CreditProduct) extends AnyVal {

    def toPassedSuitable: Suitable =
      Suitable(
        creditProductId = value.id,
        passed = true,
        checkRequirements = CheckRequirements.Passed,
        checkBorrower = CheckBorrower.Passed,
        info = MissingBlocks(
          required = Seq.empty,
          allowed = Seq.empty
        ),
        borrowerPersonProfile = MissingBlocks(
          required = Seq.empty,
          allowed = Seq.empty
        ),
        checkRateLimit = CheckRateLimit.Passed,
        checkObject = Seq.empty
      )

    def toZeroScore: YandexProductScore.ProductEntity =
      YandexProductScore.ProductEntity(value.id, score = 0f)

    def withSuitable: CreditProductWithSuitable = CreditProductWithSuitable(value, toPassedSuitable)
  }

  implicit private class RichConsumerCreditProductSeq(val values: Seq[ConsumerCreditProduct]) extends AnyVal {

    def withoutPriorityTags: Seq[ConsumerCreditProduct] =
      values.map(
        _.copy(
          priorityTags = Set.empty
        )
      )

    def withPriorityTags: Seq[ConsumerCreditProduct] =
      for (i <- values.indices)
        yield values(i).copy(
          priorityTags = SortedPriorityTags.take(i % SortedPriorityTags.size + 1)
        )

    def withoutPriority: Seq[ConsumerCreditProduct] =
      values.map(
        _.copy(
          priority = 0.taggedWith[Tag.Priority]
        )
      )

    def withPriority: Seq[ConsumerCreditProduct] =
      for (i <- values.indices)
        yield values(i).copy(
          priority = (values.size - i).taggedWith[Tag.Priority]
        )

    def withoutInterestRateRange: Seq[ConsumerCreditProduct] =
      values.map(
        _.copy(
          interestRateRange = CreditProduct.InterestRateRange(
            from = None,
            to = None
          )
        )
      )

    def withInterestRateRange: Seq[ConsumerCreditProduct] =
      for (i <- values.indices)
        yield values(i).copy(
          interestRateRange = CreditProduct.InterestRateRange(
            from = (values.size - i).toFloat.taggedWith[Tag.Rate].some,
            to = None
          )
        )

    def withSuitable: Seq[ConsumerCreditProduct] = values.map { creditProduct =>
      creditProduct.copy(
        amountRange = CreditProduct.AmountRange(
          from = 0L.taggedWith[Tag.MoneyRub].some,
          to = 10000000L.taggedWith[Tag.MoneyRub].some
        ),
        interestRateRange = CreditProduct.InterestRateRange(
          from = 0f.taggedWith[Tag.Rate].some,
          to = None
        ),
        termMonthsRange = CreditProduct.TermMonthsRange(
          from = 0.taggedWith[Tag.MonthAmount].some,
          to = 360.taggedWith[Tag.MonthAmount].some
        ),
        minInitialFeeRate = 0f.taggedWith[Tag.Rate],
        geobaseIds = Seq(RussiaRegionId),
        creditApplicationInfoBlockDependencies = Seq.empty,
        borrowerPersonProfileBlockDependencies = Seq.empty,
        isActive = true,
        borrowerConditions = None,
        excludedGeobaseIds = Seq.empty
      )
    }
  }

  implicit private class RichIframeCreditProductSeq(val values: Seq[IFrameCreditProduct]) extends AnyVal {

    private def setWithoutSending(withoutSending: Boolean): Seq[IFrameCreditProduct] =
      values.map(_.copy(withoutSending = withoutSending))

    def withoutSending: Seq[IFrameCreditProduct] = setWithoutSending(true)

    def withSending: Seq[IFrameCreditProduct] = setWithoutSending(false)

    def withoutPriorityTags: Seq[IFrameCreditProduct] =
      values.map(_.copy(priorityTags = Set.empty))

    def withPriority(initial: Int): Seq[IFrameCreditProduct] =
      for (i <- values.indices)
        yield values(i).copy(
          priority = (values.size - i - initial).taggedWith[Tag.Priority]
        )

    def withSuitable: Seq[IFrameCreditProduct] = values.map { creditProduct =>
      creditProduct.copy(
        amountRange = CreditProduct.AmountRange(
          from = 0L.taggedWith[Tag.MoneyRub].some,
          to = 10000000L.taggedWith[Tag.MoneyRub].some
        ),
        interestRateRange = CreditProduct.InterestRateRange(
          from = 0f.taggedWith[Tag.Rate].some,
          to = None
        ),
        termMonthsRange = CreditProduct.TermMonthsRange(
          from = 0.taggedWith[Tag.MonthAmount].some,
          to = 360.taggedWith[Tag.MonthAmount].some
        ),
        minInitialFeeRate = 0f.taggedWith[Tag.Rate],
        geobaseIds = Seq(RussiaRegionId),
        creditApplicationInfoBlockDependencies = Seq.empty,
        borrowerPersonProfileBlockDependencies = Seq.empty,
        isActive = true,
        borrowerConditions = None,
        excludedGeobaseIds = Seq.empty
      )
    }
  }
}
