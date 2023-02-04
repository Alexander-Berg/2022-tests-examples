package ru.yandex.vertis.telepony.dao.ydb

import org.joda.time.DateTime
import org.scalatest.Suite
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.{CallV2Gen, PhoneGen, QualifierGen, RefinedSourceGen, ShortStr, TagGen}
import ru.yandex.vertis.telepony.model.TypedDomains
import vertis.zio.test.ZioSpecBase
import ru.yandex.vertis.telepony.generator.Generator.{PhoneGen, RefinedSourceGen, ShortStr}
import vertis.ydb.test.YdbTest

class YdbSourceTargetCallDaoSpec extends ZioSpecBase with YdbTest with Suite with SpecBase {

  import ru.yandex.vertis.telepony.generator.Producer._

  val dao = new YdbSourceTargetCallDao(ydbWrapper)

  override def beforeAll(): Unit = {
    super.beforeAll()
    runSync(dao.createTable).get
  }

  "YdbSourceTargetCallDao" should {
    "upsert and return count" in ioTest {
      val source = RefinedSourceGen.next
      val target = PhoneGen.next
      val time = DateTime.now()
      for {
        _ <- dao.upsert(ShortStr.next, source, target, time)
        _ <- dao.upsert(ShortStr.next, source, target, time.minusHours(2))
        count <- dao.count(source, target, time.minusHours(1))
        _ <- check {
          count shouldBe 1
        }
      } yield ()
    }
    "upsert and return count unique calls" in ioTest {
      val source = RefinedSourceGen.next
      val target1 = PhoneGen.next
      val target2 = PhoneGen.next
      val time = DateTime.now()
      for {
        _ <- dao.upsert(ShortStr.next, source, target1, time)
        _ <- dao.upsert(ShortStr.next, source, target1, time.minusSeconds(1))
        _ <- dao.upsert(ShortStr.next, source, target1, time.minusMinutes(1))
        _ <- dao.upsert(ShortStr.next, source, target2, time)
        _ <- dao.upsert(ShortStr.next, source, target2, time.minusHours(2))
        count <- dao.countUniqueTargetCalls(source, time.minusHours(3))
        _ <- check {
          count shouldBe 2
        }
      } yield ()
    }
  }
}
