package billing.finstat.storage.test

import billing.finstat.model.domain.{raw, _}
import billing.finstat.model.domain.raw.{RawEvent, RequiredFields}
import billing.finstat.storage.schema_service.Schema
import common.zio.clients.clickhouse.http.codec.Decoder
import common.zio.clients.clickhouse.http.ClickHouseClient
import org.apache.commons.io.IOUtils
import zio.{Task, ZIO, ZManaged}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

trait ClickhouseTestStatics {

  val AutoDealersWorkspaceSchema =
    Schema("product", "client_id", "offer_id", "category", "section", "agency_id", "company_group")

  implicit class RichRawEvent(x: RawEvent) {

    def toSqlValue: String =
      List(
        x.requiredFields.eventTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        x.requiredFields.transactionId,
        x.requiredFields.version,
        x.eventFields("spent_kopecks"),
        x.eventFields("product"),
        x.eventFields("client_id"),
        x.eventFields("offer_id"),
        x.eventFields("category"),
        x.eventFields("section"),
        x.eventFields("agency_id"),
        x.eventFields("company_group")
      ).map(x => s"'${x.toString}'").mkString("(", ",", ")")

  }

  def query[A: Decoder](x: String) =
    ClickHouseClient.execute(x)

  def query_(x: String) = ClickHouseClient.execute_(x)

  def loadSqlFromFile(path: String): ZIO[Any, Throwable, String] = ZManaged
    .fromAutoCloseable(Task(getClass.getResourceAsStream(path).ensuring(_ ne null, s"Resource $path not found")))
    .use(is => Task(IOUtils.toString(is, "utf-8")))

  def incrementVersion(x: RawEvent) =
    x.copy(requiredFields = x.requiredFields.copy(version = x.requiredFields.version + 1))

  def changeDate(x: RawEvent, dateTime: LocalDateTime) =
    x.copy(requiredFields = x.requiredFields.copy(eventTime = dateTime))

  def changeTransactionId(x: RawEvent, transactionId: String) =
    x.copy(requiredFields = x.requiredFields.copy(transactionId = transactionId))

  val table = "finstat.autoru_dealers_events"

  val countRows = s"SELECT COUNT(*) FROM $table"

  val clearTable = s"ALTER TABLE $table DELETE WHERE 1=1"

  val dropTable = s"DROP TABLE IF EXISTS $table ON CLUSTER '{cluster}' no delay"

  val event1 = RawEvent(
    RequiredFields(
      transactionId = "trxId1",
      version = 1L,
      eventTime = LocalDateTime.parse("2021-11-01T00:00:00")
    ),
    Map(
      "spent_kopecks" -> LongValue(10L),
      "product" -> StringValue("boost"),
      "client_id" -> StringValue("clientId1"),
      "offer_id" -> StringValue("offerId1"),
      "category" -> StringValue("CARS"),
      "section" -> StringValue("NEW"),
      "agency_id" -> StringValue("11111"),
      "company_group" -> StringValue("123456")
    )
  )

  val event2 = RawEvent(
    raw.RequiredFields(
      transactionId = "trxId2",
      version = 1L,
      eventTime = LocalDateTime.parse("2021-11-02T00:00:00")
    ),
    Map(
      "spent_kopecks" -> LongValue(100L),
      "product" -> StringValue("boost"),
      "client_id" -> StringValue("clientId2"),
      "offer_id" -> StringValue("offerId2"),
      "category" -> StringValue("CARS"),
      "section" -> StringValue("NEW"),
      "agency_id" -> StringValue("22222"),
      "company_group" -> StringValue("987654")
    )
  )
}
