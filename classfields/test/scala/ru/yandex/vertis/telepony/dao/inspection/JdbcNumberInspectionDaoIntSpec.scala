package ru.yandex.vertis.telepony.dao.inspection

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate
import ru.yandex.vertis.telepony.model.Phone
import ru.yandex.vertis.telepony.model.inspection.NumberInspectionStatusValues._

import java.time.Instant

class JdbcNumberInspectionDaoIntSpec extends SpecBase with DatabaseSpec with JdbcSpecTemplate with BeforeAndAfterEach {

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val dao = new JdbcNumberInspectionDao

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dao.clear().databaseValue.futureValue
  }

  private val testPhone1: Phone = Phone("79111233211")
  private val testPhone2: Phone = Phone("79111233222")
  private val testPhone3: Phone = Phone("79111233233")
  private val testPhone4: Phone = Phone("79111233244")
  private val testPhone5: Phone = Phone("79111233255")

  private val All: Set[NumberInspectionStatus] = Set(New, Processing, Failed, Success)

  "JdbcNumberInspectionDao" should {
    "add new numbers" in {
      dao.add(testPhone1).databaseValue.futureValue
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe New
      res.number shouldBe testPhone1
      res.attemptNumber shouldBe 1
      res.state shouldBe None
    }

    "failed to add if exists" in {
      dao.add(testPhone1).databaseValue.futureValue
      val ex = dao.add(testPhone1).databaseValue.failed.futureValue
      ex shouldBe an[java.sql.SQLIntegrityConstraintViolationException]
    }

    "return inspection with getUnsafe" in {
      dao.add(testPhone1).databaseValue.futureValue
      val res1 = dao.get(testPhone1).databaseValue.futureValue.get
      val res2 = dao.getUnsafe(testPhone1).databaseValue.futureValue
      res1 shouldBe res2
    }

    "failed to getUnsafe if empty" in {
      val res1 = dao.get(testPhone1).databaseValue.futureValue
      res1 shouldBe empty
      val res2 = dao.getUnsafe(testPhone1).databaseValue.failed.futureValue
      res2 shouldBe an[NoSuchElementException]
    }

    "increment attempt count" in {
      dao.add(testPhone1).databaseValue.futureValue
      val updated = dao.incrementAttemptNumber(testPhone1, New, 1).databaseValue.futureValue
      updated shouldBe true
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe New
      res.attemptNumber shouldBe 2
    }

    "not increment attempt count if compare failed on count" in {
      dao.add(testPhone1).databaseValue.futureValue
      val updated = dao.incrementAttemptNumber(testPhone1, New, 2).databaseValue.futureValue
      updated shouldBe false
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe New
      res.attemptNumber shouldBe 1
    }

    "not increment attempt count if compare failed on status" in {
      dao.add(testPhone1).databaseValue.futureValue
      val updated = dao.incrementAttemptNumber(testPhone1, Processing, 1).databaseValue.futureValue
      updated shouldBe false
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe New
      res.attemptNumber shouldBe 1
    }

    "not increment attempt count for nonexistent" in {
      val updated = dao.incrementAttemptNumber(testPhone1, Processing, 1).databaseValue.futureValue
      updated shouldBe false
      dao.get(testPhone1).databaseValue.futureValue shouldBe empty
    }

    "update status" in {
      dao.add(testPhone1).databaseValue.futureValue
      val updated = dao.updateStatus(testPhone1, New, Processing).databaseValue.futureValue
      updated shouldBe true
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe Processing
    }

    "update status if compare failed on status" in {
      dao.add(testPhone1).databaseValue.futureValue
      val updated = dao.updateStatus(testPhone1, Failed, Processing).databaseValue.futureValue
      updated shouldBe false
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe New
    }

    "not update status for nonexistent" in {
      val updated = dao.updateStatus(testPhone1, Failed, Processing).databaseValue.futureValue
      updated shouldBe false
      dao.get(testPhone1).databaseValue.futureValue shouldBe empty
    }

    "retry inspection" in {
      dao.add(testPhone1).databaseValue.futureValue
      dao.updateStatus(testPhone1, New, Processing).databaseValue.futureValue shouldBe true
      dao.incrementAttemptNumber(testPhone1, Processing, 1).databaseValue.futureValue shouldBe true
      dao.retry(testPhone1, Processing, 2).databaseValue.futureValue shouldBe true

      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe Retry
      res.attemptNumber shouldBe 3
    }

    "not retry inspection if compare failed on status" in {
      dao.add(testPhone1).databaseValue.futureValue
      val updated = dao.retry(testPhone1, Processing, 1).databaseValue.futureValue
      updated shouldBe false
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe New
      res.attemptNumber shouldBe 1
    }

    "not retry inspection if compare failed on attempt count" in {
      dao.add(testPhone1).databaseValue.futureValue
      val updated = dao.retry(testPhone1, New, 2).databaseValue.futureValue
      updated shouldBe false
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe New
      res.attemptNumber shouldBe 1
    }

    "not retry inspection for nonexistent" in {
      val updated = dao.retry(testPhone1, Processing, 1).databaseValue.futureValue
      updated shouldBe false
      dao.get(testPhone1).databaseValue.futureValue shouldBe empty
    }

    // todo TELEPONY-2395 later update state tests

    "list all" in {
      dao.add(testPhone1).databaseValue.futureValue
      dao.add(testPhone2).databaseValue.futureValue
      dao.add(testPhone3).databaseValue.futureValue

      dao.updateStatus(testPhone2, New, Success).databaseValue.futureValue shouldBe true
      dao.updateStatus(testPhone3, New, Processing).databaseValue.futureValue shouldBe true

      val list = dao.list(All, 100).databaseValue.futureValue
      val res1 = dao.get(testPhone1).databaseValue.futureValue.get
      val res2 = dao.get(testPhone2).databaseValue.futureValue.get
      val res3 = dao.get(testPhone3).databaseValue.futureValue.get

      list should have size 3
      (list should contain).theSameElementsInOrderAs(Seq(res1, res2, res3))
    }

    "list by status with limit" in {
      dao.add(testPhone1).databaseValue.futureValue
      dao.add(testPhone2).databaseValue.futureValue
      dao.add(testPhone3).databaseValue.futureValue
      dao.add(testPhone4).databaseValue.futureValue
      dao.add(testPhone5).databaseValue.futureValue

      dao.updateStatus(testPhone4, New, Processing).databaseValue.futureValue shouldBe true
      dao.updateStatus(testPhone5, New, Failed).databaseValue.futureValue shouldBe true
      dao.updateStatus(testPhone2, New, Success).databaseValue.futureValue shouldBe true
      dao.updateStatus(testPhone3, New, Processing).databaseValue.futureValue shouldBe true

      val list = dao.list(Set(Success, Processing), 2).databaseValue.futureValue
      val res4 = dao.get(testPhone4).databaseValue.futureValue.get
      val res2 = dao.get(testPhone2).databaseValue.futureValue.get

      list should have size 2
      (list should contain).theSameElementsInOrderAs(Seq(res4, res2))
    }

    "renew inspection" in {
      dao.add(testPhone1).databaseValue.futureValue
      val first = dao.getUnsafe(testPhone1).databaseValue.futureValue
      val updated = dao.updateStatus(testPhone1, New, Processing).databaseValue.futureValue
      updated shouldBe true
      val res = dao.get(testPhone1).databaseValue.futureValue.get
      res.status shouldBe Processing
      dao.renew(testPhone1).databaseValue.futureValue
      val second = dao.getUnsafe(testPhone1).databaseValue.futureValue
      second.status shouldBe New
      second.attemptNumber shouldBe 1
      second.state shouldBe empty
      second.createTime should not be first.createTime
      second.updateTime should not be first.updateTime
    }

    "list outdated" in {
      dao.add(testPhone1).databaseValue.futureValue
      dao.add(testPhone2).databaseValue.futureValue
      dao.add(testPhone3).databaseValue.futureValue
      dao.add(testPhone4).databaseValue.futureValue
      dao.add(testPhone5).databaseValue.futureValue

      dao.updateStatus(testPhone1, New, Processing).databaseValue.futureValue shouldBe true
      dao.updateStatus(testPhone3, New, Processing).databaseValue.futureValue shouldBe true
      dao.updateStatus(testPhone2, New, Failed).databaseValue.futureValue shouldBe true
      val bound = Instant.now()
      dao.updateStatus(testPhone4, New, Processing).databaseValue.futureValue shouldBe true

      val list = dao.listOutdated(bound, 10).databaseValue.futureValue

      val res1 = dao.get(testPhone1).databaseValue.futureValue.get
      val res3 = dao.get(testPhone3).databaseValue.futureValue.get

      list should have size 2
      (list should contain).theSameElementsInOrderAs(Seq(res1, res3))
    }

    "list for processing" in {
      dao.add(testPhone1).databaseValue.futureValue
      dao.add(testPhone2).databaseValue.futureValue
      dao.add(testPhone3).databaseValue.futureValue
      dao.add(testPhone4).databaseValue.futureValue
      dao.add(testPhone5).databaseValue.futureValue

      dao.retry(testPhone3, New, 1).databaseValue.futureValue shouldBe true
      val bound = Instant.now()
      dao.retry(testPhone1, New, 1).databaseValue.futureValue shouldBe true

      dao.updateStatus(testPhone2, New, Processing).databaseValue.futureValue shouldBe true
      dao.updateStatus(testPhone4, New, Failed).databaseValue.futureValue shouldBe true

      val list = dao.listForProcessing(bound, 10).databaseValue.futureValue

      val res5 = dao.get(testPhone5).databaseValue.futureValue.get
      val res3 = dao.get(testPhone3).databaseValue.futureValue.get

      list should have size 2
      (list should contain).theSameElementsInOrderAs(Seq(res5, res3))
    }

  }
}
