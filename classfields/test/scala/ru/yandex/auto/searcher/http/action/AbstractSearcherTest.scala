package ru.yandex.auto.searcher.http.action

import org.scalatest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
import ru.yandex.auto.Fixtures
import ru.yandex.auto.core.dictionary.LangsProvider
import ru.yandex.auto.core.vendor.VendorManager
import ru.yandex.auto.searcher.core.CarSearchParams._
import ru.yandex.auto.searcher.core.CarSearchParamsImpl
import ru.yandex.auto.searcher.main.region.RegionServiceImpl

@ContextConfiguration(locations = Array("/auto-searcher-test.xml"))
abstract class AbstractSearcherTest extends AbstractJUnit4SpringContextTests with Matchers with Fixtures {

  @Autowired private[action] var vendorManager: VendorManager = _
  @Autowired private[action] var langsProvider: LangsProvider = _
  @Autowired private[action] var regionService: RegionServiceImpl = _

  private[action] def createSearchParams(): CarSearchParamsImpl = {
    val params = new CarSearchParamsImpl(regionService.getRegionTree, vendorManager, langsProvider)
    params.setParam(PAGE_SIZE_OFFERS, "10")
    params.setParam(PAGE_NUM_OFFERS, "1")
    params
  }
}
