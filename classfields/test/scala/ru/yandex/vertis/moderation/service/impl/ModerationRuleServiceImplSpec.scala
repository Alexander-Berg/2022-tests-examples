package ru.yandex.vertis.moderation.service.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.Globals
import ru.yandex.vertis.moderation.dao.ModerationRuleDao
import ru.yandex.vertis.moderation.feature.ModerationFeatureTypes
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.rule.service.DocumentClauseMatcher
import ru.yandex.vertis.moderation.searcher.core.saas.document.DocumentBuilder
import ru.yandex.vertis.moderation.searcher.core.saas.search.SearchClauseBuilder
import ru.yandex.vertis.moderation.service.{ModerationRuleService, ModerationRuleServiceSpecBase}

import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
class ModerationRuleServiceImplSpec extends ModerationRuleServiceSpecBase {
  override protected val dao: ModerationRuleDao = mock[ModerationRuleDao]

  override protected val featureRegistry: FeatureRegistry =
    new InMemoryFeatureRegistry(new CompositeFeatureTypes(Iterable(BasicFeatureTypes, ModerationFeatureTypes)))

  override protected val service: ModerationRuleService =
    new ModerationRuleServiceImpl(
      dao,
      new DocumentBuilder(Globals.opinionCalculator(Model.Service.AUTORU)),
      featureRegistry
    )(SearchClauseBuilder, DocumentClauseMatcher, ExecutionContext.global)
}
