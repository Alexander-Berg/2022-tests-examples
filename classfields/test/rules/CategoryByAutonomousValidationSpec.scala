package ru.yandex.vertis.general.wizard.meta.rules

import ru.yandex.vertis.general.wizard.meta.rules.impl.CategoryByAutonomousValidation
import ru.yandex.vertis.general.wizard.model.ParseStateInfo.{Discarded, Valid}
import ru.yandex.vertis.general.wizard.model.{
  IntentionType,
  MetaWizardRequest,
  ParseState,
  ParseStateInfo,
  RequestMatch
}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test.Assertion.equalTo
import zio.test._

object CategoryByAutonomousValidationSpec extends DefaultRunnableSpec with MockitoSupport {

  private val catsMatch = Option(RequestMatch.Category(RequestMatch.Source.AutonomousAttribute, "cats", 0L))

  private val dogsMatch = Option(
    RequestMatch.Category(RequestMatch.Source.UserRequestTokens(Set(0), isSynonymsParsed = false), "dogs", 0L)
  )

  private val commercialIntentionMatches = Set(
    RequestMatch.Intention.userInputIndices(Set(1), IntentionType.Common),
    RequestMatch.Intention.userInputIndices(Set(2), IntentionType.Commercial)
  )

  private val intentionMatches = Set(
    RequestMatch.Intention.userInputIndices(Set(1), IntentionType.Common),
    RequestMatch.Intention.userInputIndices(Set(2), IntentionType.Brand)
  )

  private val valid1 = ParseState
    .empty(MetaWizardRequest.empty("кошки мягкие сибирские теплые"))
    .copy(
      categoryMatch = catsMatch,
      intentionMatches = commercialIntentionMatches
    )

  private val valid2 = ParseState
    .empty(MetaWizardRequest.empty("кошки мягкие сибирские теплые"))
    .copy(
      categoryMatch = dogsMatch,
      intentionMatches = intentionMatches
    )

  private val discarded = ParseState
    .empty(MetaWizardRequest.empty("кошки мягкие сибирские теплые"))
    .copy(
      categoryMatch = catsMatch,
      intentionMatches = intentionMatches
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CategoryByAutonomousValidation RuleNode")(
      testM("valid with commercial intent") {
        for {
          parsedStates <- CategoryByAutonomousValidation.process(
            valid1
          )
        } yield assert(parsedStates.map(_.stateInfo).toSet)(equalTo(Set[ParseStateInfo](Valid)))
      },
      testM("valid non-autonomous attribute parse") {
        for {
          parsedStates <- CategoryByAutonomousValidation.process(
            valid2
          )
        } yield assert(parsedStates.map(_.stateInfo).toSet)(equalTo(Set[ParseStateInfo](Valid)))
      },
      testM("discard without commercial intention") {
        for {
          parsedStates <- CategoryByAutonomousValidation.process(
            discarded
          )
        } yield assert(parsedStates.map(_.stateInfo.isInstanceOf[Discarded]).toSet)(equalTo(Set(true)))
      }
    )
}
