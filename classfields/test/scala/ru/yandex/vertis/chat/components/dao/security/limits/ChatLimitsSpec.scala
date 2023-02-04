package ru.yandex.vertis.chat.components.dao.security.limits

import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.chat.components.dao.statistics.StatisticsService
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.chat.{RequestContext, SpecBase}
import ru.yandex.vertis.generators.DateTimeGenerators.instantInPast
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import scala.concurrent.duration._

class ChatLimitsSpec extends SpecBase with MockFactory with RequestContextAware {

  implicit private val ec: ExecutionContext = ExecutionContext.Implicits.global

  "ChatLimits" should {
    "pass any input with empty rules" in {
      val statistics = mock[StatisticsService]
      (statistics
        .getRoomsTsForDay(_: UserId)(_: RequestContext))
        .expects(*, *)
        .never()
      ChatLimits.Empty.roomsComplies(userId.next, statistics).futureValue shouldBe true
    }

    "limit number of rooms with one rule" in {
      val limits = ChatLimits(Map(2.minutes -> 5))
      val statistics = mock[StatisticsService]
      val dates = successful(instantInPast(2.minutes).next(5).map(_.toDateTime).toSeq)
      (statistics
        .getRoomsTsForDay(_: UserId)(_: RequestContext))
        .expects(*, *)
        .once()
        .returning(dates)
      limits.roomsComplies(userId.next, statistics).futureValue shouldBe false
    }

    "pass when complies with one rules" in {
      val limits = ChatLimits(Map(2.minutes -> 5))
      val statistics = mock[StatisticsService]
      val dates = successful(instantInPast(2.minutes).next(4).map(_.toDateTime).toSeq)
      (statistics
        .getRoomsTsForDay(_: UserId)(_: RequestContext))
        .expects(*, *)
        .once()
        .returning(dates)
      limits.roomsComplies(userId.next, statistics).futureValue shouldBe true
    }

    "limit number of rooms with multiple rules" in {
      val limits = ChatLimits(Map(1.hour -> 10, 2.minutes -> 5))
      val statistics = mock[StatisticsService]
      val dates = successful(instantInPast(2.minutes).next(5).map(_.toDateTime).toSeq)
      (statistics
        .getRoomsTsForDay(_: UserId)(_: RequestContext))
        .expects(*, *)
        .once()
        .returning(dates)
      limits.roomsComplies(userId.next, statistics).futureValue shouldBe false
    }

    "pass when complies with multiple rules" in {
      val limits = ChatLimits(Map(1.hour -> 10, 2.minutes -> 5))
      val statistics = mock[StatisticsService]
      val dates = successful(instantInPast(2.minutes).next(4).map(_.toDateTime).toSeq)
      (statistics
        .getRoomsTsForDay(_: UserId)(_: RequestContext))
        .expects(*, *)
        .once()
        .returning(dates)
      limits.roomsComplies(userId.next, statistics).futureValue shouldBe true
    }

    "limit number of first message with one rule" in {
      val limits = ChatLimits(Map.empty[FiniteDuration, Int], Map(1.minute -> 5))
      val statistics = mock[StatisticsService]
      val dates = successful(instantInPast(1.minutes).next(5).map(_.toDateTime).toSeq)
      (statistics
        .getFirstMessagesTsForOneMinute(_: UserId)(_: RequestContext))
        .expects(*, *)
        .once()
        .returning(dates)
      limits.messagesComplies(userId.next, statistics).futureValue shouldBe false
    }
  }
}
