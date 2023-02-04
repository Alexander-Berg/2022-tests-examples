package ru.auto.api.services.counter

import java.time.LocalDate

import org.scalatest.Ignore
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.{OfferID, RequestParams}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.{Request, RequestImpl}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 13.02.17
  */
@Ignore
class OctopusCounterClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("https", "octopus.api.team-php-01-sas.dev.vertis.yandex.net", 443)

  val counterClient = new OctopusCounterClient(http)

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }

  test("empty list") {
    val res = counterClient.getCounters(Category.CARS, Seq.empty).futureValue
    res shouldBe empty
  }

  test("get counters for one offer") {
    val id = OfferID.parse("1047448724-f6a69")
    val res = counterClient.getCounters(Category.CARS, Seq(id)).futureValue
    res should have size 1
  }

  test("get counters for many offers") {
    val id1 = OfferID.parse("1047448724-f6a69")
    val id2 = OfferID.parse("1047504516-dfd0f7")

    val res = counterClient.getCounters(Category.CARS, Seq(id1, id2)).futureValue
    res should have size 2
  }

  test("increment phone counter") {
    val id = OfferID.parse("1047448724-f6a69")

    val before = counterClient.getCounters(Category.CARS, Seq(id)).futureValue.apply(id)
    counterClient.incrementPhoneCounter(Category.CARS, id, None).futureValue
    val after = counterClient.getCounters(Category.CARS, Seq(id)).futureValue.apply(id)

    after.getPhoneAll shouldEqual (before.getPhoneAll + 1)
    after.getPhoneDaily shouldEqual (before.getPhoneDaily + 1)
  }

  test("get stat counters") {
    val id = OfferID.parse("1047448724-f6a69")
    val to = LocalDate.parse("2018-12-06")
    val from = to.minusDays(5)

    val res = counterClient.getStatCounters(Category.CARS, Seq(id), from, to).futureValue

    res should have size 1
  }

  test("get stat counters for many offers") {
    val id = OfferID.parse("1047448724-f6a69")
    val id2 = OfferID.parse("1047504516-dfd0f7")
    val to = LocalDate.parse("2018-12-06")
    val from = to.minusDays(5)

    val res = counterClient.getStatCounters(Category.CARS, Seq(id, id2), from, to).futureValue

    res should have size 2
  }

  test("get total counters") {
    val id1 = "1043045004-977b3"
    val id2 = "1045596990-a96e8"
    val ids = List(OfferID.parse(id1), OfferID.parse(id2))
    val List(counter1, counter2) = counterClient.getTotalCounters(Category.CARS, ids).futureValue.toList: @unchecked
    counter1.saleId shouldBe id1
    counter1.views should be >= 182
    counter2.saleId shouldBe id2
    counter2.views should be >= 8
  }

  test("not get total counters for non-existing offer ids") {
    val id1 = "1043045005-977b3"
    val id2 = "1045596991-a96e8"
    val ids = List(OfferID.parse(id1), OfferID.parse(id2))
    val ex = counterClient.getTotalCounters(Category.CARS, ids).failed.futureValue
    ex shouldBe an[OfferNotFoundException]
  }
}
