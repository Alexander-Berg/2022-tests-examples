package ru.yandex.realty.dochub

import org.junit.runner.RunWith
import org.scalatest.AsyncFunSuite
import org.scalatest.junit.JUnitRunner
import realty.pdfprinter.templates.Templates.DocumentTemplate
import ru.yandex.realty.serialization.json.ProtoJsonFormats
import ru.yandex.vertis.mockito.MockitoSupport

@RunWith(classOf[JUnitRunner])
class DocHubUtilsSpec extends AsyncFunSuite with MockitoSupport with ProtoJsonFormats {

  test("getDocumentId") {
    assert(
      DocHubUtils
        .generateDocumentId("rent.contract", "12435", DocumentTemplate.getDefaultInstance) == "rent.contract.12435.-1579025880"
    )
    assert(
      DocHubUtils
        .generateDocumentId("rent.contract.termination", "12435", DocumentTemplate.getDefaultInstance) == "rent.contract.termination.12435.-1579025880"
    )
    assert(
      DocHubUtils
        .generateDocumentId("rent.inventory", "orid-1", DocumentTemplate.getDefaultInstance) == "rent.inventory.orid-1.-1579025880"
    )
  }
}
