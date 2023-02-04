package ru.yandex.realty.feeds

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.{PropertyChecks, TableDrivenPropertyChecks}

@RunWith(classOf[JUnitRunner])
class ExportUtilsSpec extends WordSpec with Matchers with PropertyChecks with TableDrivenPropertyChecks {
  "ExportUtils.formatQuotes" should {
    "remove known quotes from a string" in {
      val previouslyFailedCases = Table(
        heading = "string",
        rows = "ЖК «Светлый мир «Станция «Л»"
      )
      val knownQuotesPattern = "[\"'«»‘’‚“”„‹›〝〞]"

      def propRemovesAllKnownQuotes(string: String) = {
        ExportUtils.formatQuotes(string) shouldNot include regex knownQuotesPattern
      }

      forAll(previouslyFailedCases)(propRemovesAllKnownQuotes)
      forAll("string")(propRemovesAllKnownQuotes)
    }
  }
}
