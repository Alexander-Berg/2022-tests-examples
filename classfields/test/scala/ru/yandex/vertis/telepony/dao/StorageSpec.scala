package ru.yandex.vertis.telepony.dao

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.Entry
import ru.yandex.vertis.telepony.util.Range.Full

/**
  * @author evans
  */
trait StorageSpec extends SpecBase with BeforeAndAfterEach {

  def kvDao: Storage[String]

  override protected def beforeEach(): Unit = {
    kvDao.clear().futureValue
    super.beforeEach()
  }

  private val SampleKey = "1"
  private val SampleValue = "2"

  "KVDao" should {
    "put" in {
      kvDao.put(SampleKey, SampleValue).futureValue
    }

    "get" in {
      kvDao.put(SampleKey, SampleValue).futureValue
      val x: String = kvDao.get(SampleKey).futureValue
      x shouldEqual SampleValue
    }

    "custom case" in {
      kvDao.put(SampleKey, SampleValue).futureValue
      kvDao.put("x", "y").futureValue
      kvDao.put(SampleKey, "3").futureValue
      kvDao.get(SampleKey).futureValue shouldEqual "3"
      kvDao.get("x").futureValue shouldEqual "y"
    }

    "list" in {
      kvDao.put(SampleKey, SampleValue).futureValue
      kvDao.put("x", SampleValue).futureValue
      kvDao.list(Full).futureValue.toSet shouldEqual
        Set(Entry(SampleKey, SampleValue), Entry("x", SampleValue))
    }
  }
}
