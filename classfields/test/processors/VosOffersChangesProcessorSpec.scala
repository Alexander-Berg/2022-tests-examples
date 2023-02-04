package auto.dealers.amoyak.logic.processors

import auto.dealers.amoyak.consumers.logic.DefaultVosOffersChangesProcessor.availableStatuses
import auto.dealers.amoyak.consumers.logic.VosOffersChangesProcessor
import cats.implicits._
import auto.common.clients.vos.Vos
import auto.dealers.amoyak.storage.testkit.ClientsChangedBufferMock
import ru.auto.api.api_offer_model.{Offer, OfferStatus}
import ru.auto.api.diff_log_model.{DiffItem, OfferChangeEvent, OfferStatusDiff}
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import auto.dealers.amoyak.storage.dao.ClientsChangedBufferDao._
import zio.test.mock.Expectation._
import zio.test.Assertion._

object VosOffersChangesProcessorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("VosOffersChangesProcessor") {
      testM("process should save InsertRecord to ClientsChangedBuffer") {
        import auto.dealers.amoyak.consumers.logic.DefaultVosOffersChangesProcessor._

        val oldOffers: List[Offer] = List(
          Offer(userRef = Vos.OwnerId.DealerId(1L).toString, status = OfferStatus.ACTIVE),
          Offer(userRef = Vos.OwnerId.DealerId(2L).toString, status = OfferStatus.STATUS_UNKNOWN),
          Offer(userRef = Vos.OwnerId.DealerId(3L).toString, status = OfferStatus.BANNED),
          Offer(userRef = Vos.OwnerId.DealerId(4L).toString, status = OfferStatus.DRAFT),
          Offer(userRef = Vos.OwnerId.UserId(5L).toString, status = OfferStatus.ACTIVE),
          Offer(userRef = Vos.OwnerId.DealerId(6L).toString, status = OfferStatus.EXPIRED)
        )

        val newOffers: List[Offer] = List(
          Offer(userRef = Vos.OwnerId.DealerId(1L).toString, status = OfferStatus.BANNED),
          Offer(userRef = Vos.OwnerId.DealerId(2L).toString, status = OfferStatus.BANNED),
          Offer(userRef = Vos.OwnerId.DealerId(3L).toString, status = OfferStatus.REMOVED),
          Offer(userRef = Vos.OwnerId.DealerId(4L).toString, status = OfferStatus.NEED_ACTIVATION),
          Offer(userRef = Vos.OwnerId.UserId(5L).toString, status = OfferStatus.BANNED),
          Offer(userRef = Vos.OwnerId.DealerId(6L).toString, status = OfferStatus.EXPIRED)
        )

        val offerChangeEvents = oldOffers.zip(newOffers).map { case (oldOffer, newOffer) =>
          val diff: Option[DiffItem] = {
            if (
              oldOffer.status != newOffer.status &&
              Seq(oldOffer.status, newOffer.status).exists(availableStatuses)
            ) {
              val offerStatusDiff: OfferStatusDiff = OfferStatusDiff(oldOffer.status, newOffer.status)
              Some(DiffItem(status = Some(offerStatusDiff)))
            } else None
          }

          OfferChangeEvent(
            newOffer = newOffer.some,
            oldOffer = oldOffer.some,
            diff = diff
          )
        } :+ OfferChangeEvent(
          newOffer = Offer(userRef = Vos.OwnerId.DealerId(7L).entryName, status = OfferStatus.ACTIVE).some,
          oldOffer = None,
          diff = Some(DiffItem(status = Some(OfferStatusDiff(newValue = OfferStatus.ACTIVE))))
        )

        val expected: Seq[InsertRecord] = Seq(
          InsertRecord(clientId = 1L, dataSource = DataSource.Vos.entryName),
          InsertRecord(clientId = 2L, dataSource = DataSource.Vos.entryName),
          InsertRecord(clientId = 3L, dataSource = DataSource.Vos.entryName),
          InsertRecord(clientId = 7L, dataSource = DataSource.Vos.entryName)
        )

        val clientChangedBufferMock = ClientsChangedBufferMock.Add(hasSameElements(expected), unit)

        val processor = clientChangedBufferMock >>> VosOffersChangesProcessor.live

        val process = VosOffersChangesProcessor
          .process(offerChangeEvents)

        assertM(process)(isUnit)
          .provideLayer(TestEnvironment.live ++ processor)
      }
    }

  }
}
