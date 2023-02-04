package ru.yandex.vertis.general.wizard.api.services

import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.model.RequestMatch.{Category, Intention}
import ru.yandex.vertis.general.wizard.model.{
  CategoryTag,
  IntentionType,
  ParseStateInfo,
  RequestMatch,
  WizardMatch,
  WizardRequest
}
import zio.ZLayer
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion.equalTo

object WizardMatchSelectorSpec extends DefaultRunnableSpec {

  private val musicInstrumentsMatch =
    WizardMatch.Category(
      Category.userInputIndices(Set(0), "music", 1),
      None,
      Set.empty,
      Set.empty,
      ParseStateInfo.Valid
    )

  private val birdsInstrumentsMatch =
    WizardMatch.Category(
      Category(RequestMatch.Source.UserRequestTokens(Set(0), isSynonymsParsed = true), "birds", 1),
      None,
      Set.empty,
      Set.empty,
      ParseStateInfo.Valid
    )

  private val guitarMatch =
    WizardMatch.Category(
      Category.userInputIndices(Set(0), "guitar", 1),
      None,
      Set.empty,
      Set.empty,
      ParseStateInfo.Valid
    )

  private val balalaikaMatch =
    WizardMatch.Category(
      Category.userInputIndices(Set(0), "balalaika", 1),
      None,
      Set.empty,
      Set.empty,
      ParseStateInfo.Valid
    )

  private val categoryTagsService = TestCategoryTagsResource.simple(CategoryTag.PreferableInParsing, Set("balalaika"))

  private val brandIntentionsMatch =
    WizardMatch.Intentions(None, Set(Intention.userInputIndices(Set(0), IntentionType.Brand)), ParseStateInfo.Valid)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("WizardMatchSelector")(
      testM("select category") {
        for {
          selectedMatch <-
            WizardMatchSelector.select(WizardRequest.empty("music"), Seq(musicInstrumentsMatch, brandIntentionsMatch))
        } yield assert(selectedMatch)(equalTo(musicInstrumentsMatch))
      },
      testM("select more concrete category") {
        for {
          selected <- WizardMatchSelector.select(WizardRequest.empty("guitar"), Seq(musicInstrumentsMatch, guitarMatch))
        } yield assert(selected)(equalTo(guitarMatch))
      },
      testM("select category with PreferableInParsing tag") {
        for {
          selected <- WizardMatchSelector.select(WizardRequest.empty(""), Seq(guitarMatch, balalaikaMatch))
        } yield assert(selected)(equalTo(balalaikaMatch))
      },
      testM("select pared by clean user input instead synonyms") {
        for {
          selected <- WizardMatchSelector.select(
            WizardRequest.empty(""),
            Seq(birdsInstrumentsMatch, musicInstrumentsMatch)
          )
        } yield assert(selected)(equalTo(musicInstrumentsMatch))
      }
    ).provideCustomLayer {
      val bonsai = ZLayer.succeed(LiveBonsaiService.create(TestCatalog.bonsaiSnapshot))
      val categoryTagsLayer = ZLayer.succeed(categoryTagsService)
      bonsai ++ categoryTagsLayer >>> WizardMatchSelector.live
    }
}
