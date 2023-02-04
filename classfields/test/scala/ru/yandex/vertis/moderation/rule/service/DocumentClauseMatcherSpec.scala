package ru.yandex.vertis.moderation.rule.service

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.rule.MatchingOptions
import ru.yandex.vertis.moderation.rule.service.DocumentClauseMatcherSpec.{documentFromField, TestCase}
import ru.yandex.vertis.moderation.searcher.core.saas.clauses.{Clause, Operator, ValueClause}
import ru.yandex.vertis.moderation.searcher.core.saas.document.{Document, FieldValue, RealtyFields}

class DocumentClauseMatcherSpec extends SpecBase {

  private val matcher: Matcher[Document, Clause, MatchingOptions] = DocumentClauseMatcher

  implicit val service: Service = Service.REALTY

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "match document with latin symbols when clause contains only cyrillic and option is turned on",
        document = FieldValue(RealtyFields.Description, "\u0061б\u0063"),
        clause = ValueClause(RealtyFields.Description, "\"абс\"", Operator.Eq),
        matchingOptions =
          MatchingOptions(
            considerLatinAsCyrillic = true
          ),
        expectedResult = true
      ),
      TestCase(
        description =
          "not match document with latin symbols when clause contains only cyrillic and option is turned off",
        document = FieldValue(RealtyFields.Description, "\u0061б\u0063"),
        clause = ValueClause(RealtyFields.Description, "\"абс\"", Operator.Eq),
        matchingOptions =
          MatchingOptions(
            considerLatinAsCyrillic = false
          ),
        expectedResult = false
      ),
      TestCase(
        description = "not match document with latin symbols when matched field is not zoned",
        document = FieldValue(RealtyFields.GarageName, "\u0061б\u0063"),
        clause = ValueClause(RealtyFields.GarageName, "\"абс\"", Operator.Eq),
        matchingOptions =
          MatchingOptions(
            considerLatinAsCyrillic = true
          ),
        expectedResult = false
      )
    )

  "DocumentClauseMatcher" should {
    testCases.foreach { case TestCase(description, document, clause, matchingOptions, expectedResult) =>
      description in {
        matches(document, clause)(matchingOptions) shouldBe expectedResult
      }
    }
  }

  private def matches(document: Document, clause: Clause)(implicit matchingOptions: MatchingOptions): Boolean =
    matcher.matches(document, clause)

}

object DocumentClauseMatcherSpec {

  implicit def documentFromField(field: FieldValue)(implicit service: Service): Document =
    Document(service, Seq(field), List.empty, CoreGenerators.ExternalIdGen.next)

  case class TestCase(description: String,
                      document: Document,
                      clause: Clause,
                      matchingOptions: MatchingOptions,
                      expectedResult: Boolean
                     )

}
