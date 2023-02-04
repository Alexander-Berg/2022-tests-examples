package ru.yandex.vertis.general.wizard.meta.rules

import general.bonsai.category_model.{Category, CategoryState}
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.meta.rules.impl.RabotaCategoriesByIntentions
import ru.yandex.vertis.general.wizard.model.{IntentionType, MetaWizardRequest, ParseState, RequestMatch}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object RabotaCategoriesByIntentionsSpec extends DefaultRunnableSpec with MockitoSupport {

  private val rezume =
    Category(
      id = RabotaCategoriesByIntentions.Rezume,
      state = CategoryState.DEFAULT,
      symlinkToCategoryId = "",
      synonyms = Seq.empty
    )

  private val vacansii =
    Category(
      id = RabotaCategoriesByIntentions.Vacansii,
      state = CategoryState.DEFAULT,
      symlinkToCategoryId = "",
      synonyms = Seq.empty
    )

  private val snapshotSource: Seq[ExportedEntity] =
    Seq(
      ExportedEntity(ExportedEntity.CatalogEntity.Category(rezume)),
      ExportedEntity(ExportedEntity.CatalogEntity.Category(vacansii))
    )
  private val bonsaiSnapshot = BonsaiSnapshot(snapshotSource)

  private val bonsaiService = LiveBonsaiService.create(bonsaiSnapshot)

  private val ruleNode = RabotaCategoriesByIntentions(bonsaiService)

  private val findRabotaState =
    ParseState
      .empty(
        MetaWizardRequest.empty("")
      )
      .copy(intentionMatches =
        Set(
          RequestMatch
            .Intention(
              RequestMatch.Source.UserRequestTokens(Set(0), isSynonymsParsed = false),
              IntentionType.RabotaFind
            )
        )
      )

  private val postRabotaState =
    ParseState
      .empty(
        MetaWizardRequest.empty("")
      )
      .copy(intentionMatches =
        Set(
          RequestMatch
            .Intention(
              RequestMatch.Source.UserRequestTokens(Set(0), isSynonymsParsed = false),
              IntentionType.RabotaPost
            )
        )
      )

  private val neCategoryState =
    ParseState
      .empty(
        MetaWizardRequest.empty("")
      )
      .copy(
        categoryMatch = Some(
          RequestMatch.Category(RequestMatch.Source.UserRequestTokens(Set(0), isSynonymsParsed = false), "cats", 1L)
        ),
        intentionMatches = Set(
          RequestMatch
            .Intention(
              RequestMatch.Source.UserRequestTokens(Set(0), isSynonymsParsed = false),
              IntentionType.RabotaPost
            )
        )
      )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("RabotaCategoriesByIntentions RuleNode")(
      testM("detect vacansii") {
        for {
          state <- ruleNode.process(
            findRabotaState
          )
        } yield assert(state.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(
          equalTo(Set(RabotaCategoriesByIntentions.Vacansii))
        )
      },
      testM("detect rezume") {
        for {
          state <- ruleNode.process(
            postRabotaState
          )
        } yield assert(state.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(
          equalTo(Set(RabotaCategoriesByIntentions.Rezume))
        )
      },
      testM("not detect with non-empty category") {
        for {
          state <- ruleNode.process(
            neCategoryState
          )
        } yield assert(state.flatMap(_.categoryMatch.map(_.categoryId)).toSet)(equalTo(Set("cats")))
      }
    )
}
