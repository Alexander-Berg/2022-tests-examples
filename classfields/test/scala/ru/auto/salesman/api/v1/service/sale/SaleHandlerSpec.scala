package ru.auto.salesman.api.v1.service.sale

import java.sql.Timestamp
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Conflict, OK}
import org.joda.time.LocalDate
import ru.auto.salesman.api.v1.JdbcProductServices
import ru.auto.salesman.dao.slick.invariant.GetResult
import ru.auto.salesman.dao.slick.invariant.StaticQuery.interpolation
import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.user.SaleModel.{Sale, SalePush}
import spray.json._
import spray.json.DefaultJsonProtocol._

class SaleHandlerSpec extends JdbcProductServices {

  private val dateInFuture = LocalDate.now().plusWeeks(1)
  private val dateInPast = LocalDate.now().minusDays(1)

  private val baseFutureSale =
    Sale
      .newBuilder()
      .addProducts("offers-history-reports-10")
      .setDate(dateInFuture.toString)
      .setPercent(30)
      .build()

  implicit private class RichSale(private val sale: Sale) {

    def copy(f: Sale.Builder => Unit): Sale = {
      val b = sale.toBuilder
      f(b)
      b.build()
    }
  }

  "POST /sale" should {

    "respond with 200 on valid request to create future sale" in {
      val sale = baseFutureSale
      val response = create(sale)
      response.status shouldBe OK
    }

    "save sale start properly" in {
      val sale = baseFutureSale.copy(_.setDate("2035-07-18"))
      create(sale)
      getSaleColumn[Timestamp]("start") shouldBe Timestamp.valueOf(
        "2035-07-18 00:00:00"
      )
    }

    "save sale deadline properly" in {
      val sale = baseFutureSale.copy(_.setDate("2035-07-18"))
      create(sale)
      getSaleColumn[Timestamp]("deadline") shouldBe Timestamp.valueOf(
        "2035-07-19 00:00:00"
      )
    }

    "save sale discount properly" in {
      val sale = baseFutureSale.copy(_.setPercent(50))
      create(sale)
      getSaleColumn[Int]("discount") shouldBe 50
    }

    "save sale context properly" in {
      val sale =
        baseFutureSale.copy(_.clearProducts().addProducts("turbo-package"))
      create(sale)
      getSaleColumn[String](
        "context"
      ).parseProducts.loneElement shouldBe "turbo-package"
    }

    "save sale context properly for offers-history-reports packages" in {
      val sale =
        baseFutureSale.copy(
          _.clearProducts()
            .addProducts("offers-history-reports-1")
            .addProducts("offers-history-reports-5")
            .addProducts("offers-history-reports-10")
        )
      create(sale)
      getSaleColumn[String](
        "context"
      ).parseProducts should contain theSameElementsAs List(
        "offers-history-reports-1",
        "offers-history-reports-5",
        "offers-history-reports-10"
      )
    }

    "respond with 409 on request to create sale if the sale already exists at this day" in {
      val sale = baseFutureSale
      val anotherSaleAtSameDay =
        baseFutureSale.copy(_.clearProducts().addProducts("turbo-package"))
      create(sale)
      create(anotherSaleAtSameDay).status shouldBe Conflict
    }

    "respond with 400 on request to create sale for unknown product" in {
      val sale = baseFutureSale.copy(_.addProducts("unknown-product"))
      val response = create(sale)
      response.status shouldBe BadRequest
    }

    "respond with 400 on request to create sale in past" in {
      val sale = baseFutureSale.copy(_.setDate(dateInPast.toString))
      val response = create(sale)
      response.status shouldBe BadRequest
    }

    "respond with 400 on request to create sale with percent < 0" in {
      val sale = baseFutureSale.copy(_.setPercent(-10))
      val response = create(sale)
      response.status shouldBe BadRequest
    }

    "respond with 400 on request to create sale with percent = 0" in {
      val sale = baseFutureSale.copy(_.setPercent(0))
      val response = create(sale)
      response.status shouldBe BadRequest
    }

    "respond with 400 on request to create sale with percent > 100" in {
      val sale = baseFutureSale.copy(_.setPercent(150))
      val response = create(sale)
      response.status shouldBe BadRequest
    }

    val push =
      SalePush.newBuilder.setPushTitle("test_title").setPushBody("test_body")

    "set push" in {
      val sale = baseFutureSale
        .copy(_.setSalePush(push))
        .copy(_.clearProducts())
      create(sale)
      getPushColumn[String]("push_title") shouldBe "test_title"
      getPushColumn[String]("push_body") shouldBe "test_body"
      getPushColumn[Int]("finished") shouldBe 0
      getSaleColumn[Int]("id") shouldBe getPushColumn[Int]("source_id")
    }

    "set right push start and deadline" in {
      val sale = baseFutureSale
        .copy(_.setSalePush(push))
        .copy(_.clearProducts().addProducts("turbo-package"))
        .copy(_.setDate("2035-07-18"))
      create(sale)
      getPushColumn[Timestamp]("start") shouldBe Timestamp.valueOf(
        "2035-07-18 13:00:00"
      )
      getPushColumn[Timestamp]("deadline") shouldBe Timestamp.valueOf(
        "2035-07-18 16:00:00"
      )
    }

    "set long push name" in {
      val sale = baseFutureSale
        .copy(_.setSalePush(push))
        .copy(_.clearProducts().addProducts("turbo-package"))
        .copy(_.setDate("2035-07-18"))
      create(sale)
      getPushColumn[String]("push_name") shouldBe "VAS_180735_turbo-package"
    }

    "set short push name" in {
      val sale =
        baseFutureSale
          .copy(_.setSalePush(push))
          .copy(
            _.clearProducts()
              .addProducts("offers-history-reports-1")
              .addProducts("offers-history-reports-5")
              .addProducts("offers-history-reports-10")
          )
          .copy(_.setDate("2035-07-18"))
      create(sale)
      getPushColumn[String]("push_name") shouldBe "VAS_180735_off1_off10_off5"
    }

    "set push name for too many products" in {
      val sale =
        baseFutureSale
          .copy(_.setSalePush(push))
          .copy(
            _.clearProducts()
              .addProducts("offers-history-reports-1")
              .addProducts("offers-history-reports-5")
              .addProducts("offers-history-reports-10")
              .addProducts("placement")
              .addProducts("boost")
              .addProducts("highlighting")
          )
          .copy(_.setDate("2035-07-18"))
      create(sale)
      getPushColumn[String]("push_name") shouldBe "VAS_180735_many_products"
    }

    "save source_id in push properly" in {
      val sale = baseFutureSale
        .copy(_.setSalePush(push))
      create(sale)

      val sale2 = baseFutureSale
        .copy(_.setDate("2035-01-01"))
        .copy(_.setSalePush(push))

      create(sale)
      create(sale2)

      val saleId = getSaleColumn[Int]("id", fromFirstRow = false)
      val pushSourceId = getPushColumn[Int]("source_id", fromFirstRow = false)
      pushSourceId shouldBe saleId
    }
  }

  private def create(sale: Sale) =
    post("/api/1.x/service/autoru/sale", sale.toByteArray)

  private def getColumn[A: GetResult](
      table: String,
      column: String,
      fromFirstRow: Boolean
  ) =
    database.withTransaction { implicit session =>
      val orderDir = if (fromFirstRow) "ASC" else "DESC"
      sql"SELECT #$column FROM #$table order by id #$orderDir".as[A].first
    }

  private val discountTableName = "periodical_discount"
  private val pushTableName = "broadcast_pushing_schedule"

  private def getSaleColumn[A: GetResult](
      column: String,
      fromFirstRow: Boolean = true
  ) =
    getColumn(discountTableName, column, fromFirstRow)

  private def getPushColumn[A: GetResult](
      column: String,
      fromFirstRow: Boolean = true
  ) =
    getColumn(pushTableName, column, fromFirstRow)

  implicit private class RichStringContext(private val context: String) {

    def parseProducts: List[String] =
      context.parseJson.asJsObject.fields
        .get("products")
        .value
        .convertTo[List[String]]
  }

  implicit override def domain: DeprecatedDomain = AutoRu
}
