package ru.yandex.vertis.moderation.service

import org.joda.time.DateTime
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.ModerationRuleDao
import ru.yandex.vertis.moderation.service.impl.ModerationRuleServiceImpl
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.moderation.feature.ModerationFeatureTypes.SetRuleIdFeatureType
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{AutoruEssentialsGen, InstanceIdGen}
import ru.yandex.vertis.moderation.model.rule.RuleId
import ru.yandex.vertis.moderation.model.generators.Producer._
import org.mockito.Mockito._
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, Diff, Essentials, Instance, UpdateJournalRecord}
import ru.yandex.vertis.moderation.model.meta.MetadataSet
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.rule.{MatchingOptions, ModerationAction, ModerationRule, RuleApplyingPolicy, State}
import ru.yandex.vertis.moderation.rule.ModerationRule.SearchAttributes
import ru.yandex.vertis.moderation.util.SlicedResult
import ru.yandex.vertis.moderation.util.Page

import scala.concurrent.Future

trait ModerationRuleServiceSpecBase extends SpecBase {

  protected def service: ModerationRuleService

  protected def dao: ModerationRuleDao

  protected def featureRegistry: FeatureRegistry

  private val ruleId = 42
  private val description = "funny"
  private val beforeRuleUpdateTime = DateTime.parse("2020-05-30T01:20")
  private val ruleUpdateTime = DateTime.parse("2020-06-30T01:20")
  private val afterRuleUpdateTime = DateTime.parse("2020-07-30T01:20")
  private val defaultSearchAttributes =
    Map(
      "description" -> " funny "
    )

  private def setupDao(searchAttributes: SearchAttributes = defaultSearchAttributes): Unit = {
    val rule =
      ModerationRule(
        id = ruleId,
        searchAttributes = searchAttributes,
        matchingOptions = MatchingOptions(considerLatinAsCyrillic = false),
        action =
          ModerationAction.Ban(
            domains = Set(Domain.Autoru(Model.Domain.Autoru.DEFAULT_AUTORU)),
            detailedReason = DetailedReason.WrongName,
            comment = None
          ),
        state =
          State(
            onMatch = RuleApplyingPolicy.Do,
            onMismatch = RuleApplyingPolicy.Undo
          ),
        isDeleted = false,
        createTime = ruleUpdateTime,
        updateTime = ruleUpdateTime,
        userId = "234234234",
        service = Service.AUTORU,
        frontendData = None,
        comment = None,
        lastEditor = None,
        tags = Map.empty
      )
    when(dao.getByFilter(any(), any(), any())).thenReturn(
      Future.successful(
        SlicedResult(
          values = Seq(rule),
          total = 1,
          slice = Page(0, 10)
        )
      )
    )
  }

  private def generateRecord(timestampCreate: DateTime,
                             needPrevious: Boolean = true,
                             diff: Model.Diff.Autoru.Value = Model.Diff.Autoru.Value.DESCRIPTION
                            ): UpdateJournalRecord = {
    def setupEssentials(essentials: AutoruEssentials): AutoruEssentials =
      essentials.copy(
        description = Some(description),
        timestampCreate = Some(timestampCreate)
      )
    def createInstance(essentials: Essentials): Instance =
      Instance(
        id = InstanceIdGen.next,
        essentials = essentials,
        signals = SignalSet.Empty,
        createTime = afterRuleUpdateTime,
        essentialsUpdateTime = afterRuleUpdateTime,
        context = Context.Default,
        metadata = MetadataSet.Empty
      )

    val essentials = setupEssentials(AutoruEssentialsGen.next)
    val prev = if (needPrevious) Some(AutoruEssentialsGen.next).map(setupEssentials) else None

    UpdateJournalRecord(
      timestamp = afterRuleUpdateTime,
      depth = 1,
      instance = createInstance(essentials),
      prev = prev.map(createInstance),
      diff = Diff.Autoru(Set(diff))
    )
  }

  "getRules" should {
    "try to optimize and fail to optimize if instance was created before rule update and diff optimization is enabled" in {
      featureRegistry.updateFeature(ModerationRuleServiceImpl.UseOptimizationForAllFeatureName, true).futureValue
      featureRegistry
        .updateFeature(ModerationRuleServiceImpl.DoNotUseOptimizationForSelectedFeatureName, Set.empty[RuleId])
        .futureValue
      setupDao()

      val record = generateRecord(timestampCreate = beforeRuleUpdateTime)
      val result = service.getRules(record).futureValue
      result.stat.failedToOptimizeCount shouldBe 1
      result.rules.applicable.size shouldBe 1
    }

    "try to optimize and fail to optimize if instance was created after rule update and instance is first time in moderation and diff optimization is enabled" in {
      featureRegistry.updateFeature(ModerationRuleServiceImpl.UseOptimizationForAllFeatureName, true).futureValue
      featureRegistry
        .updateFeature(ModerationRuleServiceImpl.DoNotUseOptimizationForSelectedFeatureName, Set.empty[RuleId])
        .futureValue
      setupDao()

      val record = generateRecord(timestampCreate = afterRuleUpdateTime, needPrevious = false)
      val result = service.getRules(record).futureValue
      result.stat.failedToOptimizeCount shouldBe 1
      result.rules.applicable.size shouldBe 1
    }

    "get empty result when trying to optimize if instance was created after rule update and diff not intersect and diff optimization is enabled" in {
      featureRegistry.updateFeature(ModerationRuleServiceImpl.UseOptimizationForAllFeatureName, true).futureValue
      featureRegistry
        .updateFeature(ModerationRuleServiceImpl.DoNotUseOptimizationForSelectedFeatureName, Set.empty[RuleId])
        .futureValue
      setupDao()

      val record = generateRecord(timestampCreate = afterRuleUpdateTime, diff = Model.Diff.Autoru.Value.MODEL)
      val result = service.getRules(record).futureValue
      result.stat.failedToOptimizeCount shouldBe 0
      result.rules.applicable.size shouldBe 0
    }

    "not try to optimize if instance was created after rule update and optimization is disabled" in {
      featureRegistry.updateFeature(ModerationRuleServiceImpl.UseOptimizationForAllFeatureName, false).futureValue
      featureRegistry
        .updateFeature(ModerationRuleServiceImpl.UseOptimizationForSelectedFeatureName, Set.empty[RuleId])
        .futureValue
      setupDao()

      val record = generateRecord(timestampCreate = afterRuleUpdateTime)
      val result = service.getRules(record).futureValue
      result.stat.notTriedToOptimizeCount shouldBe 1
      result.rules.applicable.size shouldBe 1
    }

    "not try to optimize if instance was created after rule update and optimization is disabled for this rule" in {
      featureRegistry.updateFeature(ModerationRuleServiceImpl.UseOptimizationForAllFeatureName, true).futureValue
      featureRegistry
        .updateFeature(ModerationRuleServiceImpl.DoNotUseOptimizationForSelectedFeatureName, Set(ruleId))
        .futureValue
      setupDao()

      val record = generateRecord(timestampCreate = afterRuleUpdateTime)
      val result = service.getRules(record).futureValue
      result.stat.notTriedToOptimizeCount shouldBe 1
      result.rules.applicable.size shouldBe 1
    }

    "optimize if instance was created after rule update, optimization is enabled and record's diff doesn't intersect with rule's diff" in {
      featureRegistry.updateFeature(ModerationRuleServiceImpl.UseOptimizationForAllFeatureName, true).futureValue
      featureRegistry
        .updateFeature(ModerationRuleServiceImpl.DoNotUseOptimizationForSelectedFeatureName, Set.empty[RuleId])
        .futureValue
      setupDao()

      val record = generateRecord(timestampCreate = afterRuleUpdateTime, diff = Model.Diff.Autoru.Value.MODEL)
      val result = service.getRules(record).futureValue
      result.stat.optimizedCount shouldBe 1
      result.rules.isEmpty shouldBe true
    }

    "optimize if instance was created after rule update, optimization is enabled for this rule and record's diff doesn't intersect with rule's diff" in {
      featureRegistry.updateFeature(ModerationRuleServiceImpl.UseOptimizationForAllFeatureName, false).futureValue
      featureRegistry
        .updateFeature(ModerationRuleServiceImpl.UseOptimizationForSelectedFeatureName, Set(ruleId))
        .futureValue
      setupDao()

      val record = generateRecord(timestampCreate = afterRuleUpdateTime, diff = Model.Diff.Autoru.Value.MODEL)
      val result = service.getRules(record).futureValue
      result.stat.optimizedCount shouldBe 1
      result.rules.isEmpty shouldBe true
    }
  }
}
