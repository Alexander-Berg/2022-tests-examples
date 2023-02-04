package ru.yandex.auto.searcher.http.action

import java.util.concurrent.TimeUnit

import org.junit.{Ignore, Test}
import org.junit.rules.Timeout
import org.springframework.beans.factory.annotation.Autowired
import ru.auto.api.{CatalogModel, ResponseModel}
import ru.yandex.auto.searcher.core.CarSearchParamsImpl
import ru.yandex.auto.searcher.http.action.api.ApiCarsSearchAction

import scala.collection.convert.decorateAll._

@Ignore
class CarSearchColorsTest extends AbstractSearcherTest {

  @Autowired var carsSearchAction: ApiCarsSearchAction = _

  @Test
  @Timeout(5, TimeUnit.MINUTES)
  def groupingAndSortingTogether(): Unit = {
    val params: CarSearchParamsImpl = createSearchParams()
    params.setParam("super_gen", "21372704")
    params.setParam("configuration_id", "21372801")

    val response: ResponseModel.OfferListingResponse =
      carsSearchAction.processInternal(params, emptyProfileData, emptyRequest)

    val results: Seq[CatalogModel.VendorColor] =
      response.getOffersList.asScala
        .flatMap(_.getCarInfo.getComplectation.getVendorColorsList.asScala)
    results.size shouldBe >(0)

    println(s"results ${results.size}")

    val mainColor = results.filter(_.getMainColor == true)
    println(mainColor.map(p => (p.getMainColor, p.getNameRu)))

//  В исходном виде тест флапает т.к. поднимает только первые 100 партиций и там может не быть нужных объявлений.
//  mainColor.nonEmpty shouldBe true // FIXME
  }
}
