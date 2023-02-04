package ru.auto.comeback.services.testkit

import ru.auto.api.comeback_model.ComebackListingRequest
import ru.auto.api.request_model.RequestPagination
import ru.auto.comeback.model.{Comeback, Filters}
import ru.auto.comeback.model.Comeback.{Comeback, NewComeback}
import ru.auto.comeback.storage.comeback.ComebackService
import ru.auto.comeback.storage.comeback.ComebackService.ComebackService
import zio.test.mock
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object ComebackServiceMock extends Mock[ComebackService] {

  object GetByIds extends Effect[List[Long], Throwable, List[Comeback]]

  object Get
    extends Effect[(Int, Filters, ComebackListingRequest.Sorting, Option[RequestPagination]), Throwable, List[Comeback]]

  object Count extends Effect[(Int, Filters), Throwable, Int]

  object FindCreatedBy extends Effect[Comeback.OfferRef, Throwable, List[Comeback]]

  object Insert extends Effect[(List[NewComeback], Boolean), Throwable, List[Comeback]]

  object Update extends Effect[(List[Comeback], Boolean), Throwable, Unit]

  override val compose: URLayer[Has[mock.Proxy], ComebackService] = ZLayer.fromService { proxy =>
    new ComebackService.Service {
      override def getByIds(ids: List[Long]): Task[List[Comeback]] = proxy(GetByIds, ids)

      override def get(
          clientId: Int,
          filters: Filters,
          sorting: ComebackListingRequest.Sorting,
          paging: Option[RequestPagination]): Task[List[Comeback]] = proxy(Get, clientId, filters, sorting, paging)

      override def count(clientId: Int, filters: Filters): Task[Int] = proxy(Count, clientId, filters)

      override def findCreatedBy(offerRef: Comeback.OfferRef): Task[List[Comeback]] = proxy(FindCreatedBy, offerRef)

      override def insert(comebacks: List[NewComeback], scheduleUpdateEvent: Boolean): Task[List[Comeback]] =
        proxy(Insert, comebacks, scheduleUpdateEvent)

      override def update(comebacks: List[Comeback], scheduleUpdateEvent: Boolean): Task[Unit] =
        proxy(Update, comebacks, scheduleUpdateEvent)
    }
  }
}
