package ru.yandex.vertis.telepony.dao

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.telepony.dao.MarkedCallDao.{Filter, MarkSources}
import ru.yandex.vertis.telepony.generator.Generator.MarkedCallGen
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

import scala.concurrent.Future

/**
  * @author ruslansd
  */
trait MarkedCallDaoSpec extends SpecBase with DatabaseSpec with BeforeAndAfterEach with ScalaCheckDrivenPropertyChecks {

  def markedCallDao: MarkedCallDao

  import Threads.lightWeightTasksEc

  override protected def beforeEach(): Unit = {
    markedCallDao.clear().futureValue
    super.beforeEach()
  }

  "MarkedCallDao" should {
    "store marked call" in {
      forAll(MarkedCallGen) { markedCall =>
        markedCallDao.store(markedCall).futureValue
        val calls = markedCallDao.get(Filter.ForCallId(markedCall.callId)).futureValue
        calls should contain theSameElementsAs Seq(markedCall)
      }
    }

    "store marked call from differ sources" in {
      forAll(MarkedCallGen) { markedCall =>
        val markedCalls = MarkSources.values.toList.map(s => markedCall.copy(source = s))
        Future.traverse(markedCalls)(c => markedCallDao.store(c)).futureValue
        val calls = markedCallDao.get(Filter.ForCallId(markedCall.callId)).futureValue
        calls should contain theSameElementsAs markedCalls
      }
    }

  }

}
