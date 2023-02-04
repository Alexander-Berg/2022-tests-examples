package ru.yandex.vertis.billing.howmuch.model.patch

import billing.common_model.{Money, Project}
import billing.howmuch.price_service.PatchPricesRequestEntry.Patch
import billing.howmuch.{price_service => proto}
import common.time.Interval
import common.zio.testkit.failsWith
import ru.yandex.vertis.billing.common.money.Kopecks
import billing.howmuch.model.testkit._
import ru.yandex.vertis.billing.howmuch.model.core.Source.StartrekTicket
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.core.criteria.CriteriaKey
import ru.yandex.vertis.billing.howmuch.model.error.BadRequest.{EmptyPatch, EmptyRuleContext, StopNonExistingRule}
import ru.yandex.vertis.billing.howmuch.model.patch.PatchPricesRequestEntry.{Create, Stop, Update}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object PatchPricesRequestEntrySpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testSource = StartrekTicket("VSMONEY-2750")
  private val testMatrixId = "call"
  private val testKey = RuleKey(MatrixId(testProject, testMatrixId), Context.fromString("mark=AUDI&model=TT"))
  private val testFrom = Instant.ofEpochMilli(100500)
  private val testRuleContext = Some(mkRuleContext("mark" -> "AUDI", "model" -> "TT"))

  private val testSchema =
    Schema(
      List(
        TimedSchema(
          MatrixId(testProject, testMatrixId),
          Interval(from = testFrom, to = None),
          ContextSchema(List(CriteriaKey("mark"), CriteriaKey("model"))),
          allowFallback = false
        )
      ),
      historyUploadAllowed = Set()
    )

  override def spec: ZSpec[TestEnvironment, Any] = suite("PatchPricesRequestEntrySpec")(
    testM("return Create if there is no previous price, and next price is set") {
      val protoEntry = testRequestEntry(previousPrice = None, Patch.NextPrice(Money(5000)))
      val parsed = parseProtoEntry(protoEntry)
      assertM(parsed)(equalTo(Create(testRule(Kopecks(5000)))))
    },
    testM("throw if there is no previous price, and next price isn't set") {
      val protoEntry = testRequestEntry(previousPrice = None, Patch.StopRule(true))
      val parsed = parseProtoEntry(protoEntry).run
      assertM(parsed)(failsWith[StopNonExistingRule])
    },
    testM("return Update if there is a previous price, and next price is set") {
      val protoEntry = testRequestEntry(previousPrice = Some(Money(3000)), Patch.NextPrice(Money(5000)))
      val parsed = parseProtoEntry(protoEntry)
      assertM(parsed)(equalTo(Update(Kopecks(3000), testRule(Kopecks(5000)))))
    },
    testM("return Stop if there is a previous price, and stop flag is set") {
      val protoEntry = testRequestEntry(previousPrice = Some(Money(3000)), Patch.StopRule(true))
      val parsed = parseProtoEntry(protoEntry)
      assertM(parsed)(equalTo(Stop(Kopecks(3000), testKey, testFrom)))
    },
    testM("throw if stop flag is set to false") {
      val protoEntry = testRequestEntry(previousPrice = Some(Money(3000)), Patch.StopRule(false))
      val parsed = parseProtoEntry(protoEntry).run
      assertM(parsed)(failsWith[EmptyPatch])
    },
    testM("throw if patch is empty") {
      val protoEntry = testRequestEntry(previousPrice = Some(Money(3000)), Patch.Empty)
      val parsed = parseProtoEntry(protoEntry).run
      assertM(parsed)(failsWith[EmptyPatch])
    },
    testM("throw if context is empty") {
      val protoEntry =
        proto.PatchPricesRequestEntry(testMatrixId, context = None, previousPrice = None, Patch.NextPrice(Money(5000)))
      val parsed = parseProtoEntry(protoEntry).run
      assertM(parsed)(failsWith[EmptyRuleContext])
    }
  )

  private def parseProtoEntry(entry: proto.PatchPricesRequestEntry) =
    PatchPricesRequestEntry.parseProtoEntry(testSchema, testProject, testSource, testFrom, entry)

  private def testRequestEntry(previousPrice: Option[Money], patch: Patch) =
    proto.PatchPricesRequestEntry(testMatrixId, testRuleContext, previousPrice, patch)

  private def testRule(kopecks: Kopecks) = {
    Rule(
      testKey,
      Interval(testFrom, to = None),
      kopecks,
      testSource
    )
  }
}
