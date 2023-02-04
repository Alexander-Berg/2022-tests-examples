package ru.yandex.vertis.moisha.impl.autoru

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.moisha.ProductPolicy.FromDateTime
import ru.yandex.vertis.moisha.environment.wholeDay
import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.{AutoRuRequest, AutoRuResponse}
import ru.yandex.vertis.moisha.impl.autoru.gens.RequestGen
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.model.gens.Producer

import scala.util.{Failure, Success, Try}

/**
  * Specs on [[AutoRuDailyPolicy]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class AutoRuDailyPolicySpec extends Matchers with WordSpecLike {

  val time: DateTime = DateTime.parse("2016-07-12T18:36:25.123+03:00")

  "AutoRuDailyPolicy.estimate" should {
    "correctly process request" in {
      val p1 = AutoRuProduct(
        Products.Placement,
        Set(AutoRuGood(Goods.Custom, Costs.PerIndexing, 10)),
        duration = DefaultDuration
      )
      val p2 = AutoRuProduct(
        Products.Placement,
        Set(AutoRuGood(Goods.Custom, Costs.PerIndexing, 20)),
        duration = DefaultDuration
      )

      val fdaily: AutoRuRequest => Try[AutoRuProduct] =
        request =>
          if (request.interval.from.equals(time.withTimeAtStartOfDay())) {
            Success(p1)
          } else {
            Success(p2)
          }

      val policy = new AutoRuDailyPolicy("policy", FromDateTime(time), Products.Placement, fdaily)

      val request = RequestGen.next.copy(
        product = Products.Placement,
        interval =
          DateTimeInterval(time.withTimeAtStartOfDay().minusDays(1), time.withTimeAtStartOfDay().plusDays(2).minus(1))
      )

      val result = policy.estimate(request)

      result match {
        case Success(AutoRuResponse(r, points)) =>
          r should be(request)
          points.size should be(2)
          points.exists(p => p.interval == wholeDay(time) && p.product == p1)
          points.exists(p => p.interval == wholeDay(time.plusDays(1)) && p.product == p2)
          points.exists(_.policy != "policy") should be(false)
        case other => fail(s"Unexpected $other")
      }
    }

    "fail if at least one daily function fails" in {
      val p1 = AutoRuProduct(
        Products.Placement,
        Set(AutoRuGood(Goods.Custom, Costs.PerIndexing, 10)),
        duration = DefaultDuration
      )
      val e = new IllegalArgumentException
      val fdaily: AutoRuRequest => Try[AutoRuProduct] =
        request =>
          if (request.interval.from.equals(time.withTimeAtStartOfDay())) {
            Failure(e)
          } else {
            Success(p1)
          }

      val policy = new AutoRuDailyPolicy("policy", FromDateTime(time), Products.Placement, fdaily)

      val request = RequestGen.next.copy(
        product = Products.Placement,
        interval =
          DateTimeInterval(time.withTimeAtStartOfDay().minusDays(1), time.withTimeAtStartOfDay().plusDays(2).minus(1))
      )

      val result = policy.estimate(request)

      result match {
        case Failure(`e`) =>
        case other => fail(s"Unexpected $other")
      }
    }
  }

}
