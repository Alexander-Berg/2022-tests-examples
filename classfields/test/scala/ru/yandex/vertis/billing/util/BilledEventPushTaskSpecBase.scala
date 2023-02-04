package ru.yandex.vertis.billing.util

import org.scalacheck.Gen
import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.dao.BilledEventDao.{BilledEventInfoWithPayload, GetFilter}
import ru.yandex.vertis.billing.dao.{BilledEventDao, BilledEventDivisionDaoResolver, DivisionDaoResolver}
import ru.yandex.vertis.billing.model_core.{
  BilledEventDivision,
  DefaultProductKey,
  EnrichedBilledEventInfo,
  EpochWithId,
  Order,
  OrderId,
  SupportedBilledEventDivisions
}
import ru.yandex.vertis.billing.model_core.Division.Projects
import ru.yandex.vertis.billing.model_core.gens.{OrderGen, Producer}
import ru.yandex.vertis.mockito.MockitoSupport
import BilledEventPushTaskSpecBase.{addProductKey, enrich}
import ru.yandex.vertis.billing.util.mock.TypedKeyValueServiceMockBuilder

import slick.jdbc
import scala.util.{Success, Try}

/**
  * @author tolmach
  */
trait BilledEventPushTaskSpecBase extends BilledEventsTestingHelpers with MockitoSupport {

  case class PreparedData(
      domain: String,
      neededDivisionsWithInfoMap: Map[BilledEventDivision, Iterable[BilledEventInfoWithPayload]],
      billedEventDaoResolver: BilledEventDivisionDaoResolver,
      ordersMap: Map[OrderId, Order],
      enrichedWrappedMap: Map[BilledEventDivision, Iterable[Option[EnrichedBilledEventInfo]]])

  protected def runOnPreparedData(
      database: jdbc.JdbcBackend.Database,
      payloadsPerDivision: Int,
      batchSize: Int
    )(action: PreparedData => Unit): Unit = {
    val divisionWithBilledEventPayloadMap = makeDivisionWithBilledEventPayloadMap(payloadsPerDivision)

    val eventDaoResolver = testingEventDaoResolver(database)

    SupportedServices.Values.foreach { domain =>
      val neededDivisionsWithInfoMap = divisionWithBilledEventPayloadMap.filter { case (division, _) =>
        division.project == domain
      }

      val divisionsWithPreparedInfoMap = neededDivisionsWithInfoMap.map { case (division, infoWithPayload) =>
        val preparedInfo = removePartOfPayloads(infoWithPayload, 4)
        (division, preparedInfo)
      }

      val divisionsWithPayloadMap = divisionsWithPreparedInfoMap.map { case (division, preparedInfo) =>
        val payloads = preparedInfo.flatMap(_.payload)
        (division, payloads)
      }.toSet

      fillEventDaos(eventDaoResolver, divisionsWithPayloadMap).get

      val divisionBilledEventDaoMap = divisionsWithPreparedInfoMap.map { case (division, rawSources) =>
        val billedEventDao = {
          val m = mock[BilledEventDao]
          val groupedSource = rawSources.grouped(batchSize).toSeq
          var completedSource = if (groupedSource.last.size == batchSize) {
            groupedSource :+ Seq.empty
          } else {
            groupedSource
          }
          stub(m.getWithPayload(_: GetFilter)) { case _ =>
            val result = Success(completedSource.head)
            completedSource = completedSource.tail
            result
          }
          m
        }
        division -> billedEventDao
      }

      val billedEventDaoResolver = {
        val m = mock[BilledEventDivisionDaoResolver]
        stub(m.resolve(_: BilledEventDivision)) { case d =>
          Try(divisionBilledEventDaoMap(d))
        }
        m
      }

      val orderIds = divisionsWithPreparedInfoMap.flatMap { case (_, infos) =>
        val transactions = infos.map(_.billedEventInfo.transaction)
        transactions.map(_.getOrderId)
      }.toSet

      val ordersMap = orderIds.map { orderId =>
        val order = OrderGen.next
        val withCorrectOrderId = order.copy(id = orderId)
        val withProductKey = addProductKey(domain, Seq(withCorrectOrderId))
        val correctOrder = withProductKey.head
        orderId -> correctOrder
      }.toMap

      val enrichedWrappedMap = divisionsWithPreparedInfoMap.map { case (division, rawSources) =>
        val enriched = rawSources.map(enrich(division, _, ordersMap))
        division -> enriched
      }

      val preparedData =
        PreparedData(domain, neededDivisionsWithInfoMap, billedEventDaoResolver, ordersMap, enrichedWrappedMap)

      action(preparedData)
    }
  }

  protected def runOnEmptyPreparedData(action: PreparedData => Unit): Unit = {
    SupportedServices.Values.foreach { domain =>
      val neededDivisions = SupportedBilledEventDivisions.Values.filter(_.project == domain)

      val neededDivisionsWithInfoMap = neededDivisions.map { division =>
        division -> Seq.empty[BilledEventInfoWithPayload]
      }.toMap

      val divisionBilledEventDaoMap = neededDivisions.map { division =>
        val billedEventDao = {
          val m = mock[BilledEventDao]

          stub(m.getWithPayload(_: GetFilter)) { case _ =>
            Success(Seq.empty)
          }
          m
        }
        division -> billedEventDao
      }.toMap

      val billedEventDaoResolver = {
        val m = mock[BilledEventDivisionDaoResolver]
        stub(m.resolve(_: BilledEventDivision)) { case d =>
          Try(divisionBilledEventDaoMap(d))
        }
        m
      }

      val ordersMap = Map.empty[OrderId, Order]

      val enrichedWrappedMap = Map.empty[BilledEventDivision, Iterable[Option[EnrichedBilledEventInfo]]]

      val preparedData =
        PreparedData(domain, neededDivisionsWithInfoMap, billedEventDaoResolver, ordersMap, enrichedWrappedMap)

      action(preparedData)
    }
  }

  protected def prepare(
      builder: TypedKeyValueServiceMockBuilder,
      neededDivisionsWithInfoMap: Map[BilledEventDivision, Iterable[BilledEventInfoWithPayload]],
      batchSize: Int,
      toEpochMarker: BilledEventDivision => String): TypedKeyValueServiceMockBuilder = {
    neededDivisionsWithInfoMap.foldLeft(builder) { case (builder, (division, infoWithPayload)) =>
      val epochMarker = toEpochMarker(division)
      val withGet = builder.withGetMock[EpochWithId](epochMarker, EpochWithId(0L, None))
      val batches = infoWithPayload.grouped(batchSize)
      batches.foldLeft(withGet) { case (builder, batch) =>
        val last = batch.last.billedEventInfo
        val expected = EpochWithId(last.epoch.get, Some(last.id))
        builder.withSetMock[EpochWithId](epochMarker, expected)
      }
    }
  }

}

object BilledEventPushTaskSpecBase {

  private def addProductKey(domain: String, orders: Iterable[Order]): Iterable[Order] = {
    Projects.withName(domain) match {
      case Projects.AutoRu =>
        orders.map { order =>
          val pr = order.properties.copy(productKey = DefaultProductKey)
          order.copy(properties = pr)
        }
      case _ =>
        orders
    }
  }

  private def enrich(
      division: BilledEventDivision,
      b: BilledEventInfoWithPayload,
      ordersMap: Map[OrderId, Order]): Option[EnrichedBilledEventInfo] = {
    b match {
      case BilledEventInfoWithPayload(billedEventInfo, payload)
          if payload.isDefined || billedEventInfo.callfact.isDefined =>
        val orderId = billedEventInfo.transaction.getOrderId
        val order = ordersMap(orderId)
        val customerId = order.owner
        val project = Projects.withName(division.project)
        val enriched = EnrichedBilledEventInfo(
          billedEventInfo,
          customerId,
          project,
          division.component,
          order,
          payload
        )
        Some(enriched)
      case _ =>
        None
    }
  }

}
