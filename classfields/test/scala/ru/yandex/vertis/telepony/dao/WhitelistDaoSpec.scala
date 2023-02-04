package ru.yandex.vertis.telepony.dao

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.util.Threads

import scala.concurrent.Future

/**
  * @author neron
  */
trait WhitelistDaoSpec extends SpecBase with BeforeAndAfterEach {

  def dao: WhitelistDao

  import Threads.lightWeightTasksEc

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dao.clear().futureValue
  }

  "dao" should {
    "add" in {
      val entry = WhitelistEntryGen.next
      dao.upsert(entry).futureValue
      dao.get(entry.source).futureValue should ===(Some(entry))
    }

    "delete" in {
      val entry = WhitelistEntryGen.next
      dao.upsert(entry).futureValue
      dao.delete(entry.source).futureValue should ===(true)
      dao.get(entry.source).futureValue should ===(None)
    }

    "getAll" in {
      val entries = WhitelistEntryGen.nextUnique(10).toSeq
      Future.sequence(entries.map(dao.upsert)).futureValue
      dao.getAll.futureValue.toSet should ===(entries.toSet)
    }
  }

}
