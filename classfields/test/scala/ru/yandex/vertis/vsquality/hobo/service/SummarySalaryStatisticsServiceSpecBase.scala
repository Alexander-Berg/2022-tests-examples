package ru.yandex.vertis.vsquality.hobo.service

import ru.yandex.vertis.vsquality.hobo.model.SummarySalaryStatistics
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

/**
  * Base specs on [[SummarySalaryStatisticsService]]
  *
  * @author semkagtn
  */
trait SummarySalaryStatisticsServiceSpecBase extends SpecBase {

  def summarySalaryStatisticsService: SummarySalaryStatisticsService

  implicit private val rc: AutomatedContext = AutomatedContext("unit-test")

  before {
    summarySalaryStatisticsService.clear().futureValue
  }

  "get" should {

    "return correct statistics" in {
      val user = UserIdGen.next
      val statistics = SummarySalaryStatisticsGen.next
      summarySalaryStatisticsService.update(user, statistics).futureValue
      val actualResult = summarySalaryStatisticsService.get(user).futureValue
      actualResult should smartEqual(statistics)
    }

    "return empty statistics if statistics for user wasn't set before" in {
      val user = UserIdGen.next
      val actualResult = summarySalaryStatisticsService.get(user).futureValue
      val expectedResult = SummarySalaryStatistics.Empty
      actualResult should smartEqual(expectedResult)
    }
  }

  "getAll" should {

    "return all values" in {
      val user1 = UserIdGen.next
      val statistics1 = SummarySalaryStatisticsGen.next
      summarySalaryStatisticsService.update(user1, statistics1).futureValue

      val user2 = UserIdGen.next
      val statistics2 = SummarySalaryStatisticsGen.next
      summarySalaryStatisticsService.update(user2, statistics2).futureValue

      val actualResult = summarySalaryStatisticsService.getAll().futureValue
      val expectedResult =
        Map(
          user1 -> statistics1,
          user2 -> statistics2
        )
      actualResult shouldBe expectedResult
    }

    "return empty map if no data" in {
      val actualResult = summarySalaryStatisticsService.getAll().futureValue
      val expectedResult = Map.empty
      actualResult shouldBe expectedResult
    }
  }

  "update" should {

    "correctly replace salary statistics" in {
      val user = UserIdGen.next
      val oldSalaryStatistics = SummarySalaryStatisticsGen.next
      val newSalaryStatistics = SummarySalaryStatisticsGen.next
      summarySalaryStatisticsService.update(user, oldSalaryStatistics).futureValue
      summarySalaryStatisticsService.update(user, newSalaryStatistics).futureValue
      val actualResult = summarySalaryStatisticsService.get(user).futureValue
      actualResult should smartEqual(newSalaryStatistics)
    }
  }

  "updateAll" should {

    "correctly update all specified user-stat pairs" in {
      val user1 = UserIdGen.next
      val stat1 = SummarySalaryStatisticsGen.next
      summarySalaryStatisticsService.update(user1, stat1).futureValue

      val user2 = UserIdGen.next
      val stat2 = SummarySalaryStatisticsGen.next
      summarySalaryStatisticsService.update(user2, stat2).futureValue

      val user3 = UserIdGen.next
      val stat3 = SummarySalaryStatisticsGen.next
      val newStat1 = SummarySalaryStatisticsGen.next
      val pairs = Map(user1 -> newStat1, user3 -> stat3)
      summarySalaryStatisticsService.updateAll(pairs).futureValue

      val actualResult1 = summarySalaryStatisticsService.get(user1).futureValue
      actualResult1 should smartEqual(newStat1)
      val actualResult2 = summarySalaryStatisticsService.get(user2).futureValue
      actualResult2 should smartEqual(stat2)
      val actualResult3 = summarySalaryStatisticsService.get(user3).futureValue
      actualResult3 should smartEqual(stat3)
    }
  }
}
