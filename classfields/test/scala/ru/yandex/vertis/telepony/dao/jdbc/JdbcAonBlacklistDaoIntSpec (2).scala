package ru.yandex.vertis.telepony.dao.jdbc

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.AonBlacklistDao
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.util.{SharedDbSupport, Threads}

import scala.annotation.nowarn
import scala.concurrent.Future

/**
  * @author neron
  */
class JdbcAonBlacklistDaoIntSpec extends SpecBase with BeforeAndAfterEach with SharedDbSupport {

  val dao: AonBlacklistDao = new JdbcAonBlacklistDao(sharedDualDb)

  import Threads.lightWeightTasksEc

  @nowarn
  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dao.clear().futureValue
  }

  "ReasonBlacklistDao" should {
    "upsert first time" in {
      val info = AonBlockInfoGen.next
      dao.upsert(info).futureValue
      val foundBs = dao.get(info.source).futureValue.get
      foundBs should ===(info)
    }

    "not add when upsert second time" in {
      val info = AonBlockInfoGen.next
      dao.upsert(info).futureValue
      dao.upsert(info).futureValue
      val foundBs = dao.get(info.source).futureValue.get
      foundBs should ===(info)
      dao.all.futureValue should have size 1
    }

    "update reason when upsert" in {
      val source = RefinedSourceGen.next
      val infoOld = AonBlockInfoGen.next.copy(source = source)
      val infoNew = AonBlockInfoGen.suchThat(_.verdict != infoOld.verdict).next.copy(source = source)
      dao.upsert(infoOld).futureValue
      dao.upsert(infoNew).futureValue
      val foundBs = dao.get(infoNew.source).futureValue.get
      foundBs should ===(infoNew)
      dao.all.futureValue should have size 1
    }

    "delete" in {
      val info1 = AonBlockInfoGen.next
      val info2 = AonBlockInfoGen.next
      dao.upsert(info1).futureValue
      dao.upsert(info2).futureValue
      val isDeleted = dao.delete(info2.source).futureValue
      isDeleted should ===(true)
      dao.get(info1.source).futureValue shouldBe defined
      dao.get(info2.source).futureValue should not be defined
    }

    "return all" in {
      val bss = 1.to(10).map(_ => AonBlockInfoGen.next)
      Future.sequence(bss.map(dao.upsert)).futureValue
      dao.all.futureValue.toSet should ===(bss.toSet)
    }
  }
}
