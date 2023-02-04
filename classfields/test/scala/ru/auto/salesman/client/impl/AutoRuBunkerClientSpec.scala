package ru.auto.salesman.client.impl

import akka.http.scaladsl.server.Directives._
import ru.auto.api.ApiOfferModel.Category
import ru.auto.salesman.client.json.JsonExecutorImpl
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

import scala.io.Codec

class AutoRuBunkerClientSpec extends BaseSpec {

  private val serverAddress = runServer {
    (path("cat") & get & parameter("node") & parameter("version")) {
      case ("auto_ru/common/vas_salesman", "latest") =>
        complete(readResource("/validDescriptions.json"))
      case (node, version) =>
        fail(s"Unexpected node = $node, version = $version")
    }
  }

  private val executor = new JsonExecutorImpl(serverAddress.toString)
  private val client = new AutoRuBunkerClient(executor, "latest")

  implicit val rc: RequestContext = AutomatedContext("test")
  implicit val codec = Codec("UTF-8")

  "AutoRuBunkerClient" should {
    "success request with all known fields" in {
      val descriptions = client.getDescriptions().success.value
      val name =
        for {
          byCategoryMap <-
            descriptions.autoruProductsDescriptions.autoruOfferTypeProductDescriptions
              .get(Placement)
          byDescriptionTypeMap <- byCategoryMap.get(Category.CARS)
          productDescription <- byDescriptionTypeMap.get(EndUserType.Default)
        } yield productDescription.name.get

      name.get shouldBe "Активация объявления"
    }
  }
}
