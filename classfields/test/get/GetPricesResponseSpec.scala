package ru.yandex.vertis.billing.howmuch.model.get

import billing.common_model.{Money, Project}
import billing.howmuch.price_service.GetPricesResponseEntry
import billing.howmuch.price_service.GetPricesResponseEntry.Result
import billing.howmuch.price_service.GetPricesResponseEntry.Result.NotFound
import billing.howmuch.{price_service => proto}
import billing.howmuch.model.{Source => ProtoSource}
import cats.data.NonEmptyMap
import common.time.Interval
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object GetPricesResponseSpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testMatrixId = "call"
  private def testKey(context: String) = RuleKey(MatrixId(testProject, testMatrixId), Context.fromString(context))
  private val testFrom = Instant.ofEpochMilli(100500)
  private val testSource = Source.StartrekTicket("VSMONEY-2750")

  private def testRule(context: String, price: Kopecks) =
    Rule(testKey(context), Interval(testFrom, to = None), price, testSource)

  override def spec: ZSpec[TestEnvironment, Any] = suite("GetPricesResponseSpec")(
    test("convert to proto properly") {
      val rule1 = testRule("mark=AUDI&model=Q7", Kopecks(3000))
      val rule3 = testRule("mark=AUDI&model=A3", Kopecks(5000))
      val expected =
        proto.GetPricesResponse(
          Seq(
            GetPricesResponseEntry(
              entryId = "1",
              ruleResponse(
                id = "cb8e5f3886933127ea2be270be4d558d25218ef5110b490fa20d6af208539cff",
                Money(kopecks = 3000),
                ProtoSource(ProtoSource.Source.StartrekTicket("VSMONEY-2750"))
              )
            ),
            GetPricesResponseEntry(entryId = "2", NotFound(true)),
            GetPricesResponseEntry(
              entryId = "3",
              ruleResponse(
                id = "71945a1fb75999842c71082f1ec2e5afaf335ad1547798d353f80c5179ebad79",
                Money(kopecks = 5000),
                ProtoSource(ProtoSource.Source.StartrekTicket("VSMONEY-2750"))
              )
            )
          )
        )
      val actual = GetPricesResponse(
        NonEmptyMap.of(EntryId("1") -> Some(rule1), EntryId("2") -> None, EntryId("3") -> Some(rule3))
      )
      assert(actual)(equalTo(expected))
    }
  )

  private def ruleResponse(id: String, money: Money, source: ProtoSource) =
    Result.Rule(GetPricesResponseEntry.Rule(id, Some(money), Some(source)))
}
