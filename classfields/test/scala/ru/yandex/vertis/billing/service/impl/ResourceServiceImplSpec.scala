package ru.yandex.vertis.billing.service.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Spec on [[ResourceServiceImpl]]
  *
  * @author ruslansd
  */
class ResourceServiceImplSpec extends AnyWordSpec with Matchers {

  "ResourceServiceImpl" should {
    "extracts hosts from valid urls" in {
      val validUrls =
        Seq(
          ("http://habr.ru/post/11111/", "habr.ru"),
          ("https://tesT-1.net/dfdf", "tesT-1.net"),
          ("https://www.sdsd.ds-1S.ru/sdsd", "sdsd.ds-1S.ru"),
          ("http://www.sdsdds-1S.ru/sdsd", "sdsdds-1S.ru"),
          ("www.sdsdds-1S.ru/sdsd", "sdsdds-1S.ru"),
          ("sdsdds-1S.ru/sdsd", "sdsdds-1S.ru"),
          ("https://sdsdds-1S.net/sdsd", "sdsdds-1S.net"),
          ("yandex.ru", "yandex.ru"),
          ("www.unionauto.company", "unionauto.company"),
          ("https://sdsdds-1S.net?sas=as", "sdsdds-1S.net"),
          ("https://sdsdds-1S.net/asa?sas=asa", "sdsdds-1S.net"),
          ("https://sdsdds-1S.net/asa?sas=asa&dsf=sd", "sdsdds-1S.net"),
          ("https://sdsdds-1S.ru.com.net.net", "sdsdds-1S.ru.com.net.net")
        )
      validUrls.foreach(urlHostPair => ResourceServiceImpl.extractHost(urlHostPair._1).get should equal(urlHostPair._2))

    }

    "not return Success on invalid urls" in {
      val invalidUrls =
        Seq(
          "https://",
          "http://",
          "",
          "http:///",
          "https://dfs /",
          "https:dfs.net/",
          "https:/dfs.net/",
          "hsdsd:/dfs.net/",
          "https://dfsnet/"
        )
      invalidUrls.foreach(ResourceServiceImpl.extractHost(_) should be(None))
    }
  }
}
