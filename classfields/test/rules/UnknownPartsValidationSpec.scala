package ru.yandex.vertis.general.wizard.meta.rules

import ru.yandex.vertis.general.wizard.meta.rules.impl.UnknownPartsValidation
import ru.yandex.vertis.general.wizard.model.Experiment.{UnknownWords, WithoutClassification}
import ru.yandex.vertis.general.wizard.model.{
  ClassifierResult,
  Experiment,
  IntentionType,
  MetaWizardRequest,
  ParseState,
  ParseStateInfo,
  RequestMatch
}
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, testM, DefaultRunnableSpec, ZSpec}

object UnknownPartsValidationSpec extends DefaultRunnableSpec {

  private val categoryMatch = RequestMatch.Category.userInputIndices(Set(0), "notebooks", 1)
  private val commercialMatch = RequestMatch.Intention.userInputIndices(Set(1), IntentionType.Commercial)

  private val emptyRequest = MetaWizardRequest.empty("купить ноутбук")

  private val categoryMatchedState = ParseState.empty(emptyRequest).copy(categoryMatch = Some(categoryMatch))

  private val fullyMatchedState =
    ParseState.empty(emptyRequest).copy(categoryMatch = Some(categoryMatch), intentionMatches = Set(commercialMatch))

  private val inapplicableClassifier =
    ClassifierResult("notebooks", "notebooks", 0.1f, 0.4f, 0.0f, rearr = None, isProductionReady = true)

  private val applicableProdClassifier =
    ClassifierResult("notebooks", "notebooks", 0.6f, 0.4f, 0.0f, rearr = None, isProductionReady = true)

  private val applicableTestClassifier =
    ClassifierResult("notebooks", "notebooks", 0.6f, 0.4f, 0.0f, rearr = None, isProductionReady = false)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("UnknownPartsValidation RuleNode")(
      testM("accept all parsed requests") {
        for {
          v <- UnknownPartsValidation.validate(fullyMatchedState)
        } yield assert(v)(
          equalTo(
            ParseStateInfo.Valid
          )
        )
      },
      testM("fail partial parsed requests") {
        for {
          v <- UnknownPartsValidation.validate(categoryMatchedState)
        } yield assert(v.isInstanceOf[ParseStateInfo.Discarded])(
          equalTo(
            true
          )
        )
      },
      testM("accept full parsed request with triggered classifier") {
        for {
          v <- UnknownPartsValidation.validate(fullyMatchedState.withClassifier(applicableProdClassifier))
        } yield assert(v)(
          equalTo(
            ParseStateInfo.Valid
          )
        )
      },
      testM("accept full parsed request with underweight triggered classifier") {
        for {
          v <- UnknownPartsValidation.validate(fullyMatchedState.withClassifier(inapplicableClassifier))
        } yield assert(v)(
          equalTo(
            ParseStateInfo.Valid
          )
        )
      },
      testM("accept partial parsed request with prod classifier") {
        for {
          v <- UnknownPartsValidation.validate(categoryMatchedState.withClassifier(applicableProdClassifier))
        } yield assert(v)(
          equalTo(
            ParseStateInfo.Valid
          )
        )
      },
      testM("fail partial parsed requests with test classifier") {
        for {
          v <- UnknownPartsValidation.validate(categoryMatchedState.withClassifier(applicableTestClassifier))
        } yield assert(v.isInstanceOf[ParseStateInfo.Discarded])(
          equalTo(
            true
          )
        )
      },
      testM("accept partial parsed requests with test classifier UnknownWords(1) experiment") {
        for {
          v <- UnknownPartsValidation.validate(
            categoryMatchedState.withClassifier(applicableTestClassifier).withExp(UnknownWords(1))
          )
        } yield assert(v)(
          equalTo(
            ParseStateInfo.Valid
          )
        )
      },
      testM("fail partial parsed requests with prod classifier and WithoutClassification experiment") {
        for {
          v <- UnknownPartsValidation.validate(
            categoryMatchedState.withClassifier(applicableProdClassifier).withExp(WithoutClassification)
          )
        } yield assert(v.isInstanceOf[ParseStateInfo.Discarded])(
          equalTo(
            true
          )
        )
      },
      testM("fail partial parsed requests with prod classifier and UnknownWords(0) experiment") {
        for {
          v <- UnknownPartsValidation.validate(
            categoryMatchedState.withClassifier(applicableProdClassifier).withExp(UnknownWords(0))
          )
        } yield assert(v.isInstanceOf[ParseStateInfo.Discarded])(
          equalTo(
            true
          )
        )
      }
    )

  implicit class TestParseState(val state: ParseState) extends AnyVal {

    def withClassifier(cr: ClassifierResult): ParseState =
      state.copy(metaWizardRequest = state.metaWizardRequest.copy(classifiersResults = Set(cr)))

    def withExp(exp: Experiment): ParseState =
      state.copy(metaWizardRequest = state.metaWizardRequest.copy(experiments = Set(exp)))
  }

}
