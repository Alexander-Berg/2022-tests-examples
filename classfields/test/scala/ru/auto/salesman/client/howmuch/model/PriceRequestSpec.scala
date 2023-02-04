package ru.auto.salesman.client.howmuch.model

import billing.common_model.Project
import billing.howmuch.model.{RequestContext, RequestCriteria}
import billing.howmuch.price_service.{GetPricesRequest, GetPricesRequestEntry}
import com.google.protobuf.timestamp.Timestamp
import org.joda.time.DateTime
import ru.auto.salesman.client.howmuch.model.MatrixId.ProductMatrixId
import ru.auto.salesman.client.howmuch.model.PriceRequest.PriceRequestEntry
import ru.auto.salesman.model.criteria.Criterion
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.model.criteria.CriteriaContext.CallCarsNewCriteriaContext

class PriceRequestSpec extends BaseSpec {
  "convert toProto with right context ordering" in {
    val ctx = CallCarsNewCriteriaContext(
      List(
        Criterion(Criterion.modelKey, "X5"),
        Criterion(Criterion.regionKey, "1"),
        Criterion(Criterion.markKey, "BMW")
      )
    )
    val product = ProductId.Call
    val date = DateTime.now()

    val request = PriceRequest(
      List(
        PriceRequestEntry(ProductMatrixId(ProductId.Call), ctx)
      ),
      date
    )

    val expectedContext = RequestContext(
      List(
        RequestCriteria("region_id", "1"),
        RequestCriteria("mark", "BMW"),
        RequestCriteria("model", "X5")
      )
    )

    val expected = GetPricesRequest(
      Project.AUTORU,
      List(
        GetPricesRequestEntry(
          request.entries.head.entryId.value,
          product.toString,
          Some(expectedContext)
        )
      ),
      Some(Timestamp.fromJavaProto(request.datetime.asTimestamp))
    )

    PriceRequest.toProto(request) shouldBe expected
  }
}
