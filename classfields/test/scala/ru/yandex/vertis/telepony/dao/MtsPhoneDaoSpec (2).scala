package ru.yandex.vertis.telepony.dao

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.mts.MtsPhone
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.time._
import ru.yandex.vertis.telepony.generator.Producer._

/**
  * @author evans
  */
trait MtsPhoneDaoSpec extends SpecBase with BeforeAndAfterEach {

  def dao: MtsPhoneDao

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dao.clear().futureValue
  }

  "MtsPhoneDao" should {
    "upsert new" in {
      val phone = MtsPhone(PhoneGen.next, 1, DateTime.now(TimeZone))
      dao.upsert(phone).futureValue
      dao.list().futureValue.toList shouldEqual List(phone)
    }
    "upsert over old" in {
      val phone = MtsPhone(PhoneGen.next, 1, DateTime.now(TimeZone))
      dao.upsert(phone).futureValue
      val phone2 = phone.copy(startId = 2)
      dao.upsert(phone2).futureValue
      dao.list().futureValue.toList shouldEqual List(phone2)
    }
    "delete mts phone" in {
      val phone = MtsPhone(PhoneGen.next, 1, DateTime.now(TimeZone))
      dao.upsert(phone).futureValue
      dao.delete(phone.phone).futureValue
      dao.list().futureValue.toList shouldEqual List()
    }
    "put big start id" in {
      val phone = MtsPhone(PhoneGen.next, Long.MaxValue, DateTime.now(TimeZone))
      dao.upsert(phone).futureValue
    }
  }
}
