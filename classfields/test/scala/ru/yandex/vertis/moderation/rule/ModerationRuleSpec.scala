package ru.yandex.vertis.moderation.rule

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet, SignalsSummary}
import ru.yandex.vertis.moderation.model.realty.PriceInfo
import ru.yandex.vertis.moderation.model.signal.{AutomaticSource, SignalInfoSet, SignalSet, WarnSignal}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Signal.SignalType
import ru.yandex.vertis.moderation.proto.Model.{Reason, Service}
import ru.yandex.vertis.moderation.proto.RealtyLight.PriceInfo.Currency
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials.{CategoryType, CommercialType, OfferType}
import ru.yandex.vertis.moderation.rule.Generators._
import ru.yandex.vertis.moderation.rule.ModerationRule.SearchAttributes
import ru.yandex.vertis.moderation.rule.service.{DocumentClauseMatcher, Matcher}
import ru.yandex.vertis.moderation.searcher.core.saas.clauses.Clause
import ru.yandex.vertis.moderation.searcher.core.saas.document.{Document, DocumentBuilder}
import ru.yandex.vertis.moderation.searcher.core.saas.search.SearchClauseBuilder
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.{Globals, SpecBase}

/**
  * Spec for moderation rules
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class ModerationRuleSpec extends SpecBase {

  private def documentBuilder(service: Service): DocumentBuilder =
    new DocumentBuilder(Globals.opinionCalculator(service))

  implicit private val docClauseMatcher: Matcher[Document, Clause, MatchingOptions] = DocumentClauseMatcher
  implicit private val clauseBuilder: SearchClauseBuilder = SearchClauseBuilder

  case class TestCase(description: String,
                      service: Service,
                      searchAttributes: SearchAttributes,
                      instance: Instance,
                      expectedResult: Boolean
                     )

  val testCases =
    Seq(
      TestCase(
        description = "return true when price is in the range",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "offer_type" -> "SELL",
            "category_type" -> "APARTMENT",
            "min_price" -> "10",
            "max_price" -> "20"
          ),
        instance =
          InstanceGen.next
            .copy(essentials =
              RealtyEssentialsGen.next.copy(
                price = Some(PriceInfo(Some(Currency.RUR), Some(15), None, None)),
                offerType = Some(OfferType.SELL),
                categoryType = Some(CategoryType.APARTMENT)
              )
            ),
        expectedResult = true
      ),
      TestCase(
        description = "return false when price isn't in the range",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "offer_type" -> "SELL",
            "category_type" -> "APARTMENT",
            "min_price" -> "10",
            "max_price" -> "20"
          ),
        instance =
          InstanceGen.next
            .copy(essentials =
              RealtyEssentialsGen.next.copy(
                price = Some(PriceInfo(Some(Currency.RUR), Some(21), None, None)),
                offerType = Some(OfferType.SELL),
                categoryType = Some(CategoryType.APARTMENT)
              )
            ),
        expectedResult = false
      ),
      TestCase(
        description = "return true when object id is one of expected",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "offer_type" -> "SELL",
            "category_type" -> "COMMERCIAL",
            "min_price" -> "10",
            "max_price" -> "20",
            "object_id" -> "0,1,2",
            "commercial_types" -> "AUTO_REPAIR,BUSINESS"
          ),
        instance =
          InstanceGen.next.copy(
            id = instanceIdGen(ExternalIdGen.next.copy(objectId = "0")).next,
            essentials =
              RealtyEssentialsGen.next.copy(
                price = Some(PriceInfo(Some(Currency.RUR), Some(15), None, None)),
                offerType = Some(OfferType.SELL),
                categoryType = Some(CategoryType.COMMERCIAL),
                commercialTypes = Seq(CommercialType.AUTO_REPAIR, CommercialType.BUSINESS)
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "return false when object id isn't expected",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "offer_type" -> "SELL",
            "category_type" -> "COMMERCIAL",
            "min_price" -> "10",
            "max_price" -> "20",
            "object_id" -> "0,1,2",
            "commercial_types" -> "AUTO_REPAIR,BUSINESS"
          ),
        instance =
          InstanceGen.next.copy(
            id = instanceIdGen(ExternalIdGen.next.copy(objectId = "3")).next,
            essentials =
              RealtyEssentialsGen.next.copy(
                price = Some(PriceInfo(Some(Currency.RUR), Some(15), None, None)),
                offerType = Some(OfferType.SELL),
                categoryType = Some(CategoryType.COMMERCIAL),
                commercialTypes = Seq(CommercialType.AUTO_REPAIR, CommercialType.BUSINESS)
              )
          ),
        expectedResult = false
      ),
      TestCase(
        description = "return true when instance 'Multiple' value contains expected",
        service = Service.AUTO_REVIEWS,
        searchAttributes = Map("pro" -> "pro0"),
        instance =
          InstanceGen.next.copy(
            essentials =
              AutoReviewsEssentialsGen.next.copy(
                pro = Seq("pro0", "pro1")
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "return false when service mismatch",
        service = Service.AUTO_REVIEWS,
        searchAttributes = Map("pro" -> "pro0"),
        instance =
          InstanceGen.next.copy(
            essentials = RealtyEssentialsGen.next
          ),
        expectedResult = false
      ),
      TestCase(
        description = "return true when set of instance signals contains set of expected signals",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "signal_keys" -> "automatic_MODERATION_warn_REGION_MISMATCH,automatic_MODERATION_warn_BLOCKED_IP",
            "min_signals_time" -> "0",
            "max_signals_time" -> "2000"
          ),
        instance =
          InstanceGen.next
            .copy(essentials = RealtyEssentialsGen.next)
            .copy(signals =
              SignalSet(
                WarnSignal(
                  domain = Domain.Realty.default,
                  source = AutomaticSource(Application.MODERATION),
                  detailedReason = DetailedReason.RegionMismatch,
                  weight = 1.0,
                  timestamp = DateTimeUtil.fromMillis(1000),
                  info = None,
                  switchOff = None,
                  ttl = None,
                  outerComment = None,
                  auxInfo = SignalInfoSet.Empty
                ),
                WarnSignal(
                  domain = Domain.Realty.default,
                  source = AutomaticSource(Application.MODERATION_RULES),
                  detailedReason = DetailedReason.DoNotExist,
                  weight = 1.0,
                  timestamp = DateTimeUtil.fromMillis(5000),
                  info = None,
                  switchOff = None,
                  ttl = None,
                  outerComment = None,
                  auxInfo = SignalInfoSet.Empty
                ),
                WarnSignal(
                  domain = Domain.Realty.default,
                  source = AutomaticSource(Application.MODERATION),
                  detailedReason = DetailedReason.BlockedIp,
                  weight = 1.0,
                  timestamp = DateTimeUtil.fromMillis(1000),
                  info = None,
                  switchOff = None,
                  ttl = None,
                  outerComment = None,
                  auxInfo = SignalInfoSet.Empty
                )
              )
            ),
        expectedResult = true
      ),
      TestCase(
        description =
          "return false when set of instance signals contains set of expected signals " +
            "but there is time range mismatch",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "signal_keys" -> "automatic_MODERATION_warn_REGION_MISMATCH,automatic_MODERATION_warn_BLOCKED_IP",
            "min_signals_time" -> "0",
            "max_signals_time" -> "2000"
          ),
        instance =
          InstanceGen.next
            .copy(essentials = RealtyEssentialsGen.next)
            .copy(signals =
              SignalSet(
                WarnSignal(
                  domain = Domain.Realty.default,
                  source = AutomaticSource(Application.MODERATION),
                  detailedReason = DetailedReason.RegionMismatch,
                  weight = 1.0,
                  timestamp = DateTimeUtil.fromMillis(1000),
                  info = None,
                  switchOff = None,
                  ttl = None,
                  outerComment = None,
                  auxInfo = SignalInfoSet.Empty
                ),
                WarnSignal(
                  domain = Domain.Realty.default,
                  source = AutomaticSource(Application.MODERATION_RULES),
                  detailedReason = DetailedReason.DoNotExist,
                  weight = 1.0,
                  timestamp = DateTimeUtil.fromMillis(1000),
                  info = None,
                  switchOff = None,
                  ttl = None,
                  outerComment = None,
                  auxInfo = SignalInfoSet.Empty
                ),
                WarnSignal(
                  domain = Domain.Realty.default,
                  source = AutomaticSource(Application.MODERATION),
                  detailedReason = DetailedReason.BlockedIp,
                  weight = 1.0,
                  timestamp = DateTimeUtil.fromMillis(5000),
                  info = None,
                  switchOff = None,
                  ttl = None,
                  outerComment = None,
                  auxInfo = SignalInfoSet.Empty
                )
              )
            ),
        expectedResult = false
      ),
      TestCase(
        description = "use case-insensitive search",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "commercial_types" -> "auto_repair"
          ),
        instance =
          InstanceGen.next.copy(
            essentials =
              RealtyEssentialsGen.next.copy(
                commercialTypes = Seq(CommercialType.AUTO_REPAIR)
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "finds substring in the normalized actually zone fields",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "description" -> " продам квартиру "
          ),
        instance =
          InstanceGen.next.copy(
            essentials =
              RealtyEssentialsGen.next.copy(
                description = Some("Объявление: Продам\nквартиру! дешево")
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "finds substring in the actually zone fields (two spaces in a row)",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "description" -> " продам квартиру "
          ),
        instance =
          InstanceGen.next.copy(
            essentials =
              RealtyEssentialsGen.next.copy(
                description = Some("продам  квартиру")
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "finds substring in the actually zone fields (start of the string)",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "description" -> " продам квартиру "
          ),
        instance =
          InstanceGen.next.copy(
            essentials =
              RealtyEssentialsGen.next.copy(
                description = Some("Продам квартиру дешево")
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "finds substring in the actually zone fields (end of the string)",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "description" -> " продам квартиру "
          ),
        instance =
          InstanceGen.next.copy(
            essentials =
              RealtyEssentialsGen.next.copy(
                description = Some("Объявление: Продам квартиру дешево")
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "VSMODERATION-2213",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "description" -> "\"ru\""
          ),
        instance =
          InstanceGen.next.copy(
            essentials =
              RealtyEssentialsGen.next.copy(
                description = Some("rus")
              )
          ),
        expectedResult = false
      ),
      TestCase(
        description = "correctly search by signal_info",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "signal_info" -> " total region mismatch "
          ),
        instance =
          InstanceGen.next.copy(
            essentials = RealtyEssentialsGen.next,
            signals =
              SignalSet(
                WarnSignal(
                  domain = Domain.Realty.default,
                  source = AutomaticSource(Application.MODERATION),
                  detailedReason = DetailedReason.RegionMismatch,
                  weight = 1.0,
                  timestamp = DateTimeUtil.now(),
                  info = Some("total_region_mismatch;offer=11095;ip=11131;phones=10897"),
                  switchOff = None,
                  ttl = None,
                  outerComment = None,
                  auxInfo = SignalInfoSet.Empty
                )
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "correctly search by metadata for service USERS_AUTORU",
        service = Service.USERS_AUTORU,
        searchAttributes =
          Map(
            "meta_signals_maybe_inaccurate" -> "false",
            "min_meta_signals_DO_NOT_EXIST_count" -> "100"
          ),
        instance =
          InstanceGen.next.copy(
            essentials = UserAutoruEssentialsGen.next,
            metadata =
              MetadataSet(
                Metadata.SignalsStatistics(
                  timestamp = DateTimeGen.next,
                  statistics =
                    SignalsSummary(
                      maybeInaccurate = false,
                      signals =
                        Map(
                          SignalsSummary.AutoruQualifier(SignalType.BAN, Reason.DO_NOT_EXIST) -> TimedCounterGen.next
                            .copy(count = 100)
                        )
                    )
                )
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "correctly search by metadata for service USERS_REALTY",
        service = Service.USERS_REALTY,
        searchAttributes =
          Map(
            "meta_signals_maybe_inaccurate" -> "false",
            "min_meta_signals_DO_NOT_EXIST_SELL_VOS_count" -> "100"
          ),
        instance =
          InstanceGen.next.copy(
            essentials = UserRealtyEssentialsGen.next,
            metadata =
              MetadataSet(
                Metadata.SignalsStatistics(
                  timestamp = DateTimeGen.next,
                  statistics =
                    SignalsSummary(
                      maybeInaccurate = false,
                      signals =
                        Map(
                          SignalsSummary
                            .RealtyQualifier(SignalType.BAN, Reason.DO_NOT_EXIST, OfferType.SELL, isVos = true) ->
                            TimedCounterGen.next.copy(count = 100)
                        )
                    )
                )
              )
          ),
        expectedResult = true
      ),
      TestCase(
        description = "correctly search by fields prefix",
        service = Service.REALTY,
        searchAttributes =
          Map(
            "offer_type" -> "SELL",
            "category_type" -> "APART*",
            "phones" -> "777*"
          ),
        instance =
          InstanceGen.next
            .copy(essentials =
              RealtyEssentialsGen.next.copy(
                offerType = Some(OfferType.SELL),
                categoryType = Some(CategoryType.APARTMENT),
                phones = Seq("777888")
              )
            ),
        expectedResult = true
      )
    )

  "ModerationRule.isApplicable" should {

    testCases.foreach { case TestCase(description, service, searchAttributes, instance, expectedResult) =>
      description in {
        val rule =
          ModerationRuleGen.next.copy(
            service = service,
            searchAttributes = searchAttributes,
            matchingOptions = MatchingOptions(considerLatinAsCyrillic = false)
          )
        rule.isApplicable(documentBuilder(service).build(instance)) shouldBe expectedResult
      }
    }
  }
}
