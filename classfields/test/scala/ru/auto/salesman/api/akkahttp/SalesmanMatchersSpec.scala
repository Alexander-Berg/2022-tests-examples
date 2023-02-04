package ru.auto.salesman.api.akkahttp

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.Matched
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.test.BaseSpec

class SalesmanMatchersSpec extends BaseSpec with SalesmanMatchers {

  "User offer product matcher" should {

    "match placement" in {
      offerProductMatcher(AutoRu)(Path("placement")) should matchPattern {
        case Matched(_, _) =>
      }
    }
  }
}
