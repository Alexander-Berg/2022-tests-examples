package ru.yandex.realty.managers.company

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.managers.company.CompanyConverters._
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class CompanyManagerSpec
  extends AsyncSpecBase
  with RequestAware
  with PropertyChecks
  with SearcherResponseModelGenerators {

  private val searcherClient: SearcherClient = mock[SearcherClient]

  private val mockGetCompanyData = toMockFunction2(searcherClient.getCompanyData(_: Long)(_: Traced))

  val manager: CompanyManager = new DefaultCompanyManager(searcherClient)

  private val notFound = new NoSuchElementException("")

  "companyManager" when {
    "getObjects" should inSequence {
      "propagate missing companyId as NOT_FOUND" in {
        inSequence {
          forAll(companyIdGen) { companyId =>
            mockGetCompanyData
              .expects(companyId, *)
              .throwingF(notFound)

            val result = manager.getObjects(companyId)

            val throwable = result.failed.futureValue
            throwable should be(notFound)
          }
        }
      }

      "conclude with some valid response for a valid company" in {
        inSequence {
          forAll(companyIdGen, companyObjectsGen) { (companyId, companyObjects) =>
            mockGetCompanyData
              .expects(companyId, *)
              .returningF(companyObjects)

            val result = manager.getObjects(companyId)

            val expected = toResponse(companyObjects)
            result.futureValue should be(expected)
          }
        }
      }
    }
  }
}
