package ru.auto.salesman.dao.impl.jdbc.user

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.TransactionDao.Filter.ForTransactionId
import ru.auto.salesman.dao.user.TransactionDao.{
  Filter,
  Patch,
  Request,
  TransactionAlreadyExists,
  TransactionIdentity
}
import ru.auto.salesman.model.user.ProductContext.SubscriptionContext
import ru.auto.salesman.model.user.{ProductRequest, Prolongable}
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  TransactionId,
  TransactionStatuses
}
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

import scala.slick.jdbc.StaticQuery
import com.github.nscala_time.time.Imports._
import zio.ZIO

class JdbcTransactionDaoSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate
    with UserModelGenerators
    with IntegrationPropertyCheckConfig {
  import JdbcTransactionDaoSpec._

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val dao = new JdbcTransactionDao(database)

  "JdbcTransactionDao" should {

    "create transaction" in {
      dao.create(request(testTrnId)).success.value.right.value
    }

    "handle duplicate id" in {
      clean()
      dao.create(request(testTrnId)).success.value.right.value
      dao
        .create(request(testTrnId))
        .success
        .value
        .left
        .value shouldBe a[TransactionAlreadyExists]
    }

    "save transaction payload properly" in {
      clean()
      forAll(ProductRequestGen) { productRequest =>
        dao
          .create(request(id = testTrnId, payload = List(productRequest)))
          .success
          .value
          .right
        val created = dao.get(ForTransactionId(testTrnId)).success.value.head
        created.payload shouldBe List(productRequest)
      }
    }

    "save transaction payload for subscription properly" in {
      clean()
      val product = UserProductGen.next
      val price = productPriceGen().next
      val offer = OfferIdentityGen.next
      val context =
        SubscriptionContext(price, Some("vin"), garageId = Some("garage_id"))
      val productRequest =
        ProductRequest(
          product,
          Some(offer),
          amount = 100L,
          context,
          Prolongable(false)
        )
      dao
        .create(request(testTrnId, payload = List(productRequest)))
        .success
        .value
        .right
      val created = dao.get(ForTransactionId(testTrnId)).success.value.head
      created.payload shouldBe List(productRequest)
    }

  }

  "JdbcTransactionDao.createWithIdentity" should {
    "insert transaction and identity" in {
      clean()
      val identity = actualIdentity
      dao.createWithIdentity(request(testTrnId), identity).success

      val created = dao.get(Filter.ForTransactionId(testTrnId)).success.value
      created.size shouldBe 1
    }

    "return existed transactionId if transaction with same identity exists" in {
      clean()
      val identity = actualIdentity
      dao.createWithIdentity(request(testTrnId), identity).success

      val secondTrnId = testTrnId + "2"
      dao
        .createWithIdentity(request(secondTrnId), identity)
        .success
        .value shouldBe Left(TransactionAlreadyExists(testTrnId))
    }

    "insert transaction if transaction with same identity canceled" in {
      clean()
      val identity = actualIdentity
      dao.createWithIdentity(request(testTrnId), identity).success
      dao
        .update(testTrnId, Patch.Status(TransactionStatuses.Canceled))
        .success

      val secondTrnId = testTrnId + "2"
      dao
        .createWithIdentity(request(secondTrnId), identity)
        .success

      val created = dao.get(Filter.ForTransactionId(secondTrnId)).success.value
      created.size shouldBe 1
    }

    "insert transaction with other identity" in {
      clean()
      val identity = actualIdentity
      dao.createWithIdentity(request(testTrnId), identity).success

      val secondTrnId = testTrnId + "2"
      val secondIdentity = identity.copy(identity = identity + "2")
      dao
        .createWithIdentity(request(secondTrnId), secondIdentity)
        .success

      val created = dao.get(Filter.ForTransactionId(secondTrnId)).success.value
      created.size shouldBe 1
    }

    "insert only one transaction for multiply requests and returns it id for others" in {
      clean()

      val identity = actualIdentity
      val requests = (1 to 10).map(i => request(s"tr:$i"))
      val results = ZIO
        .foreachPar(requests)(dao.createWithIdentity(_, identity))
        .success
        .value

      val (created, existed) = results.partition(_.isRight)
      created.size shouldBe 1
      existed.size shouldBe 9

      val oneOfExisted = existed.head
      val expectedExisted = List.fill(9)(oneOfExisted)

      existed should contain theSameElementsAs expectedExisted
    }

    "create new transaction if identity expired" in {
      clean()
      val identity = oldIdentity
      dao.createWithIdentity(request(testTrnId), identity).success

      val secondTrnId = testTrnId + "2"
      dao
        .createWithIdentity(request(secondTrnId), identity)
        .success
        .value shouldBe Right(unit)
    }

    "create new transaction if previous was canceled" in {
      clean()
      val identity = actualIdentity
      dao.createWithIdentity(request(testTrnId), identity).success
      dao
        .update(testTrnId, Patch.Status(TransactionStatuses.Canceled))
        .success
        .value

      val secondTrnId = testTrnId + "2"
      dao
        .createWithIdentity(request(secondTrnId), identity)
        .success
        .value shouldBe Right(unit)

      val created = dao.get(Filter.ForTransactionId(secondTrnId)).success.value
      created.size shouldBe 1
    }
  }

  private def request(id: TransactionId, payload: List[ProductRequest] = Nil) =
    Request(id, "user:2", 300, TransactionStatuses.New, payload, fields = Nil)

  private def clean(): Unit = database.withSession { implicit session =>
    StaticQuery.queryNA[Int](s"delete from `transactions_identity`").execute
    StaticQuery.queryNA[Int](s"delete from `transactions_lock`").execute
    StaticQuery.queryNA[Int](s"delete from `transactions`").execute
  }
}

object JdbcTransactionDaoSpec {
  private val testTrnId = "1"

  private val testIdentityDuration = 10.minutes

  private def actualIdentity =
    TransactionIdentity("tr:1", DateTime.now() - testIdentityDuration)

  private def oldIdentity =
    TransactionIdentity("tr:1", DateTime.now() + testIdentityDuration)
}
