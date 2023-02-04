package ru.yandex.vertis.telepony.dao.ydb

import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.{CallV2Gen, QualifierGen, TagGen}
import ru.yandex.vertis.telepony.model.{Tag, TypedDomains}
import ru.yandex.vertis.zio.ZioSpecBase
import vertis.ydb.test.YdbTest

class YdbRecentCallsDaoSpec extends ZioSpecBase with YdbTest with Suite with SpecBase {

  import ru.yandex.vertis.telepony.generator.Producer._

  val dao = new YdbRecentCallsDao(TypedDomains.autoru_def, ydbWrapper)

  override def afterStart(): Unit = {
    super.afterStart()
    runSync(dao.init()).get
  }

  "YdbRecentCallDao" should {
    "upsert and return calls stats" in ioTest {
      val call = CallV2Gen.next.copy(time = DateTime.now())
      for {
        _ <- dao.upsertCall(call)
        r <- dao.getRecentCallsStat(call.redirect.key.objectId, call.redirect.key.tag)
        _ <- check {
          r.count should be(1)
          r.lastCallTime.value should be(call.time)
        }
      } yield ()
    }

    "upsert and return calls stats for empty tag" in ioTest {
      val call = CallV2Gen.suchThat(_.redirect.key.tag.asOption.isEmpty).next.copy(time = DateTime.now())
      for {
        _ <- dao.upsertCall(call)
        r <- dao.getRecentCallsStat(call.redirect.key.objectId, call.redirect.key.tag)
        _ <- check {
          r.count should be(1)
          r.lastCallTime.value should be(call.time)
        }
      } yield ()
    }

    "return call stats only for last day" in ioTest {
      val call1 = CallV2Gen.next.copy(time = DateTime.now())
      val call2 = call1.copy(id = "test_12345", time = DateTime.now().minusDays(1))
      for {
        _ <- dao.upsertCall(call1)
        _ <- dao.upsertCall(call2)
        r <- dao.getRecentCallsStat(call1.redirect.key.objectId, call1.redirect.key.tag)
        _ <- check {
          r.count should be(1)
          r.lastCallTime.value should be(call1.time)
        }
      } yield ()
    }

    "return empty call stats" in ioTest {
      for {
        r <- dao.getRecentCallsStat(QualifierGen.next, TagGen.next)
        _ <- check {
          r.count should be(0)
          r.lastCallTime should be(None)
        }
      } yield ()
    }
  }
}
