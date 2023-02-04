package ru.yandex.vertis.moderation

import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient.DefaultAsyncHttpClient
import org.junit.Ignore
import ru.auto.catalog.model.api.ApiModel.{RawCatalogFilter, RawFilterRequest}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Category
import ru.yandex.vertis.moderation.service.catalog.CatalogHttpClient

import scala.concurrent.ExecutionContext

@Ignore("For manual running")
class CatalogClientSpec extends SpecBase with StrictLogging {
  val asyncHttpClient = new DefaultAsyncHttpClient()

  "Catalog client" should {
    val testingUrl = "http://autoru-catalog-api-int.vrts-slb.test.vertis.yandex.net/api"
    val client: CatalogHttpClient =
      new CatalogHttpClient(new DefaultAsyncHttpClient(), testingUrl)(ExecutionContext.global)

    "do request and deserialize" in {
      val filter =
        RawCatalogFilter
          .newBuilder()
          .setMark("KIA")
          .setModel("CEED")
          .setComplectation("21453272")
          .setSuperGen("21212472")
          .build()
      val request = RawFilterRequest.newBuilder().addFilters(filter).build()
      client.filter(Category.CARS, request).futureValue
    }
  }

}
