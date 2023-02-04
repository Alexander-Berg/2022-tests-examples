package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.model.{GeoId, Operator, PhoneType, StatusCount}
import ru.yandex.vertis.telepony.service.OperatorNumberServiceV2.DistributionKey
import ru.yandex.vertis.telepony.service.{OperatorNumberServiceV2, PhoneStatisticsLoader}
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * Loads stats from DB on every call.
  * !!!For usage in tests only!!!
  *
  * @author zvez
  */
class PhoneStatisticsEagerLoader(operatorNumberServiceV2: OperatorNumberServiceV2) extends PhoneStatisticsLoader {

  implicit private val rc: RequestContext =
    AutomatedContext("pool-statistics-eager-loader")

  private def currentStatistics(): Map[DistributionKey, StatusCount] = {
    Await.result(operatorNumberServiceV2.statusDistributions(), 5.seconds)
  }

  override def statusCount(
      operator: Operator,
      originOperator: Operator,
      phoneType: PhoneType,
      geoId: GeoId): StatusCount = {
    currentStatistics()(DistributionKey(operator, originOperator, phoneType, geoId))
  }

  override def statusCount(operator: Operator, phoneType: PhoneType, geoId: GeoId): StatusCount = {
    val results = currentStatistics().collect {
      case (DistributionKey(`operator`, _, `phoneType`, `geoId`), value) => value
    }
    results.foldLeft(StatusCount.Empty)(_.merge(_))
  }

}
