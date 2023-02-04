package ru.yandex.vertis.general.wizard.meta.parser

import ru.yandex.vertis.general.wizard.meta.parser.TokensMatcher.UserRequestTokenDescriptor
import ru.yandex.vertis.general.wizard.meta.utils.Parser
import ru.yandex.vertis.general.wizard.model.BegemotResponse.Lemmas
import ru.yandex.vertis.general.wizard.model.{BegemotResponse, IntentionType, MetaWizardRequest, RequestMatch}
import zio.test.Assertion.equalTo
import zio.test.{assert, suite, test, DefaultRunnableSpec, ZSpec}

object UtilsSpec extends DefaultRunnableSpec {

  private val query = "The commander of the regiment was an elderly"
  private val metaWizardRequest = MetaWizardRequest.empty(query)

  private val metaWizardRequestSynonyms = MetaWizardRequest
    .empty(query)
    .copy(begemotResponse =
      Some(
        BegemotResponse(tokenLemmas =
          Seq(
            Lemmas(Seq("the", "a", "he")),
            Lemmas(Seq("commander", "general", "capo")),
            Lemmas(Seq("of", "outh")),
            Lemmas(Seq("the", "a", "he")),
            Lemmas(Seq("regiment", "regime")),
            Lemmas(Seq("was", "waths")),
            Lemmas(Seq("an", "a", "ann")),
            Lemmas(Seq("elder", "old", "granny"))
          )
        )
      )
    )

  private val matches: Set[RequestMatch] =
    Set(
      RequestMatch.Intention.userInputIndices(Set(0), IntentionType.Brand),
      RequestMatch.Intention.userInputIndices(Set(4, 5), IntentionType.Brand)
    )

  private val input = PartialParsedQuery(metaWizardRequest, matches)
  private val input2 = PartialParsedQuery(metaWizardRequest, Set.empty)
  private val output = Set("commander" -> 1, "of" -> 2, "the" -> 3, "an" -> 6, "elderly" -> 7)

  private val inputWithSynonyms = PartialParsedQuery(metaWizardRequestSynonyms, matches)

  private val output2 =
    Set("the" -> 0, "commander" -> 1, "of" -> 2, "the" -> 3, "regiment" -> 4, "was" -> 5, "an" -> 6, "elderly" -> 7)

  private val outputUserDictionary =
    Map(
      "commander" -> UserRequestTokenDescriptor(Set(1), isSynonym = false),
      "of" -> UserRequestTokenDescriptor(Set(2), isSynonym = false),
      "the" -> UserRequestTokenDescriptor(Set(3), isSynonym = false),
      "an" -> UserRequestTokenDescriptor(Set(6), isSynonym = false),
      "elderly" -> UserRequestTokenDescriptor(Set(7), isSynonym = false),
      "general" -> UserRequestTokenDescriptor(Set(1), isSynonym = true),
      "capo" -> UserRequestTokenDescriptor(Set(1), isSynonym = true),
      "outh" -> UserRequestTokenDescriptor(Set(2), isSynonym = true),
      "a" -> UserRequestTokenDescriptor(Set(3, 6), isSynonym = true),
      "he" -> UserRequestTokenDescriptor(Set(3), isSynonym = true),
      "ann" -> UserRequestTokenDescriptor(Set(6), isSynonym = true),
      "elder" -> UserRequestTokenDescriptor(Set(7), isSynonym = false),
      "old" -> UserRequestTokenDescriptor(Set(7), isSynonym = true),
      "granny" -> UserRequestTokenDescriptor(Set(7), isSynonym = true)
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Utils.Parser.split")(
      test("split query into correct parts") {
        assert(Parser.splitIntoIndexedTokens(input).toSet)(equalTo(output))
      },
      test("split query into correct parts") {
        assert(Parser.splitIntoIndexedTokens(input2).toSet)(equalTo(output2))
      },
      test("split query into correct parts with synonyms") {
        assert(Parser.userRequestDictionary(inputWithSynonyms))(equalTo(outputUserDictionary))
      }
    )

}
