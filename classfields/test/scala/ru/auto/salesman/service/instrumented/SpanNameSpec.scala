package ru.auto.salesman.service.instrumented

import ru.auto.salesman.test.BaseSpec
import sourcecode.Name

class SpanNameSpec extends BaseSpec {

  "TraceName.apply" should {
    "include class name & method name in snake_case; not include instrumented prefix" in {
      InstrumentedTestService.testMethod shouldBe "test_service.test_method"
    }

    "convert method name to snake_case" in {
      object TestRoute {
        def upsertCampaignRoute: String =
          SpanName(implicitly[Name])
      }
      TestRoute.upsertCampaignRoute shouldBe "upsert_campaign_route"
    }
  }
}
