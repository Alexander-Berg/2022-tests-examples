package ru.yandex.vertis.billing.service.cached

import org.scalatest.Ignore
import org.mockito.Mockito.{times, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory
import ru.yandex.vertis.billing.dao.{LimitDao, OrderDao}
import ru.yandex.vertis.billing.model_core.LimitSetting
import ru.yandex.vertis.billing.model_core.gens.{CampaignIdGen, Producer}
import ru.yandex.vertis.billing.service.LimitService
import ru.yandex.vertis.billing.service.cached.CachedLimitServiceSpec._
import ru.yandex.vertis.billing.service.impl.LimitServiceImpl
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, startOfToday}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * @author @logab
  */
@Ignore
class CachedLimitServiceSpec extends AnyWordSpec with Matchers with MockitoSupport {
  val log = LoggerFactory.getLogger(classOf[CachedLimitServiceSpec])

  "CachedLimitService" should {
    "cache" in {
      val (dao, limitService) = suite()
      val firstSample = CampaignIdGen.next
      limitService.getCurrent(firstSample, current)
      verify(dao, times(1)).getAll(?)
      limitService.getCurrent(firstSample, current)
      verify(dao, times(1)).getAll(?)
      val secondSample = CampaignIdGen.next
      limitService.getCurrent(secondSample, current)
      verify(dao, times(2)).getAll(?)
      limitService.getCurrent(secondSample, current)
      verify(dao, times(2)).getAll(?)
    }

    "get only new keys from dao" in {
      val (dao, limitService) = suite()
      val first = CampaignIdGen.next(10)
      val second = first.drop(5) ++ CampaignIdGen.next(5)
      val current1 = limitService.getAllCurrent(first, current)
      verify(dao, times(1)).getAll(?)
      val current2 = limitService.getAllCurrent(second, current)
      verify(dao, times(2)).getAll(?)
      for {
        firstElements <- current1
        secondElements <- current2
      } yield {
        (firstElements.keySet & secondElements.keySet).foreach(o => {
          firstElements(o) should ===(secondElements(o))
        })
      }
    }
  }

}

object CachedLimitServiceSpec {

  import ru.yandex.vertis.mockito.MockitoSupport.{mock, stub}

  val current = now()

  def suite(): (LimitDao, LimitService) = {
    val limitDao = mock[LimitDao]
    val orderDao = mock[OrderDao]
    stub(limitDao.getAll _) { case ids =>
      Success(
        ids
          .map(o => {
            val daily = LimitSetting.Daily(100, startOfToday())
            val weekly = LimitSetting.Weekly(1000, startOfToday())
            o -> Seq(daily, weekly)
          })
          .toMap
      )
    }

    val limitService = new LimitServiceImpl(limitDao, orderDao) with CachedLimitService {
      override val support: Cache = new NeverExpireCache()

      override def serviceNamespace: String = "test"
    }
    (limitDao, limitService)
  }
}
