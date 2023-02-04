package ru.auto.comeback.storage

import ru.auto.api.comeback_model.ComebackListingRequest.Sorting
import ru.auto.api.request_model.RequestPagination
import ru.auto.comeback.model.Comeback.{Comeback, NewComeback, OfferRef}
import ru.auto.comeback.model.testkit.Fits
import ru.auto.comeback.model.{Filters, Status}
import ru.auto.comeback.storage.comeback.ComebackService
import zio._

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 20/01/2020
  */
object TestComebackDao {
  type TestComebackDao = Has[Service]

  trait Service extends ComebackService.Service {
    def allComebacks: RIO[Any, Seq[Comeback]]
  }

  def make: UIO[TestService] =
    Ref.make(List.empty[Comeback]).zipWith(Ref.make(0L))(TestService)

  val live: ZLayer[Any, Nothing, Has[TestService]] = ZLayer.fromEffect(make)

  case class TestService(data: Ref[List[Comeback]], counter: Ref[Long]) extends Service {
    override def allComebacks: RIO[Any, Seq[Comeback]] = data.get

    override def get(
        clientId: Int,
        filters: Filters,
        sorting: Sorting,
        paging: Option[RequestPagination]): Task[List[Comeback]] = {
      data.get
        .map { list =>
          val res = list
            .filter(_.status != Status.Hidden)
            .filter(Fits(filters, _))
            .sortBy(cb =>
              sorting match {
                case Sorting.COMEBACK_DURATION => cb.meta.comebackAfter.toMillis
                case Sorting.CREATION_DATE => -cb.offer.activated.toEpochMilli
                case Sorting.LAST_EVENT_DATE_DESC => cb.meta.lastEventDate.toEpochMilli
                case Sorting.Unrecognized(value) => sys.error(s"Unexpected Sorting: $value")
              }
            )
          paging.fold(res)(p =>
            res.slice(
              (p.page - 1) * p.pageSize,
              (p.page - 1) * p.pageSize + p.pageSize
            )
          )
        }
    }

    override def count(clientId: Int, filters: Filters): Task[Int] = {
      data.get.map(_.filter(_.status != Status.Hidden).count(Fits(filters, _)))
    }

    override def findCreatedBy(offerRef: OfferRef): Task[List[Comeback]] = {
      data.get.map(_.filter(_.offer.ref == offerRef))
    }

    override def insert(comebacks: List[NewComeback], scheduleComebackUpdateEvent: Boolean): Task[List[Comeback]] = {
      for {
        toInsert <- ZIO.foreach(comebacks)(cb => counter.updateAndGet(_ + 1).map(cb.withId))
        _ <- data.update(_ ++ toInsert)
      } yield toInsert
    }

    override def update(comebacks: List[Comeback], scheduleComebackUpdateEvent: Boolean): Task[Unit] = {
      data.update { list =>
        val updateMap = comebacks.map(c => c.id -> c).toMap
        list.map(c => updateMap.getOrElse(c.id, c))
      }.unit
    }

    override def getByIds(ids: List[Long]): Task[List[Comeback]] = ???
  }
}
