package ru.yandex.vertis.moisha.impl.autoru_quota

import ru.yandex.vertis.moisha.ProductPolicy
import ru.yandex.vertis.moisha.ProductPolicy.{FromDateTime, RangeDateTime}
import ru.yandex.vertis.moisha.environment._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.model.gens.Producer

import ru.yandex.vertis.moisha.impl.autoru_quota.AutoRuQuotaPolicy.{AutoRuQuotaRequest, AutoRuQuotaResponse}
import ru.yandex.vertis.moisha.impl.autoru_quota.gens.AutoRuQuotaRequestGen
import ru.yandex.vertis.moisha.impl.autoru_quota.model._

import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpecLike}
import scala.util.{Failure, Success}

trait SingleProductDailyPolicySpec extends Matchers with WordSpecLike {

  def inRegion(region: RegionId)(request: AutoRuQuotaRequest): AutoRuQuotaRequest =
    request.copy(context = request.context.copy(clientRegionId = region))

  def inCity(city: Option[RegionId])(request: AutoRuQuotaRequest): AutoRuQuotaRequest =
    request.copy(context = request.context.copy(clientCityId = city))

  def withInterval(interval: DateTimeInterval)(request: AutoRuQuotaRequest): AutoRuQuotaRequest =
    request.copy(interval = interval)

  def dailyRequest(request: AutoRuQuotaRequest): AutoRuQuotaRequest =
    request.copy(interval = wholeDay(now()))

  def withProduct(request: AutoRuQuotaRequest, product: Products.Value): AutoRuQuotaRequest =
    request.copy(product = product)

  def withAmount(size: Amount)(request: AutoRuQuotaRequest): AutoRuQuotaRequest =
    request.copy(context = request.context.copy(amount = size))

  def withPolicy(policy: PolicyId)(request: AutoRuQuotaRequest): AutoRuQuotaRequest =
    request.copy(context = request.context.copy(tariff = Some(policy)))

  def policy: ProductPolicy

  def testIterations: Int = 50

  type RequestSpec = AutoRuQuotaRequest => AutoRuQuotaRequest

  def correctInterval: DateTimeInterval =
    policy.lifetime match {
      case l: FromDateTime => wholeDay(l.from)
      case l: RangeDateTime => wholeDay(l.to.minusDays(1))
    }

  def incorrectInterval: DateTimeInterval =
    policy.lifetime match {
      case l: FromDateTime => wholeDay(l.from.minusDays(1))
      case l: RangeDateTime => wholeDay(l.to.plusDays(1))
    }

  def createRequest(
      interval: DateTimeInterval,
      product: Products.Value,
      specs: Iterable[RequestSpec]): AutoRuQuotaRequest = {
    val autoruRequest = withInterval(interval)(withProduct(AutoRuQuotaRequestGen.next, product))
    specs.foldLeft(autoruRequest)((r, s) => s(r))
  }

  def priceIn(min: Funds, max: Funds): Gen[Funds] =
    Gen.chooseNum(if (min == 0) 0 else min + 1, max)

  def checkPolicy(
      requestInterval: DateTimeInterval,
      expectedProduct: AutoRuQuotaProduct,
      requestSpecs: RequestSpec*): Unit = {
    val request = createRequest(requestInterval, expectedProduct.p, requestSpecs)
    policy.estimate(request) match {
      case Success(AutoRuQuotaResponse(`request`, points)) =>
        points.size should be(1)
        points.head.product should be(expectedProduct)
      case other => fail(s"Unexpected $other")
    }
  }

  def checkPolicyEmpty(interval: DateTimeInterval, product: Products.Value, requestSpecs: RequestSpec*): Unit = {
    val request = createRequest(interval, product, requestSpecs)
    policy.estimate(request) match {
      case Success(AutoRuQuotaResponse(`request`, points)) =>
        points.size should be(0)
      case other => fail(s"Unexpected $other")
    }
  }

  def checkPolicyFailure(interval: DateTimeInterval, product: Products.Value, requestSpecs: RequestSpec*): Unit = {
    val request = createRequest(interval, product, requestSpecs)
    policy.estimate(request) match {
      case Failure(e: IllegalArgumentException) =>
      case other => fail(s"Unexpected $other")
    }
  }
}
