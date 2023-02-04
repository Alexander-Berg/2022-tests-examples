package ru.yandex.vertis.parsing.auto.dao.legacy

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class UrlFixLegacyDaoTest extends FunSuite with MockitoSupport {

  private val mockedLegacyDao = mock[LegacyDao]
  private val urlFixLegacyDao = new LegacyDaoWrapper(mockedLegacyDao) with UrlFixLegacyDao

  implicit private val trace: Traced = TracedUtils.empty

  test("getStatus") {
    val urls = Seq(
      "https://m.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387",
      "https://barnaul.drom.ru/mazda/mazda6/32304521.html",
      "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af"
    )
    when(mockedLegacyDao.getStatus(?)(?)).thenReturn(
      Map(
        "https://www.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387" -> Some(1),
        "https://barnaul.drom.ru/mazda/mazda6/32304521.html" -> Some(2),
        "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af" -> Some(4)
      )
    )
    val result = urlFixLegacyDao.getStatus(urls)
    assert(
      result == Map(
        "https://m.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387" -> Some(1),
        "https://barnaul.drom.ru/mazda/mazda6/32304521.html" -> Some(2),
        "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af" -> Some(4)
      )
    )
    verify(mockedLegacyDao).getStatus(
      eq(
        Seq(
          "https://www.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387",
          "https://barnaul.drom.ru/mazda/mazda6/32304521.html",
          "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af"
        )
      )
    )(any())
  }

  test("setSent") {
    val urls = Seq(
      "https://m.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387",
      "https://barnaul.drom.ru/mazda/mazda6/32304521.html",
      "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af"
    )
    when(mockedLegacyDao.setSent(?)(?)).thenReturn(
      Map(
        "https://www.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387" -> true,
        "https://barnaul.drom.ru/mazda/mazda6/32304521.html" -> true,
        "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af" -> true
      )
    )
    val result = urlFixLegacyDao.setSent(urls)
    assert(
      result == Map(
        "https://m.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387" -> true,
        "https://barnaul.drom.ru/mazda/mazda6/32304521.html" -> true,
        "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af" -> true
      )
    )
    verify(mockedLegacyDao).setSent(
      eq(
        Seq(
          "https://www.avito.ru/odintsovo/avtomobili/volvo_xc70_2015_804279387",
          "https://barnaul.drom.ru/mazda/mazda6/32304521.html",
          "https://auto.youla.ru/advert/used/nissan/qashqai/prv--18e61779bbcd67af"
        )
      )
    )(any())
  }
}
