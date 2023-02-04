package ru.auto.salesman.dao

import org.joda.time.DateTime
import ru.auto.salesman.dao.ProductDao.{CreateProductRequest, NoActiveProductException}
import ru.auto.salesman.dao.impl.jdbc.JdbcProductDao
import ru.auto.salesman.model.{
  ActiveProductNaturalKey,
  AutoruDealer,
  Product,
  ProductTariff,
  ProductType,
  UniqueProductType
}
import ru.auto.salesman.model.Product.ProductPaymentStatus
import ru.auto.salesman.test.BaseSpec
import zio.ZIO
import ru.auto.salesman.dao.slick.invariant.StaticQuery.interpolation
import ru.auto.salesman.model.Domain.ApplicationCredit
import ru.auto.salesman.model.Product.ProductPaymentStatus.{Active, Inactive}
import ru.auto.salesman.model.ProductTariff.ApplicationCreditSingleTariffCarsNew
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

import scala.slick.jdbc.StaticQuery

class ProductDaoSpec extends BaseSpec with SalesmanJdbcSpecTemplate {

  def dao = new JdbcProductDao(database, database)

  private def getProduct(id: Long) = {
    import zio.blocking._
    import JdbcProductDao.ProductResult

    effectBlocking {
      database.withTransaction { implicit session =>
        sql"""
          SELECT
            `id` , `domain`, `payer`, `target`, `product_type`, `status`, `create_date`, `expire_date`, `context`, `inactive_reason`, `prolongable`, `pushed`
          FROM `product`
          WHERE `id` = $id
         """.as[Product].list
      }
    }
  }

  "ProductDao" should {
    "create product record and return it as need_payment" in {
      clean()
      val request = CreateProductRequest(
        ActiveProductNaturalKey(
          payer = "dealer:20101",
          target = "cars:new",
          UniqueProductType.ApplicationCreditAccess
        ),
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      dao.create(request).success.value
      val created = dao.getWaitingForPayment.success.value.head

      created.key shouldBe request.key
      created.createDate should ~=(request.createDate)
      created.status shouldBe ProductPaymentStatus.NeedPayment
      created.prolongable shouldBe request.prolongable
      created.inactiveReason shouldBe None
      created.expireDate shouldBe None
      created.tariff shouldBe None
    }

    "not create product record if same exists in active/need_payment status" in {
      clean()
      val request = CreateProductRequest(
        ActiveProductNaturalKey(
          payer = "dealer:20101",
          target = "cars:new",
          UniqueProductType.ApplicationCreditAccess
        ),
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      dao.create(request).success.value
      dao
        .create(request)
        .success
        .value
        .left
        .value shouldBe a[ProductDao.ProductAlreadyExists]
    }

    "update prolongable" in {
      clean()
      val key = ActiveProductNaturalKey(
        payer = "dealer:20101",
        target = "cars:new",
        UniqueProductType.ApplicationCreditAccess
      )

      val request = CreateProductRequest(
        key,
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      dao.create(request).success.value
      database.withSession(implicit session =>
        StaticQuery
          .queryNA[Int]("""update product set status = 'ACTIVE'""")
          .execute
      )
      dao.updateProlongable(key, prolongable = false).success.value

      dao
        .getPayerActiveProductsByDomain(ApplicationCredit, AutoruDealer(20101))
        .success
        .value
        .head
        .prolongable shouldBe false
    }

    "throw exception on prolongable update if no active record found" in {
      clean()
      val key = ActiveProductNaturalKey(
        payer = "dealer:20101",
        target = "cars:new",
        UniqueProductType.ApplicationCreditAccess
      )

      val request = CreateProductRequest(
        key,
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      dao.create(request).success.value

      dao
        .updateProlongable(key, prolongable = false)
        .failure
        .exception shouldBe an[NoActiveProductException]
    }

    "create only 1 record" in {
      clean()

      val request = CreateProductRequest(
        ActiveProductNaturalKey(
          payer = "dealer:20101",
          target = "cars:new",
          UniqueProductType.ApplicationCreditAccess
        ),
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      val res = ZIO.foreachPar_(1 to 100)(_ => dao.create(request).run)

      (res *> dao.getWaitingForPayment).success.value should have size 1
    }

    "update successfully billed product status" in {
      clean()

      val payer = AutoruDealer(20101L)
      val createRequest = CreateProductRequest(
        ActiveProductNaturalKey(
          payer = payer.toString,
          target = "cars:new",
          UniqueProductType.ApplicationCreditAccess
        ),
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      val checks = for {
        _ <- dao.create(createRequest)
        created <- dao.getWaitingForPayment.map(_.head)
        id = created.id
        expireDate = created.createDate
          .plusDays(1)
        _ <- dao.activate(id, expireDate)
        updated <- getProduct(id).map(_.head)
      } yield {
        updated.expireDate shouldBe Some(expireDate)
        updated.status shouldBe ProductPaymentStatus.Active
      }

      checks.success
    }

    "update unsuccessfully billed product status" in {
      clean()

      val payer = AutoruDealer(20101L)
      val createRequest = CreateProductRequest(
        ActiveProductNaturalKey(
          payer = payer.toString,
          target = "cars:new",
          UniqueProductType.ApplicationCreditAccess
        ),
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      val checks = for {
        _ <- dao.create(createRequest)
        created <- dao.getWaitingForPayment.map(_.head)
        id = created.id
        _ <- dao.markFailed(id)
        updated <- getProduct(id).map(_.head)
      } yield {
        updated.expireDate shouldBe None
        updated.status shouldBe ProductPaymentStatus.Failed
      }

      checks.success
    }

    "not update already updated billing status" in {
      val payer = AutoruDealer(20101L)
      val key1 = ActiveProductNaturalKey(
        payer = payer.toString,
        target = "cars:new",
        UniqueProductType.ApplicationCreditAccess
      )

      val createRequest1 = CreateProductRequest(
        key1,
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      val createRequest2 =
        CreateProductRequest(
          key1.copy(target = "cars:used"),
          createDate = DateTime.now(),
          prolongable = true,
          productTariff = None
        ).right.value

      val checks = for {
        _ <- dao.create(createRequest1)
        _ <- dao.create(createRequest2)
        created <- dao.getWaitingForPayment
        List(p1, p2) = created
        expire = p1.createDate.plusDays(1)
        _ <- dao.activate(p1.id, expire)
        _ <- dao.markFailed(p2.id)
        _ <- dao.activate(p2.id, expire)
        _ <- dao.markFailed(p1.id)
        updatedList1 <- getProduct(p1.id).map(_.head)
        updatedList2 <- getProduct(p2.id).map(_.head)
        updated1 = updatedList1
        updated2 = updatedList2
      } yield {
        updated1.expireDate shouldBe Some(expire)
        updated2.expireDate shouldBe None
        updated1.status shouldBe ProductPaymentStatus.Active
        updated2.status shouldBe ProductPaymentStatus.Failed
      }

      checks.success
    }

    "prolong expired prolongable products and leave them active" in {
      clean()

      val payer = AutoruDealer(20101L)
      val createProlongableRequest = CreateProductRequest(
        ActiveProductNaturalKey(
          payer = payer.toString,
          target = "cars:new",
          UniqueProductType.ApplicationCreditAccess
        ),
        createDate = DateTime.now().minusDays(2),
        prolongable = true,
        productTariff = None
      ).right.value

      val checks = for {
        _ <- dao.create(createProlongableRequest)
        created <- dao.getWaitingForPayment.map(_.head)
        expireDate0 = created.createDate.plusDays(1)
        _ <- dao.activate(created.id, expireDate0)
        activeExpired <- dao.getActiveExpiredProducts.map(_.head)
        now = DateTime.now()
        expireDate1 = expireDate0.plusDays(1)
        _ <- dao.prolong(activeExpired, now, expireDate1)
        inactive <- getProduct(activeExpired.id).map(_.head)
        active <- dao
          .getPayerActiveProductsByDomain(ApplicationCredit, payer)
          .map(_.head)
      } yield {
        activeExpired.key shouldBe created.key
        activeExpired.expireDate shouldBe Some(expireDate0)
        inactive.status shouldBe Inactive
        active.key shouldBe activeExpired.key
        active.createDate should ~=(now)
        active.status shouldBe Active
        active.expireDate shouldBe Some(expireDate1)
      }

      checks.success
    }

    "haveActiveProduct return true if there is active product" in {
      clean()
      val payer = "dealer:20101"

      val key = ActiveProductNaturalKey(
        payer = payer,
        target = "cars:new",
        UniqueProductType.ApplicationCreditAccess
      )

      val request = CreateProductRequest(
        key,
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      dao.create(request).success.value
      database.withSession(implicit session =>
        StaticQuery
          .queryNA[Int]("""update product set status = 'ACTIVE'""")
          .execute
      )

      dao
        .haveActiveProduct(payer, UniqueProductType.ApplicationCreditAccess)
        .success
        .value shouldBe true
    }

    "haveActiveProduct return false if there is no active product" in {
      clean()
      val payer = "dealer:20101"
      val key = ActiveProductNaturalKey(
        payer = payer,
        target = "cars:new",
        UniqueProductType.ApplicationCreditAccess
      )

      val request = CreateProductRequest(
        key,
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = None
      ).right.value

      dao.create(request).success.value

      dao
        .haveActiveProduct(payer, UniqueProductType.ApplicationCreditAccess)
        .success
        .value shouldBe false
    }

    "create product record with productTariff and return it with same value" in {
      clean()
      val tariff = Some(ProductTariff.ApplicationCreditSingleTariffCarsNew)
      val request = CreateProductRequest(
        ActiveProductNaturalKey(
          payer = "dealer:20101",
          target = "cars:new",
          UniqueProductType.ApplicationCreditSingle
        ),
        createDate = DateTime.now(),
        prolongable = true,
        productTariff = tariff
      ).right.value

      dao.create(request).success.value
      val created = dao.getWaitingForPayment.success.value.head

      created.key shouldBe request.key
      created.createDate should ~=(request.createDate)
      created.status shouldBe ProductPaymentStatus.NeedPayment
      created.prolongable shouldBe request.prolongable
      created.inactiveReason shouldBe None
      created.expireDate shouldBe None
      created.tariff shouldBe tariff
    }

    "getAllActiveProductsByDomainAndProductType return products by domain and type" in {
      clean()

      val applicationCreditAccessIds = 1 to 10
      val applicationCreditAccessRequests: Seq[CreateProductRequest] =
        applicationCreditAccessIds.map { i =>
          val key = ActiveProductNaturalKey(
            payer = s"dealer:$i",
            target = "cars:new",
            UniqueProductType.ApplicationCreditAccess
          )

          CreateProductRequest(
            key = key,
            createDate = DateTime.now(),
            prolongable = true,
            productTariff = None
          ).right.value
        }
      val otherRequests: Seq[CreateProductRequest] = (11 to 15).map { i =>
        val key = ActiveProductNaturalKey(
          payer = s"dealer:$i",
          target = "cars:new",
          UniqueProductType.ApplicationCreditSingle
        )

        CreateProductRequest(
          key = key,
          createDate = DateTime.now(),
          prolongable = true,
          productTariff = Some(ApplicationCreditSingleTariffCarsNew)
        ).right.value
      } ++ (16 to 20).map { i =>
        val key = ActiveProductNaturalKey(
          payer = s"dealer:$i",
          target = "cars:new",
          UniqueProductType.GibddHistoryReport
        )
        CreateProductRequest(
          key = key,
          createDate = DateTime.now(),
          prolongable = false,
          productTariff = None
        ).right.value
      } ++ (21 to 25).map { i =>
        val key = ActiveProductNaturalKey(
          payer = s"dealer:$i",
          target = "cars:new",
          UniqueProductType.FullHistoryReport
        )
        CreateProductRequest(
          key = key,
          createDate = DateTime.now(),
          prolongable = false,
          productTariff = None
        ).right.value
      }

      val resultZ =
        for {
          _ <- ZIO.foreach_(applicationCreditAccessRequests ++ otherRequests)(
            dao.create
          )
          _ = database.withSession { implicit session =>
            StaticQuery
              .queryNA[Int]("""update product set status = 'ACTIVE'""")
              .execute
          }
          products <- dao.getAllActiveProductsByDomainAndProductType(
            ApplicationCredit,
            ProductType.Access
          )
        } yield products

      val result = resultZ.success.value

      result
        .map(product => AutoruDealer(product.key.payer))
        .forall(dealer => applicationCreditAccessIds.toSet.contains(dealer.id))
    }
  }

  private def clean(): Unit = database.withTransaction { implicit session =>
    StaticQuery.queryNA[Int](s"delete from `product`").execute
    StaticQuery.queryNA[Int](s"delete from `product_lock`").execute
  }
}
