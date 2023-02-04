package ru.yandex.vertis.telepony.service

import akka.actor.Scheduler
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.Operators.{Mts, Mtt}
import ru.yandex.vertis.telepony.model.PhoneTypes.{Local, Mobile}
import ru.yandex.vertis.telepony.model.{GeoId, StatusCount}
import ru.yandex.vertis.telepony.model.StatusValues.Ready
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2.DistributionKey
import ru.yandex.vertis.telepony.service.impl.PhoneStatisticsBackgroundLoader

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author neron
  */
trait PhoneStatisticsLoaderSpec extends SpecBase with MockitoSupport {

  private val Msk: GeoId = 1

  implicit def scheduler: Scheduler

  override def spanScaleFactor: Double = 2.0

  "pool statistics loader" should {
    "return fresh stats" in {
      val operatorNumberService = mock[OperatorNumberServiceV2]
      when(operatorNumberService.statusDistributions()(?))
        .thenReturn(
          Future.successful(
            Map(DistributionKey(Mtt, Mtt, Mobile, Msk) -> StatusCount(Map(Ready -> 10)))
          )
        )
      val loader = new PhoneStatisticsBackgroundLoader(operatorNumberService, 100.millis)
      eventually {
        loader.statusCount(Mtt, Mobile, Msk) shouldEqual StatusCount(Map(Ready -> 10))
        loader.statusCount(Mtt, Local, Msk) shouldEqual StatusCount.Empty
        loader.statusCount(Mts, Mobile, Msk) shouldEqual StatusCount.Empty
        loader.statusCount(Mts, Local, Msk) shouldEqual StatusCount.Empty
      }
    }
  }
}
