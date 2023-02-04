package ru.auto.comeback.services.test

import auto.common.manager.region.RegionManager
import auto.common.manager.region.testkit.RegionManagerMock
import common.zio.logging.Logging
import ru.auto.api.comeback_model.ComebackListingRequest
import ru.auto.api.request_model.RequestPagination
import ru.auto.api.response_model.Pagination
import ru.auto.comeback.model.Comeback.{Comeback, NewComeback}
import ru.auto.comeback.model.testkit._
import ru.auto.comeback.model.{Comeback, Filters}
import ru.auto.comeback.proto.comeback_service.{ComebackListResponse, ErrorCode, ResponseStatus}
import ru.auto.comeback.services.ComebackModelConverter.comebackToProto
import ru.auto.comeback.services.excel.ComebackExportService
import ru.auto.comeback.services.{ComebackManager, LiveComebackManager, PaymentsService}
import ru.auto.comeback.storage.comeback.ComebackService
import zio.test.Assertion.{anything, equalTo, hasSameElements}
import zio.test.TestAspect.{samples, shrinks}
import zio.test._
import zio.test.mock.Expectation._
import zio.{Has, Task, URIO, ZIO}

import java.time.Instant
import java.time.temporal.ChronoUnit

object ComebackManagerSpec extends DefaultRunnableSpec {

  private def db(comebacks: List[Comeback]): ComebackService.Service =
    new ComebackService.Service {

      override def get(
          clientId: Int,
          filters: Filters,
          sorting: ComebackListingRequest.Sorting,
          paging: Option[RequestPagination]): Task[List[Comeback]] = ZIO.succeed(comebacks)

      override def count(clientId: Int, filters: Filters): Task[Int] =
        ZIO.succeed(comebacks.size)

      override def findCreatedBy(
          offerRef: Comeback.OfferRef): Task[List[Comeback]] = ???

      override def insert(comebacks: List[NewComeback], scheduleUpdateEvent: Boolean): Task[List[Comeback]] =
        ???

      override def update(comebacks: List[Comeback], scheduleUpdateEvent: Boolean): Task[Unit] = ???

      override def getByIds(ids: List[Long]): Task[List[Comeback]] = ???
    }

  private val exportService = new ComebackExportService.Service {

    override def exportComebacks(clientId: Int, comebacks: List[Comeback]): Task[String] =
      ZIO.succeed("url")
  }

  private def paymentsService(
      provideRegularPayments: Boolean): PaymentsService.Service = new PaymentsService.Service {

    override def hasRegularPayments(clientId: Long): Task[Boolean] =
      Task(provideRegularPayments)

    override def requireRegularPayments[R](
        clientId: Long
      )(onSuccess: => Task[R],
        onFail: => Task[R]): Task[R] =
      if (provideRegularPayments) onSuccess else onFail
  }

  private def comebackManager(
      db: ComebackService.Service,
      hasRequiredPayments: Boolean): URIO[Has[RegionManager] with Logging.Logging, ComebackManager.Service] =
    for {
      logging <- ZIO.service[Logging.Service]
      regionManager <- ZIO.service[RegionManager]
    } yield new LiveComebackManager(
      db,
      exportService,
      paymentsService(hasRequiredPayments),
      regionManager,
      logging
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("ComebackManager")(
      testM("listComebacks returns error if payments are irregular") {
        checkM(
          ComebackGen.existingComebacksFromOneClient,
          ExternalFiltersGen.anyComebackFilter,
          CommonGen.anySorting,
          CommonGen.anyPagination
        ) { case ((clientId, comebacks), filters, sorting, pagination) =>
          for {
            manager <- comebackManager(
              db(comebacks),
              hasRequiredPayments = false
            )
            expected <- ZIO.succeed(
              new ComebackListResponse(
                comebacks = Nil,
                pagination = Some(
                  Pagination(pagination.page, pagination.pageSize, 0, 0)
                ),
                error = ErrorCode.COMEBACK_REGULAR_PAYMENT_REQUIRED,
                status = ResponseStatus.ERROR
              )
            )
            result <- manager.listComebacks(
              clientId,
              filters,
              pagination,
              sorting
            )
          } yield assert(result)(equalTo(expected))
        }
      },
      testM("listComebacks returns success") {
        checkM(
          ComebackGen.existingComebacksFromOneClient,
          ExternalFiltersGen.anyComebackFilter,
          CommonGen.anySorting,
          CommonGen.anyPagination
        ) { case ((clientId, comebacks), filters, sorting, pagination) =>
          val totalPageCount = (comebacks.size - 1) / pagination.pageSize + 1
          for {
            manager <- comebackManager(
              db(comebacks),
              hasRequiredPayments = true
            )
            result <- manager
              .listComebacks(clientId, filters, pagination, sorting)
          } yield assert(result.comebacks)(
            hasSameElements(comebacks.map(comebackToProto))
          ) && assertTrue {
            result.status == ResponseStatus.SUCCESS &&
            result.pagination.contains(
              Pagination(
                pagination.page,
                pagination.pageSize,
                comebacks.size,
                totalPageCount
              )
            )
          }
        }
      },
      testM("exportComebacks returns error if payments are irregular") {
        checkM(
          ComebackGen.existingComebacksFromOneClient,
          ExternalFiltersGen.anyComebackFilter,
          CommonGen.anySorting
        ) { case ((clientId, comebacks), filters, sorting) =>
          for {
            manager <- comebackManager(
              db(comebacks),
              hasRequiredPayments = false
            )
            result <- manager.exportComebacks(clientId, filters, sorting)
          } yield assertTrue {
            result.downloadUri.isBlank &&
            result.status == ResponseStatus.ERROR &&
            result.error == ErrorCode.COMEBACK_REGULAR_PAYMENT_REQUIRED
          }
        }
      },
      testM("exportComebacks return success") {
        checkM(
          ComebackGen.existingComebacksFromOneClient,
          ExternalFiltersGen.anyComebackFilter,
          CommonGen.anySorting
        ) { case ((clientId, comebacks), filters, sorting) =>
          for {
            manager <- comebackManager(
              db(comebacks),
              hasRequiredPayments = true
            )
            result <- manager.exportComebacks(clientId, filters, sorting)
          } yield assertTrue {
            result.downloadUri.nonEmpty &&
            result.status == ResponseStatus.SUCCESS
          }
        }
      },
      testM(
        "exportComebacks returns error if filter contains date range more than 180 days"
      ) {
        checkM(
          ComebackGen.existingComebacksFromOneClient,
          ExternalFiltersGen.anyComebackFilter,
          CommonGen.anySorting
        ) { case ((clientId, comebacks), filters, sorting) =>
          val tooLongFilter = {
            val creationDateTo = Instant
              .parse("2015-01-01T00:00:00Z")
              .truncatedTo(ChronoUnit.MILLIS)
            val creationDateFrom = creationDateTo.minus(181, ChronoUnit.DAYS)
            filters.copy(
              creationDateFrom = Some(creationDateFrom.toEpochMilli),
              creationDateTo = Some(creationDateTo.toEpochMilli)
            )
          }
          for {
            manager <- comebackManager(
              db(comebacks),
              hasRequiredPayments = true
            )
            result <- manager
              .exportComebacks(clientId, tooLongFilter, sorting)
          } yield assertTrue {
            result.downloadUri.isBlank &&
            result.status == ResponseStatus.ERROR &&
            result.error == ErrorCode.COMEBACK_EXPORT_PERIOD_FILTER_IS_TOO_LONG
          }
        }
      }
    ) @@ samples(30) @@ shrinks(0)
  }.provideCustomLayerShared(
    Logging.live ++ RegionManagerMock.FindNearestSettlements(anything, valueF { case (rid, _) => rid.toSet }).atLeast(2)
  )
}
