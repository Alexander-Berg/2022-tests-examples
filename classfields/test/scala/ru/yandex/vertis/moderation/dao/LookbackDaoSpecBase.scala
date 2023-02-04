package ru.yandex.vertis.moderation.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.LookbackDao.{Filter, LookbackRow}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.dao.LookbackDaoSpecBase.DecreasingDateTimeIterator

/**
  * @author semkagtn
  */
trait LookbackDaoSpecBase[A] extends SpecBase {

  def lookbackDao: LookbackDao[A]

  def payloadGen: Gen[A]

  val rowGen: Gen[LookbackRow[A]] =
    for {
      instanceId <- InstanceIdGen
      payload    <- payloadGen
      timestamp = DecreasingDateTimeIterator.next()
    } yield LookbackRow(timestamp, instanceId, payload)

  before {
    lookbackDao.clear().futureValue
  }

  "upsert" should {

    "correctly insert new row" in {
      val row = rowGen.next
      lookbackDao.upsert(row).futureValue

      val filter = Filter.Before(row.timestamp, limit = 1)
      val actualResult = lookbackDao.find(filter).futureValue.toSet
      val expectedResult = Set(row)

      actualResult shouldBe expectedResult
    }

    "correctly update row" in {
      val oldPayload = payloadGen.next
      val oldRow = rowGen.next.copy(payload = oldPayload)
      lookbackDao.upsert(oldRow).futureValue

      val newPayload = payloadGen.suchThat(_ != oldPayload).next
      val newRow = oldRow.copy(payload = newPayload)
      lookbackDao.upsert(newRow).futureValue

      val filter = Filter.Before(newRow.timestamp, limit = 1)
      val actualResult = lookbackDao.find(filter).futureValue.toSet
      val expectedResult = Set(newRow)

      actualResult shouldBe expectedResult
    }
  }

  "find" should {

    "correctly find rows" in {
      val firstRow = rowGen.next
      val secondRow = rowGen.next
      val thirdRow = rowGen.next
      assume(secondRow.timestamp.isBefore(firstRow.timestamp))
      assume(thirdRow.timestamp.isBefore(secondRow.timestamp))

      lookbackDao.upsert(firstRow).futureValue
      lookbackDao.upsert(secondRow).futureValue
      lookbackDao.upsert(thirdRow).futureValue

      val filter = Filter.Before(secondRow.timestamp, limit = 3)
      val actualResult = lookbackDao.find(filter).futureValue.toSet
      val expectedResult = Set(secondRow, thirdRow)

      actualResult shouldBe expectedResult
    }

    "returns no more than limit rows" in {
      val firstRow = rowGen.next
      val secondRow = rowGen.next

      lookbackDao.upsert(firstRow).futureValue
      lookbackDao.upsert(secondRow).futureValue

      val timestamp = Seq(firstRow.timestamp, secondRow.timestamp).maxBy(_.getMillis)
      val filter = Filter.Before(timestamp, limit = 1)
      val actualResult = lookbackDao.find(filter).futureValue.size
      val expectedResult = 1

      actualResult shouldBe expectedResult
    }
  }

  "delete" should {

    "correctly delete row" in {
      val firstRow = rowGen.next
      val secondRow = rowGen.next
      assume(secondRow.timestamp.isBefore(firstRow.timestamp))

      lookbackDao.upsert(firstRow).futureValue
      lookbackDao.upsert(secondRow).futureValue

      lookbackDao.delete(firstRow).futureValue

      val filter = Filter.Before(firstRow.timestamp, limit = 2)
      val actualResult = lookbackDao.find(filter).futureValue.toSet
      val expectedResult = Set(secondRow)

      actualResult shouldBe expectedResult
    }

    "do nothing if row doesn't exist" in {
      val nonexistentRow = rowGen.next

      lookbackDao.delete(nonexistentRow).futureValue
    }
  }

  "minTimestamp" should {

    "return correct result" in {
      val minTimestamp = DateTimeGen.next
      val timestamp = minTimestamp.plusDays(1)

      val firstRow = rowGen.next.copy(timestamp = minTimestamp)
      val secondRow = rowGen.next.copy(timestamp = timestamp)

      lookbackDao.upsert(firstRow).futureValue
      lookbackDao.upsert(secondRow).futureValue

      val actualResult = lookbackDao.minTimestamp().futureValue
      val expectedResult = Some(minTimestamp)
      actualResult shouldBe expectedResult
    }

    "return None if no rows" in {
      val actualResult = lookbackDao.minTimestamp().futureValue
      val expectedResult = None
      actualResult shouldBe expectedResult
    }
  }
}

object LookbackDaoSpecBase {

  val DecreasingDateTimeIterator: Iterator[DateTime] = Iterator.iterate(DateTimeUtil.now())(_.minusYears(1))
}
