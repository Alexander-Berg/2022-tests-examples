package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.billing.dao.CampaignHistoryDao
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.Filter.WithEventTypeBatched
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.{CampaignHistoryPoint, EventType, Filter}

import scala.util.{Failure, Success, Try}

/**
  * @author tolmach
  */
case class CampaignHistoryDaoMockBuilder(
    getWithEventTypeStub: Option[PartialFunction[Filter, Try[Iterable[CampaignHistoryPoint]]]] = None,
    getFailOnAny: Option[Throwable] = None,
    swapTypeBatchStubs: Seq[PartialFunction[Seq[CampaignHistoryPoint], Try[Unit]]] = Seq.empty)
  extends MockBuilder[CampaignHistoryDao] {

  def withGetWithEventTypeBatched(
      eventType: EventType,
      batches: Seq[Seq[CampaignHistoryPoint]],
      batchSize: Int): CampaignHistoryDaoMockBuilder = {
    val filter = WithEventTypeBatched(eventType, batchSize)
    var acc = batches
    val stub: PartialFunction[Filter, Try[Iterable[CampaignHistoryPoint]]] = {
      case `filter` if acc.nonEmpty =>
        val current = acc.head
        acc = acc.tail
        Success(current)
    }
    this.copy(getWithEventTypeStub = Some(stub))
  }

  def withSwapTypeBatch(batch: Seq[CampaignHistoryPoint]): CampaignHistoryDaoMockBuilder = {
    if (getFailOnAny.isDefined) {
      throw new IllegalArgumentException("Cannot mock get filter cause it already mocked to fail on any")
    } else {
      val stub: PartialFunction[Seq[CampaignHistoryPoint], Try[Unit]] = {
        case actualBatch if actualBatch.size == batch.size && actualBatch.forall(batch.contains) =>
          Success(())
      }
      this.copy(swapTypeBatchStubs = this.swapTypeBatchStubs :+ stub)
    }
  }

  def withGetFailOnAny(throwable: Throwable): CampaignHistoryDaoMockBuilder = {
    if (getWithEventTypeStub.isDefined) {
      throw new IllegalArgumentException("Cannot mock fail on any filter cause it already mocked")
    } else {
      this.copy(getFailOnAny = Some(throwable))
    }
  }

  override def build: CampaignHistoryDao = {
    val m: CampaignHistoryDao = mock[CampaignHistoryDao]

    getWithEventTypeStub.foreach { getWithEventTypeStub =>
      stub(m.getTry(_: Filter))(getWithEventTypeStub)
    }

    getFailOnAny.foreach { throwable =>
      stub(m.getTry(_: Filter)) { case _ => Failure(throwable) }
    }

    val swapTypeBatchStub = swapTypeBatchStubs.reduceOption(_.orElse(_))
    swapTypeBatchStub.foreach { swapTypeBatchStub =>
      stub(m.swapTypeBatch(_: Seq[CampaignHistoryPoint]))(swapTypeBatchStub)
    }

    m
  }

}
