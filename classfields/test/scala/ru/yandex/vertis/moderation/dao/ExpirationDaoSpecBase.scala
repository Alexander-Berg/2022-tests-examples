package ru.yandex.vertis.moderation.dao

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.extdata.core.gens.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.dao.ExpirationDao.ExpirationRow
import ru.yandex.vertis.moderation.dao.ExpirationDaoSpecBase.DecreasingDateTimeIterator
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.InstanceIdGen
import ru.yandex.vertis.moderation.util.DateTimeUtil

trait ExpirationDaoSpecBase extends SpecBase {

  def expirationDao: ExpirationDao

  val rowGen: Gen[ExpirationRow] =
    for {
      instanceId <- InstanceIdGen
      timestamp = DecreasingDateTimeIterator.next()
    } yield ExpirationRow(timestamp, instanceId)

  before {
    expirationDao.clear().futureValue
  }

  "upsert" should {

    "correctly insert new row" in {
      val row = rowGen.next
      expirationDao.upsert(row).futureValue

      val actualResult = expirationDao.findBefore(row.timestamp, limit = 1).futureValue.toSet
      val expectedResult = Set(row)

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

      expirationDao.upsert(firstRow).futureValue
      expirationDao.upsert(secondRow).futureValue
      expirationDao.upsert(thirdRow).futureValue

      val actualResult = expirationDao.findBefore(secondRow.timestamp, limit = 3).futureValue.toSet
      val expectedResult = Set(secondRow, thirdRow)

      actualResult shouldBe expectedResult
    }

    "returns no more than limit rows" in {
      val firstRow = rowGen.next
      val secondRow = rowGen.next

      expirationDao.upsert(firstRow).futureValue
      expirationDao.upsert(secondRow).futureValue

      val timestamp = Seq(firstRow.timestamp, secondRow.timestamp).maxBy(_.getMillis)
      val actualResult = expirationDao.findBefore(timestamp, limit = 1).futureValue.size
      val expectedResult = 1

      actualResult shouldBe expectedResult
    }
  }

  "delete" should {

    "correctly delete row" in {
      val firstRow = rowGen.next
      val secondRow = rowGen.next
      assume(secondRow.timestamp.isBefore(firstRow.timestamp))

      expirationDao.upsert(firstRow).futureValue
      expirationDao.upsert(secondRow).futureValue

      expirationDao.delete(firstRow).futureValue

      val actualResult = expirationDao.findBefore(firstRow.timestamp, limit = 2).futureValue.toSet
      val expectedResult = Set(secondRow)

      actualResult shouldBe expectedResult
    }

    "do nothing if row doesn't exist" in {
      val nonexistentRow = rowGen.next

      expirationDao.delete(nonexistentRow).futureValue
    }
  }
}

object ExpirationDaoSpecBase {

  private val DecreasingDateTimeIterator: Iterator[DateTime] = Iterator.iterate(DateTimeUtil.now())(_.minusYears(1))
}
