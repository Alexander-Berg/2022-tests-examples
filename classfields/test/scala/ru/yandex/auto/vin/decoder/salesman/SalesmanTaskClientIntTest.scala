package ru.yandex.auto.vin.decoder.salesman

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.salesman.products.ProductsOuterClass.Product.ProductStatus
import ru.yandex.auto.vin.decoder.model.UserRef
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

@Ignore
class SalesmanTaskClientIntTest extends AnyFunSuite {

  implicit val t: Traced = Traced.empty

  val endpoint: HttpEndpoint = HttpEndpoint(
    "salesman-tasks-01-sas.test.vertis.yandex.net",
    1030,
    "http"
  )

  val remoteService = new RemoteHttpService("salesman-task", endpoint)
  val client = new SalesmanTasksClient(remoteService)

  test("create product") {

    val result = client
      .createProduct(
        "gibdd-history",
        UserRef.parseOrThrow("dealer:123"),
        s"test-${System.currentTimeMillis()}",
        "report"
      )
      .await

    assert(result == ProductCreated)
  }

  test("get product") {

    val id = s"test-${System.currentTimeMillis()}"

    val created = client
      .createProduct(
        "gibdd-history",
        UserRef.parseOrThrow("dealer:20101"),
        id,
        "report"
      )
      .await

    val result = client
      .getProduct(
        "gibdd-history",
        UserRef.parseOrThrow("dealer:20101"),
        id, // "test-1602600561853",
        "report"
      )
      .await

    assert(created == ProductCreated)
    assert(result.get.getStatus == ProductStatus.NEED_PAYMENT)
  }

}
