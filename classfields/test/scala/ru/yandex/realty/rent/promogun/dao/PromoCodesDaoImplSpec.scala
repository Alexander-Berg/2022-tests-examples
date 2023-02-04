package ru.yandex.realty.promogun.dao

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.util.Timestamps
import doobie.implicits._
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.testcontainers.MySQLTestContainer
import ru.yandex.realty.doobie.{DoobieTestDatabase, StubDbMonitorFactory}
import ru.yandex.realty.promogun.proto.scalapb.model.PromoCode
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class PromoCodesDaoImplSpec extends AsyncSpecBase with MySQLTestContainer.V8_0 with DoobieTestDatabase {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  before {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/schema.sql")).futureValue
  }

  after {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/drop_tables.sql")).futureValue
  }

  private val dao = new PromoCodesDaoImpl(new StubDbMonitorFactory)
  implicit val trace: Traced = Traced.empty

  "PromoCodesDaoImpl" should {
    "insert promo code" in {
      val promoCode = PromoCode(
        id = "23557463532",
        domain = "Arenda",
        source = "taxi",
        code = "FFFFFFFFFFFFF",
        typeId = "taxi-300rub",
        createAt = Some(Timestamp.fromJavaProto(Timestamps.fromSeconds(1657292145))),
        expiresAt = Some(Timestamp.fromJavaProto(Timestamps.fromSeconds(1657392145)))
      )
      val inserted = doobieDatabase
        .masterTransaction {
          dao.insert(promoCode :: Nil)(_)
        }
        .futureValue
        .head
      val count = doobieDatabase.masterTransaction { _ =>
        sql"select count(*) from promo_codes where promo_code_id = ${inserted.id}".query[Long].unique
      }.futureValue
      count shouldBe 1
    }
    "update promo code" in {
      val promoCode = PromoCode(
        id = "123446",
        domain = "Arenda",
        source = "taxi",
        code = "FFFFFFFFFFFFF",
        typeId = "taxi-300rub",
        createAt = Some(Timestamp.fromJavaProto(Timestamps.fromSeconds(1657292145))),
        expiresAt = Some(Timestamp.fromJavaProto(Timestamps.fromSeconds(1657392145)))
      )

      val inserted = doobieDatabase.masterTransaction { dao.insert(promoCode :: Nil)(_) }.futureValue.head
      val updatedPromoCode = inserted.copy(targetUid = 300, grantedBy = "uid:123456")
      doobieDatabase.masterTransaction {
        dao.update(updatedPromoCode)(_)
      }.futureValue
      val count = doobieDatabase.masterTransaction { _ =>
        sql"select count(*) from promo_codes where promo_code_id = ${inserted.id} and target_uid = 300"
          .query[Long]
          .unique
      }.futureValue
      count shouldBe 1
    }
  }
}
