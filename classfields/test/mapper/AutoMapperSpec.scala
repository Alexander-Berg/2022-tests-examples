package ru.vertistraf.cost_plus.builder.auto.mapper

import ru.vertistraf.cost_plus.builder.model.mapper.OfferWithSet
import ru.vertistraf.cost_plus.builder.model.mapper.OfferWithSet.Auto
import ru.vertistraf.cost_plus.model.ServiceSetInfo.AutoSetInfo
import ru.vertistraf.cost_plus.model.{
  AllServiceAdSets,
  AllServiceAdSetsRow,
  RawServiceSet,
  RawServiceSetInfo,
  ServiceOffer,
  ServiceSetInfo
}
import ru.vertistraf.cost_plus.model.auto.{AutoUrlParams, Filter, Section}
import ru.yandex.inside.yt.kosher.impl.operations.StatisticsImpl
import ru.yandex.inside.yt.kosher.operations.{OperationContext, Yield}
import zio.test._
import zio.test.environment.TestEnvironment

import scala.collection.mutable.ArrayBuffer

object AutoMapperSpec extends DefaultRunnableSpec {

  private def runMapAndGetSetInfo(urlPath: String, params: AutoUrlParams) = {
    val buff = ArrayBuffer.empty[Auto]
    val `yield` = new Yield[OfferWithSet.Auto] {
      override def `yield`(index: Int, value: Auto): Unit = {
        require(index == 0)
        buff += value
      }

      override def close(): Unit = ()
    }

    val offer = ServiceOffer.Auto.Moto(relevance = 1)

    new AutoMapper().map(
      AllServiceAdSetsRow(
        AllServiceAdSets(offer, Seq(RawServiceSet(urlPath, "title", RawServiceSetInfo.Auto(params))))
      ),
      `yield`,
      new StatisticsImpl,
      new OperationContext()
    )

    require(buff.size == 1)
    buff.head.info
  }

  private def mapperTest(name: String)(urlPath: String, params: AutoUrlParams, expected: ServiceSetInfo.AutoSetInfo) =
    test(name) {

      assertTrue(runMapAndGetSetInfo(urlPath, params) == expected)
    }

  private def params(
      geoId: Option[Long] = None,
      mark: Option[String] = None,
      model: Option[String] = None,
      section: Option[Section] = None,
      filters: Set[Filter] = Set.empty) = AutoUrlParams(
    geoId,
    mark,
    model,
    section,
    filters
  )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("AutoMapper")(
      mapperTest("should correctly parse tags wo geo")(
        "/cars/all/tag/liquid/",
        params = params(section = Some(Section.All), filters = Set(Filter.SearchTag)),
        expected = AutoSetInfo.Collapse.Tags(geoCode = None, state = Section.All)
      ),
      mapperTest("should correctly parse tags with geo")(
        "/sankt-peterburg/cars/all/tag/liquid/",
        params = params(geoId = Some(2L), section = Some(Section.Used), filters = Set(Filter.SearchTag)),
        expected = AutoSetInfo.Collapse.Tags(geoCode = Some("sankt-peterburg"), state = Section.Used)
      ),
      mapperTest("should correctly parse collapsing by dealer from dealer net")(
        "/dealer-net/rolf/",
        params = params(filters = Set(Filter.DealerNetSemanticUrl)),
        expected = AutoSetInfo.Collapse.ByDealer
      ),
      mapperTest("should correctly parse collapsing by dealer for dealer net")(
        "/dealer-net/rolf/",
        params = params(filters = Set(Filter.DealerNetSemanticUrl)),
        expected = AutoSetInfo.Collapse.ByDealer
      ),
      mapperTest("should correctly parse collapsing by dealer for dealers listing")(
        "/dilery/cars/all/",
        params = params(section = Some(Section.All)),
        expected = AutoSetInfo.Collapse.ByDealer
      ),
      mapperTest("should correctly parse carousel for direct dealer")(
        "/diler-oficialniy/cars/used/inchcape_certified_moskva/",
        params = params(filters = Set(Filter.DealerCode), section = Some(Section.Used)),
        expected = AutoSetInfo.Carousel
      ),
      mapperTest("should correctly parse vendors")(
        "/cars/vendor-european/used/drive-forward_wheel/",
        params = params(section = Some(Section.Used), filters = Set(Filter.GearType, Filter.CatalogFilter)),
        expected = AutoSetInfo.Collapse.Vendor(vendorCode = "european")
      )
    )
}
