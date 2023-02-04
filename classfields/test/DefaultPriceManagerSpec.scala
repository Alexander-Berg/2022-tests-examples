package ru.yandex.vertis.billing.howmuch.logic

import billing.common_model.{Money, Project}
import billing.howmuch.model.{testkit, RuleContext}
import billing.howmuch.model.Source.Source.{StartrekTicket, UserRequest}
import billing.howmuch.model.testkit.mkRuleContext
import billing.howmuch.price_service.PatchPricesRequest.From.FromNow
import billing.howmuch.price_service.{PatchPricesRequest, PatchPricesRequestEntry}
import billing.howmuch.price_service.PatchPricesRequestEntry.Patch.NextPrice
import cats.data.NonEmptyList
import common.time.Interval
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core.{Context, MatrixId, Rule, RuleKey, Schema, Source}
import ru.yandex.vertis.billing.howmuch.model.get.EntryId
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.environment.TestEnvironment
import zio.test.TestAspect.{before, ignore, nonFlaky, sequential}
import ru.yandex.vertis.billing.howmuch.model.get._
import ru.yandex.vertis.billing.howmuch.storage.RuleLogDao
import cats.data.NonEmptyMap
import common.zio.ops.tracing.RequestId.RequestId
import common.zio.ops.tracing.testkit.TestTracing
import common.zio.ops.tracing.RequestId
import common.zio.ydb.Ydb
import ru.yandex.vertis.billing.howmuch.logic.PriceManager.PriceManager
import ru.yandex.vertis.billing.howmuch.storage.ydb.{YdbActualRuleDao, YdbRuleLogDao}
import zio.{Has, ZLayer}
import zio.clock.Clock
import zio.test._
import zio.magic._
import common.zio.tagging.syntax._
import common.zio.ydb.Ydb.HasTxRunner
import ru.yandex.vertis.billing.howmuch.logic.PriceStore.PriceStore
import ru.yandex.vertis.billing.howmuch.storage.RuleLogDao.RuleLogDao
import billing.howmuch.{model => proto_model, price_service => proto}
import com.google.protobuf.Timestamp
import common.scalapb.ScalaProtobuf
import common.scalapb.ScalaProtobuf.instantToTimestamp

import java.time.Instant
import java.time.temporal.ChronoUnit.MICROS

object DefaultPriceManagerSpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testMatrixId = "call"
  private def testKey(context: Context) = RuleKey(MatrixId(testProject, testMatrixId), context)
  // чтобы случайно не попасть на TTL записей по actual_rule.to в YDB
  // YDB обрубает timestamp до микросекунд, поэтому здесь тоже обрубаем, чтобы тесты не падали из-за различия в мс
  private val now = Instant.now().truncatedTo(MICROS)
  private val testFrom = now.plusSeconds(100500)
  private val protoFrom = instantToTimestamp(testFrom)
  private val testSource = Source.StartrekTicket("VSMONEY-2750")

  private def testRule(context: String, price: Kopecks) = {
    Rule(
      testKey(Context.fromString(context)),
      Interval(testFrom, to = None),
      price,
      testSource
    )
  }

  val testSize = 1300

  override def spec: ZSpec[TestEnvironment, Any] =
    (suite("DefaultPriceManagerSpec")(
      testM("return more than 1000 rules if they exist (ydb limit for one query)") {
        val rules = (1 to testSize).map(i => testRule(s"region_id=0&mark=AUDI&model=$i", Kopecks(i * 100))).toVector
        val requestEntries = (1 to testSize).map(i =>
          proto.GetPricesRequestEntry(
            entryId = i.toString,
            matrixId = testMatrixId,
            context = Some(
              proto_model.RequestContext(
                criteriaList = List(
                  proto_model.RequestCriteria(
                    key = "region_id",
                    value = "0"
                  ),
                  proto_model.RequestCriteria(
                    key = "mark",
                    value = "AUDI"
                  ),
                  proto_model.RequestCriteria(
                    key = "model",
                    value = i.toString
                  )
                )
              )
            )
          )
        )
        val request = proto.GetPricesRequest(
          billing.common_model.Project.AUTORU,
          entries = requestEntries,
          timestamp = Some(protoFrom)
        )
        val expectedEntries = (1 to testSize).map(i => EntryId(i.toString) -> Some(rules(i - 1)))
        val expected = GetPricesResponse(NonEmptyMap.of(expectedEntries.head, expectedEntries.tail: _*))
        for {
          _ <- runTx(RuleLogDao.upsert(NonEmptyList.fromListUnsafe(rules.toList)))
          actual <-
            PriceManager.getPrices(request)
        } yield {
          assertTrue(actual == expected)
        }
      },
      testM("succeed when PatchPrices is invoked twice with the same rule") {
        val criteriaList = List("region_id" -> "1", "mark" -> "AUDI", "model" -> "TT")
        val patchEntry = PatchPricesRequestEntry(
          matrixId = "call",
          Some(mkRuleContext(criteriaList: _*)),
          patch = NextPrice(Money(kopecks = 500))
        )
        val patchRequest =
          PatchPricesRequest(
            Project.AUTORU,
            FromNow(true),
            Seq(patchEntry),
            Some(proto_model.Source(StartrekTicket("VSBILLING-1")))
          )
        val getEntry =
          proto.GetPricesRequestEntry("entryId", "call", Some(testkit.mkRequestContext(criteriaList: _*)))
        val getRequest = proto.GetPricesRequest(
          Project.AUTORU,
          Seq(getEntry),
          // На всякий случай таймстемп в будущем, во избежание race condition в тесте
          Some(ScalaProtobuf.instantToTimestamp(Instant.now().plusSeconds(60)))
        )
        for {
          _ <- PriceManager.patchPrices(patchRequest)
          _ <- PriceManager.patchPrices(patchRequest)
          result <- PriceManager.getPrices(getRequest)
        } yield assertTrue(
          result.entries.size == 1 && result.entries.head.entryId == "entryId" &&
            result.entries.head.result.rule.flatMap(_.price).map(_.kopecks).get == 500L
        )
      }
    ) @@ sequential @@
      before(TestYdb.clean("rule_log") *> TestYdb.clean("actual_rule"))).provideCustomLayerShared {
      ZLayer.wireSome[
        TestEnvironment,
        Ydb.Ydb with PriceStore with Clock with HasTxRunner with RuleLogDao with PriceManager
      ](
        TestTracing.noOp,
        RequestId.live,
        TestYdb.ydb,
        YdbRuleLogDao.live,
        YdbActualRuleDao.live,
        PriceStore.live,
        Ydb.txRunner,
        // передаём реальный Clock вместо тестового, чтобы случайно не попасть на TTL записей по actual_rule.to в YDB
        Clock.live.tagged[PriceManager.Service],
        Schema.live,
        PriceManager.live.requireTagged[Clock.Service, PriceManager.Service, Has[
          Schema
        ] with PriceStore with HasTxRunner with RequestId]
      ) ++ Clock.live // А здесь используем Clock, чтобы корректно работал traced. Иначе тест считает что время не прошло.
    }
}
