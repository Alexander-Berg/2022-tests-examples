package ru.yandex.vertis.telepony.dao.jdbc

import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.generators.NetGenerators.asProducer
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.BlacklistUsernameDao
import ru.yandex.vertis.telepony.generator.Generator.{BlockUsernameInfoGen, ShortStr}
import ru.yandex.vertis.telepony.util.{SharedDbSupport, Threads}

import scala.annotation.nowarn
import scala.concurrent.Future

@nowarn
class JdbcBlacklistUsernameDaoIntSpec extends SpecBase with BeforeAndAfterEach with SharedDbSupport {

  val dao: BlacklistUsernameDao = new JdbcBlacklistUsernameDao(sharedDualDb)

  import Threads.lightWeightTasksEc

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dao.clear().futureValue
  }

  "BlacklistUsernameDao" should {
    "upsert first time" in {
      val info = BlockUsernameInfoGen.next
      dao.upsert(info).futureValue
      val foundBs = dao.get(info.voxUsername).futureValue
      foundBs.get shouldBe info
    }

    "not add when upsert second time" in {
      val info = BlockUsernameInfoGen.next
      dao.upsert(info).futureValue
      dao.upsert(info).futureValue
      val foundBs = dao.get(info.voxUsername).futureValue
      foundBs.get shouldBe info
      dao.all.futureValue should have size 1
    }

    "update reason when upsert" in {
      val infoOld = BlockUsernameInfoGen.next
      val infoNew = infoOld.copy(comment = Gen.option(ShortStr).next)
      dao.upsert(infoOld).futureValue
      dao.upsert(infoNew).futureValue
      val foundBs = dao.get(infoNew.voxUsername).futureValue
      foundBs.get shouldBe infoNew
      dao.all.futureValue should have size 1
    }

    "delete" in {
      val info1 = BlockUsernameInfoGen.next
      val info2 = BlockUsernameInfoGen.next
      dao.upsert(info1).futureValue
      dao.upsert(info2).futureValue
      val isDeleted = dao.delete(info2.voxUsername).futureValue
      isDeleted should ===(true)
      dao.get(info1.voxUsername).futureValue should not be empty
      dao.get(info2.voxUsername).futureValue shouldBe empty
    }

    "return all" in {
      val bss = 1.to(10).map(_ => BlockUsernameInfoGen.next)
      Future.sequence(bss.map(dao.upsert)).futureValue
      dao.all.futureValue.toSet should ===(bss.toSet)
    }
  }

}
