package ru.yandex.vertis.billing.util

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.billing.dao.BilledEventDao.BilledEventInfoWithPayload
import ru.yandex.vertis.billing.dao.DuplicationPolicyResolver.Default
import ru.yandex.vertis.billing.dao.impl.jdbc.{DualDatabase, JdbcEventDivisionDao}
import ru.yandex.vertis.billing.dao.{DivisionDaoResolver, EventDivisionDao}
import ru.yandex.vertis.billing.model_core.Division.Components
import ru.yandex.vertis.billing.model_core.Division.Components.PhoneShow
import ru.yandex.vertis.billing.model_core.Division.Locales.Ru
import ru.yandex.vertis.billing.model_core.Division.Projects.AutoRu
import ru.yandex.vertis.billing.model_core.gens.{
  orderTransactionGen,
  OfferIdGen,
  OrderTransactionGenParams,
  PayloadGen,
  Producer
}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.model_core.{
  BilledEventDivision,
  BilledEventInfo,
  CostPerCall,
  CostPerClick,
  CostPerIndexing,
  Division,
  FixPrice,
  Highlighting,
  OrderTransactions,
  Payload,
  Product,
  SupportedBilledEventDivisions,
  SupportedDivisions,
  Withdraw2
}
import ru.yandex.vertis.util.collection.TryUtil
import slick.jdbc
import slick.jdbc.JdbcBackend

import scala.util.Try

trait BilledEventsTestingHelpers {

  protected def makeDivisionWithBilledEventPayloadMap(
      payloadCount: Int): Map[BilledEventDivision, Iterable[BilledEventInfoWithPayload]] = {
    divisionWithPayloads(payloadCount).map { case (division, payloads) =>
      division -> billedEventWithPayload(division, payloads)
    }.toMap
  }

  private def divisionWithPayloads(payloadCount: Int): Set[(BilledEventDivision, Iterable[Payload])] = {
    SupportedBilledEventDivisions.Values.map { d =>
      (d, PayloadGen.next(payloadCount / SupportedBilledEventDivisions.Values.size))
    }
  }

  protected def filledTestingEventDaos(
      database: jdbc.JdbcBackend.Database,
      source: Set[(BilledEventDivision, Iterable[Payload])]): Try[DivisionDaoResolver] = Try {
    val resolver = testingEventDaoResolver(database)
    fillEventDaos(resolver, source).get
    resolver
  }

  protected def fillEventDaos(
      resolver: DivisionDaoResolver,
      source: Set[(BilledEventDivision, Iterable[Payload])]): Try[Unit] = {
    TryUtil
      .traverse(source) { case (division, payload) =>
        resolver.resolve(division.connected).flatMap(_.write(payload))
      }
      .map(_ => ())
  }

  private def componentToProduct(component: String): Product = {
    val price = if (component == Components.Click.toString) {
      CostPerClick(FixPrice(100L))
    } else if (component == Components.Indexing.toString) {
      CostPerIndexing(FixPrice(100L))
    } else if (component == Components.PhoneShow.toString) {
      CostPerCall(FixPrice(100L))
    } else {
      throw new IllegalArgumentException(s"Unexpected component: $component")
    }
    Product(Highlighting(price))
  }

  private val WithdrawGen = {
    for {
      tr <- orderTransactionGen(OrderTransactionGenParams().withType(OrderTransactions.Withdraw))
      timestamp <- Gen.choose(
        DateTimeUtils.now().minusDays(62).getMillis,
        DateTimeUtils.now().getMillis
      )
      changedTr = tr match {
        case w: Withdraw2 =>
          val changedSnapshot = w.snapshot.copy(
            time = DateTimeUtils.fromMillis(timestamp)
          )
          Withdraw2(
            w.id,
            changedSnapshot,
            w.amount,
            w.details,
            w.epoch
          )
        case other =>
          throw new IllegalArgumentException(s"Unexpected transaction: $other")
      }
    } yield changedTr
  }

  protected def billedEventWithPayload(
      division: BilledEventDivision,
      payloads: Iterable[Payload]): Iterable[BilledEventInfoWithPayload] = {
    val source = payloads.map { p =>
      val tr = WithdrawGen.next(1).head
      val changedTr = tr match {
        case w: Withdraw2 =>
          val changedProduct = componentToProduct(division.component)
          w.copy(snapshot = w.snapshot.copy(product = changedProduct))
        case other =>
          throw new IllegalArgumentException(s"Unexpected transaction: $other")
      }
      val changedP = p.copy(timestamp = changedTr.timestamp)
      val offerId = Gen.option(OfferIdGen).next
      val epoch = Some(DateTime.now().getMillis)
      val info = BilledEventInfo(
        Some(changedP.id),
        offerId,
        None,
        changedTr.timestamp,
        changedTr.amount,
        changedTr.amount,
        Conversions.toMessage(changedTr).get,
        None,
        epoch
      )
      BilledEventInfoWithPayload(info, Some(changedP))
    }
    source.groupBy(_.payload.get.id).map(_._2.head).toSeq.sortBy(_.billedEventInfo.epoch.get)
  }

  protected def removePartOfPayloads(
      source: Iterable[BilledEventInfoWithPayload],
      neededSize: Int): Iterable[BilledEventInfoWithPayload] = {
    val size = source.size
    val partSize = size / neededSize
    val part = source.take(partSize).map { case BilledEventInfoWithPayload(b, _) =>
      BilledEventInfoWithPayload(b, None)
    }
    part ++ source.drop(partSize)
  }

  protected def testingEventDaoResolver(database: JdbcBackend.DatabaseDef): DivisionDaoResolver = {
    val TestingSupportedDivisions = SupportedDivisions.Values + Division(AutoRu, Ru, PhoneShow)

    new DivisionDaoResolver {
      val map =
        TestingSupportedDivisions
          .map(d =>
            (
              d,
              new JdbcEventDivisionDao(
                DualDatabase(database, database),
                d.identity,
                Default.resolve(d).dao
              )
            )
          )
          .toMap

      override def resolve(d: Division): Try[EventDivisionDao] =
        Try(map(d))

      override def all: Iterable[(Division, EventDivisionDao)] =
        map
    }
  }

}
