package ru.yandex.auto.vin.decoder.yt

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.raw.{RawModelManager, VinRawModel}
import auto.carfax.common.utils.misc.StringUtils.RichString
import ru.yandex.auto.vin.decoder.yt.diff.DbActions.{Delete, Insert}
import ru.yandex.auto.vin.decoder.yt.diff.DiffReducer
import ru.yandex.auto.vin.decoder.yt.diff.DiffReducer.Item
import ru.yandex.vertis.mockito.MockitoSupport

class DiffReducerTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfter {

  val TestVin = "ABC"
  val TestGroupId = "g1"
  val ProcessTimestamp = 123L
  val EventTypeSource = EventType.UNKNOWN
  val rawModelManager = mock[RawModelManager[VinCode, VinRawModel]]

  before {
    reset(rawModelManager)
  }

  "build action" should {
    "insert" when {
      "db list is empty" in {
        val reducer = new DiffReducer(ProcessTimestamp, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq.empty, item)

        assert(res === Some(Insert(TestVin, TestGroupId, 100L, 100L, ProcessTimestamp, item.hash, "raw", "")))
      }
      "not equal prev and there are not next" in {
        val reducer = new DiffReducer(ProcessTimestamp, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val dbFirst = buildRecord(1L, "raw", true)
        val dbPrev = buildRecord(10L, "raw2", true)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq(dbFirst, dbPrev), item)

        assert(res === Some(Insert(TestVin, TestGroupId, 100, 100, ProcessTimestamp, item.hash, "raw", "")))
      }
      "not equal next and there are not prev" in {
        val reducer = new DiffReducer(ProcessTimestamp, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val dbNext = buildRecord(200L, "raw2", true)
        val dbLast = buildRecord(300L, "raw", true)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq(dbNext, dbLast), item)

        assert(res === Some(Insert(TestVin, TestGroupId, 100, 100, ProcessTimestamp, item.hash, "raw", "")))
      }
      "not equal prev and next" in {
        val reducer = new DiffReducer(ProcessTimestamp, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val dbFirst = buildRecord(1L, "raw", true)
        val dbPrev = buildRecord(10L, "raw2", true)
        val dbNext = buildRecord(101L, "raw2", true)
        val dbLast = buildRecord(200L, "raw", true)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq(dbFirst, dbPrev, dbNext, dbLast), item)

        assert(res === Some(Insert(TestVin, TestGroupId, 100, 100, ProcessTimestamp, item.hash, "raw", "")))
      }
    }
    "skip" when {
      "equals next and there are not prev" in {
        val reducer = new DiffReducer(123L, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val dbNext = buildRecord(200L, "raw", true)
        val dbLast = buildRecord(300L, "raw2", true)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq(dbNext, dbLast), item)

        assert(res === None)
      }
      "equals next and not equals prev" in {
        val reducer = new DiffReducer(123L, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val dbPrev = buildRecord(10L, "raw2", true)
        val dbNext = buildRecord(101L, "raw", true)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq(dbPrev, dbNext), item)

        assert(res === None)
      }
      "equals prev and there are not next" in {
        val reducer = new DiffReducer(123L, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val db = buildRecord(1L, "raw", true)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq(db), item)

        assert(res === None)
      }
      "equals prev and not equals next" in {
        val reducer = new DiffReducer(123L, 123L, false, rawModelManager, "vin")

        val item = buildRecord(100L, "raw", false)
        val dbPrev = buildRecord(1L, "raw", true)
        val dbNext = buildRecord(101L, "raw2", true)
        val res = reducer.buildAction(TestVin, TestGroupId, Seq(dbPrev, dbNext), item)

        assert(res === None)
      }
    }
  }

  "build action list" should {
    "contains correct actions" when {
      "db list is empty, insert 2 different records" in {
        val reducer = new DiffReducer(ProcessTimestamp, 123L, false, rawModelManager, "vin")

        val item1 = buildRecord(100L, "raw", false)
        val item2 = buildRecord(200L, "raw2", false)
        val res = reducer.buildDiff(TestVin, TestGroupId, Seq.empty, Seq(item1, item2))

        assert(res.length === 2)
        assert(
          res(0) === Insert(
            TestVin,
            TestGroupId,
            item2.dataTimestamp,
            item2.dataTimestamp,
            ProcessTimestamp,
            item2.hash,
            item2.raw,
            ""
          )
        )
        assert(
          res(1) === Insert(
            TestVin,
            TestGroupId,
            item1.dataTimestamp,
            item1.dataTimestamp,
            ProcessTimestamp,
            item1.hash,
            item1.raw,
            ""
          )
        )
      }
      "db list is empty, insert 2 equals records" in {
        val reducer = new DiffReducer(ProcessTimestamp, 123L, false, rawModelManager, "vin")

        val item1 = buildRecord(100L, "raw", false)
        val item2 = buildRecord(200L, "raw", false)
        val res = reducer.buildDiff(TestVin, TestGroupId, Seq.empty, Seq(item1, item2))

        assert(res.length === 1)
        assert(
          res(0) === Insert(
            TestVin,
            TestGroupId,
            item2.dataTimestamp,
            item2.dataTimestamp,
            ProcessTimestamp,
            item2.hash,
            item2.raw,
            ""
          )
        )
      }
      "one of new records equals db record and other is different" in {
        val reducer = new DiffReducer(ProcessTimestamp, 123L, false, rawModelManager, "vin")

        val dbRecord = buildRecord(200L, "raw", true)
        val item1 = buildRecord(150L, "raw", false)
        val item2 = buildRecord(101L, "raw2", alreadyInDb = false)
        val res = reducer.buildDiff(TestVin, TestGroupId, Seq(dbRecord), Seq(item1, item2))

        assert(res.length === 1)
        assert(
          res(0) === Insert(
            TestVin,
            TestGroupId,
            item2.dataTimestamp,
            item2.dataTimestamp,
            ProcessTimestamp,
            item2.hash,
            item2.raw,
            ""
          )
        )
      }
      "delete if there are no record in latest file" in {
        when(rawModelManager.alreadyDeleted(?)).thenReturn(false)
        val reducer = new DiffReducer(ProcessTimestamp, 150L, true, rawModelManager, "vin")

        val dbRecord = buildRecord(100L, "raw", true)
        val item1 = buildRecord(120L, "raw", false)
        val res = reducer.buildDiff(TestVin, TestGroupId, Seq(dbRecord), Seq(item1))

        assert(res.length === 1)
        assert(res(0) === Delete(TestVin, TestGroupId, 150L, 150L, ProcessTimestamp, "raw")) // todo
      }
      "don't delete if already deleted" in {
        when(rawModelManager.alreadyDeleted(?)).thenReturn(true)
        val reducer = new DiffReducer(ProcessTimestamp, 150L, true, rawModelManager, "vin")

        val dbRecord = buildRecord(100L, "raw", true)
        val item1 = buildRecord(120L, "raw", false)
        val res = reducer.buildDiff(TestVin, TestGroupId, Seq(dbRecord), Seq(item1))

        assert(res.length === 0)
      }
      "don't delete if there are record in latest file" in {
        when(rawModelManager.alreadyDeleted(?)).thenReturn(false)
        val reducer = new DiffReducer(ProcessTimestamp, 150L, true, rawModelManager, "vin")

        val dbRecord = buildRecord(100L, "raw", true)
        val item1 = buildRecord(150L, "raw", false)
        val res = reducer.buildDiff(TestVin, TestGroupId, Seq(dbRecord), Seq(item1))

        assert(res.length === 0)
      }
      "don't delete if there are more fresh record in db " in {
        val reducer = new DiffReducer(ProcessTimestamp, 150L, true, rawModelManager, "vin")

        val dbRecord = buildRecord(200L, "raw", true)
        val item1 = buildRecord(120L, "raw", false)
        val res = reducer.buildDiff(TestVin, TestGroupId, Seq(dbRecord), Seq(item1))

        assert(res.length === 0)
      }
    }
  }

  private def buildRecord(dataTimestamp: Long, raw: String, alreadyInDb: Boolean): Item = {

    Item(dataTimestamp, raw.md5hash(), alreadyInDb, raw, None)
  }

}
