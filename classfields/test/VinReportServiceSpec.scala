package ru.auto.comeback.consumer.test

import java.time.Instant
import com.google.protobuf.timestamp.Timestamp
import auto.common.clients.carfax.Carfax
import ru.auto.api.vin.vin_report_model.HistoryBlock.HistoryRecord.Record.{
  AutoServiceRecord,
  EstimateRecord,
  InsuranceRecord,
  SellAutoEventRecord
}
import ru.auto.api.vin.vin_report_model.HistoryBlock.{AutoServiceHistoryRecord, HistoryRecord, OwnerHistory}
import ru.auto.api.vin.vin_report_model.RecordMeta.SourceMeta
import ru.auto.api.vin.vin_report_model.{
  EstimateItem,
  HistoryBlock,
  InsuranceItem,
  RawVinEssentialsReport,
  RecordMeta,
  SellAutoEventItem
}
import ru.auto.api.vin.orders.orders_api_model.{OrderIdentifierType, PublicOrderModel, ReportType}
import ru.auto.api.vin.orders.orders_api_model.PublicOrderModel.{ReportTypeOrId, Status}
import ru.auto.api.vin.orders.request_model.GetOrdersListRequest
import ru.auto.comeback.VinHistoryService
import ru.auto.comeback.model.Vin
import ru.auto.comeback.model.carfax.{
  CarfaxReportPurchase,
  Estimate,
  ExternalSale,
  Maintenance,
  SaleOfInsurance,
  VinReport
}
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.{IO, ZIO}
import zio.test._

object VinReportServiceSpec extends DefaultRunnableSpec {

  val essentialsReport = new RawVinEssentialsReport(
    vin = "Z8T4C5FS9BM005269",
    history = Some(
      HistoryBlock(
        owners = Seq(
          OwnerHistory(
            historyRecords = Seq(
              HistoryRecord(
                record = AutoServiceRecord(
                  AutoServiceHistoryRecord(
                    timestamp = 100020003000L,
                    meta = Option(
                      RecordMeta(
                        source = Option(
                          SourceMeta(
                            autoruClientIds = Seq(13, 42, 1337)
                          )
                        )
                      )
                    )
                  )
                )
              ),
              HistoryRecord(
                record = SellAutoEventRecord(
                  SellAutoEventItem(
                    date = 400050006000L,
                    meta = Option(
                      RecordMeta(
                        source = Option(
                          SourceMeta(
                            autoruClientIds = Seq(1000, 2000)
                          )
                        )
                      )
                    )
                  )
                )
              ),
              HistoryRecord(
                record = EstimateRecord(
                  EstimateItem(
                    date = 400070006000L,
                    meta = Option(
                      RecordMeta(
                        source = Option(
                          SourceMeta(
                            autoruClientIds = Seq(4000, 5000)
                          )
                        )
                      )
                    )
                  )
                )
              ),
              HistoryRecord(
                record = InsuranceRecord(
                  InsuranceItem(
                    date = 400070006000L,
                    meta = Option(
                      RecordMeta(
                        source = Option(
                          SourceMeta(
                            autoruClientIds = Seq(7000, 7001)
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  val carfaxReportPurchase1 = PublicOrderModel(
    id = "1",
    created = Some(Timestamp.of(1623995222L, 0)),
    status = Status.SUCCESS,
    identifierType = OrderIdentifierType.VIN,
    reportTypeOrId = ReportTypeOrId.ReportType(ReportType.FULL_REPORT),
    vin = "XW8ZZZ3CZ9G001083",
    userId = "dealer:123"
  )

  val carfaxReportPurchase2 = PublicOrderModel(
    id = "2",
    created = Some(Timestamp.of(1623985222L, 0)),
    status = Status.PREPARING,
    identifierType = OrderIdentifierType.VIN,
    reportTypeOrId = ReportTypeOrId.ReportType(ReportType.GIBDD_REPORT),
    vin = "NMTBB0BE30R025837",
    userId = "dealer:456"
  )

  val carfaxService = new Carfax.Service {

    override def getEssentialsRawReport(vin: String): IO[Carfax.CarfaxError, RawVinEssentialsReport] =
      ZIO.succeed(essentialsReport)

    override def getOrders(request: GetOrdersListRequest): IO[Carfax.CarfaxError, List[PublicOrderModel]] =
      ZIO.succeed(List(carfaxReportPurchase1, carfaxReportPurchase2))
  }

  def spec =
    suite("VinReportService")(
      testM(
        "return multiple autoruClientIds for one record meta"
      ) {
        val service = new VinHistoryService.Live(carfaxService)

        for {
          vinReport <- service.getReport(Vin(""))
        } yield assert(vinReport)(
          equalTo(
            VinReport(
              "Z8T4C5FS9BM005269",
              List(),
              List(),
              List(
                Maintenance(Instant.ofEpochMilli(100020003000L), Some(13)),
                Maintenance(Instant.ofEpochMilli(100020003000L), Some(42)),
                Maintenance(Instant.ofEpochMilli(100020003000L), Some(1337))
              ),
              List(
                ExternalSale(Instant.ofEpochMilli(400050006000L), Some(1000)),
                ExternalSale(Instant.ofEpochMilli(400050006000L), Some(2000))
              ),
              List(
                Estimate(Instant.ofEpochMilli(400070006000L), Some(4000)),
                Estimate(Instant.ofEpochMilli(400070006000L), Some(5000))
              ),
              List(
                SaleOfInsurance(Instant.ofEpochMilli(400070006000L), Some(7000)),
                SaleOfInsurance(Instant.ofEpochMilli(400070006000L), Some(7001))
              )
            )
          )
        )
      },
      testM("return carfax orders") {

        val service = new VinHistoryService.Live(carfaxService)

        for {
          orders <- service.getOrders(Vin(""), Nil, Nil)
        } yield assert(orders)(
          hasSameElements(
            List(
              CarfaxReportPurchase(Instant.ofEpochMilli(1623995222000L), "XW8ZZZ3CZ9G001083", Some(123L)),
              CarfaxReportPurchase(Instant.ofEpochMilli(1623985222000L), "NMTBB0BE30R025837", Some(456L))
            )
          )
        )
      }
    )
}
