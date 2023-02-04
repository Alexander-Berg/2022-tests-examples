package ru.yandex.vertis.billing.howmuch.logic

import billing.common_model.Project
import cats.data.{NonEmptyList, NonEmptyMap}
import common.time.Interval
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.error.Conflict.PatchNonExistingRule
import ru.yandex.vertis.billing.howmuch.model.get._
import ru.yandex.vertis.billing.howmuch.model.patch.PatchPricesRequestEntry._
import ru.yandex.vertis.billing.howmuch.model.patch._
import ru.yandex.vertis.billing.howmuch.storage.ydb.{YdbActualRuleDao, YdbRuleLogDao}
import ru.yandex.vertis.billing.howmuch.storage.{ActualRuleDao, RuleLogDao}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{before, nonFlaky, sequential}
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant
import java.time.temporal.ChronoUnit.MICROS

object DefaultPriceStoreSpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testMatrixId = "call"
  private def testKey(context: Context) = RuleKey(MatrixId(testProject, testMatrixId), context)
  // чтобы случайно не попасть на TTL записей по actual_rule.to в YDB
  // YDB обрубает timestamp до микросекунд, поэтому здесь тоже обрубаем, чтобы тесты не падали из-за различия в мс
  private val now = Instant.now().truncatedTo(MICROS)
  private val testFrom = now.plusSeconds(100500)
  private val afterTestFrom = testFrom.plusSeconds(100500)
  private val testSource = Source.StartrekTicket("VSMONEY-2750")

  private def testRule(context: String, price: Kopecks) = {
    Rule(
      testKey(Context.fromString(context)),
      Interval(testFrom, to = None),
      price,
      testSource
    )
  }

  implicit class RichTestRule(private val rule: Rule) {

    def updated(from: Instant, price: Kopecks): Rule =
      rule.copy(interval = rule.interval.copy(from = from), price = price)
  }

  override def spec: ZSpec[TestEnvironment, Any] = (suite("DefaultPriceStoreSpec")(
    testM("update two different rules, and leave third as is") {
      val originalRule1 = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val originalRule2 = testRule("mark=AUDI&model=A3", Kopecks(3000))
      val originalRule3 = testRule("mark=AUDI&model=Q7", Kopecks(5000))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(originalRule1, originalRule2, originalRule3)))
        update1 =
          Update(previous = Kopecks(1000), next = originalRule1.updated(from = afterTestFrom, price = Kopecks(2000)))
        update2 =
          Update(previous = Kopecks(3000), next = originalRule2.updated(from = afterTestFrom, price = Kopecks(4000)))
        request = PatchPricesRequest(NonEmptyList.of(update1, update2), from = afterTestFrom)
        _ <- runTx(PriceStore.patchPrices(request))
        keys = NonEmptyList.of(originalRule1.key, originalRule2.key, originalRule3.key)
        updated <- runTx(RuleLogDao.select(keys, activeAt = testFrom))
        inserted <- runTx(RuleLogDao.select(keys, activeAt = afterTestFrom))
      } yield {
        assert(updated)(
          hasSameElements(List(originalRule1.withTo(afterTestFrom), originalRule2.withTo(afterTestFrom), originalRule3))
        ) &&
        assert(inserted)(
          hasSameElements(
            List(
              originalRule1.copy(interval = Interval(from = afterTestFrom, to = None), price = Kopecks(2000)),
              originalRule2.copy(interval = Interval(from = afterTestFrom, to = None), price = Kopecks(4000)),
              originalRule3
            )
          )
        )
      }
    },
    testM("fail on one invalid entry, and not insert another one") {
      val originalRule = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val nonExistingRule = testRule("mark=AUDI&model=A3", Kopecks(3000))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(originalRule)))
        validUpdate =
          Update(previous = Kopecks(1000), next = originalRule.updated(from = afterTestFrom, price = Kopecks(2000)))
        invalidUpdate =
          Update(previous = Kopecks(3000), next = nonExistingRule.updated(from = afterTestFrom, price = Kopecks(4000)))
        request = PatchPricesRequest(NonEmptyList.of(validUpdate, invalidUpdate), from = afterTestFrom)
        patchResult <- runTx(PriceStore.patchPrices(request)).run
        afterPatch <- runTx(RuleLogDao.select(NonEmptyList.of(originalRule.key), activeAt = testFrom))
      } yield {
        assert(patchResult)(fails(equalTo(PatchNonExistingRule(invalidUpdate)))) &&
        assert(afterPatch)(equalTo(List(originalRule)))
      }
    },
    testM("insert into actual_rule too") {
      val rule = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val request = PatchPricesRequest(NonEmptyList.of(Create(rule)), from = testFrom)
      for {
        _ <- runTx(PriceStore.patchPrices(request))
        afterPatch <- ActualRuleDao.select(activeAt = testFrom).runCollect
      } yield {
        assert(afterPatch)(hasSameElements(List(rule)))
      }
    },
    testM("return two rules if they exist") {
      val rule1 = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val rule2 = testRule("mark=AUDI&model=Q7", Kopecks(1000))
      val request = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("1") -> testEntry("mark=AUDI&model=TT"),
          EntryId("2") -> testEntry("mark=AUDI&model=Q7")
        ),
        testFrom
      )
      val expected = NonEmptyMap.of(EntryId("1") -> Some(rule1), EntryId("2") -> Some(rule2))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(rule1, rule2)))
        actual <- runTx(PriceStore.getPrices(request))
      } yield {
        assert(actual)(equalTo(expected))
      }
    },
    testM("return only 1 active rule (with latest from) for key if more that 1 exist") {
      val rule1 = testRule("mark=AUDI&model=TT", Kopecks(10000)).copy(interval =
        Interval(
          testFrom.minusSeconds(100),
          to = None
        )
      )
      val rule2 = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val request = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("2") -> testEntry("mark=AUDI&model=TT")
        ),
        testFrom
      )
      val expected = NonEmptyMap.of(EntryId("2") -> Some(rule2))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(rule1, rule2)))
        actual <- runTx(PriceStore.getPrices(request))
      } yield {
        assertTrue(actual == expected)
      }
    },
    testM("return None if rule doesn't exist") {
      val rule = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val request = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("1") -> testEntry("mark=AUDI&model=Q5", "mark=AUDI&model->*")
        ),
        testFrom
      )
      val expected = NonEmptyMap.of(EntryId("1") -> None)
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(rule)))
        actual <- runTx(PriceStore.getPrices(request))
      } yield {
        assert(actual)(equalTo(expected))
      }
    },
    testM("fallback to next context if haven't found rule by first context") {
      val rule = testRule("mark=AUDI&model->*", Kopecks(1000))
      val request = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("1") -> testEntry("mark=AUDI&model=Q7", "mark=AUDI&model->*")
        ),
        testFrom
      )
      val expected = NonEmptyMap.of(EntryId("1") -> Some(rule))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(rule)))
        actual <- runTx(PriceStore.getPrices(request))
      } yield {
        assert(actual)(equalTo(expected))
      }
    },
    testM("choose exact rule instead of fallback") {
      val exact = testRule("mark=AUDI&model=Q7", Kopecks(1000))
      val fallback = testRule("mark=AUDI&model->*", Kopecks(1000))
      val request = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("1") -> testEntry("mark=AUDI&model=Q7", "mark=AUDI&model->*")
        ),
        testFrom
      )
      val expected = NonEmptyMap.of(EntryId("1") -> Some(exact))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(exact, fallback)))
        actual <- runTx(PriceStore.getPrices(request))
      } yield {
        assert(actual)(equalTo(expected))
      }
    },
    testM("choose different fallbacks") {
      val rule1 = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val rule1Fallback = testRule("mark=AUDI&model->*", Kopecks(2000))
      val rule2 = testRule("mark=BMW&model=X5", Kopecks(3000))
      val rule2Fallback = testRule("mark=BMW&model->*", Kopecks(4000))
      val request = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("1") -> testEntry("mark=AUDI&model=Q7", "mark=AUDI&model->*"),
          EntryId("2") -> testEntry("mark=BMW&model=X3", "mark=BMW&model->*")
        ),
        testFrom
      )
      val expected = NonEmptyMap.of(EntryId("1") -> Some(rule1Fallback), EntryId("2") -> Some(rule2Fallback))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(rule1, rule1Fallback, rule2, rule2Fallback)))
        actual <- runTx(PriceStore.getPrices(request))
      } yield {
        assert(actual)(equalTo(expected))
      }
    },
    testM("choose same fallback") {
      val rule1 = testRule("mark=AUDI&model=TT", Kopecks(1000))
      val rule2 = testRule("mark=AUDI&model=Q7", Kopecks(2000))
      val fallback = testRule("mark=AUDI&model->*", Kopecks(3000))
      val request = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("1") -> testEntry("mark=AUDI&model=Q5", "mark=AUDI&model->*"),
          EntryId("2") -> testEntry("mark=AUDI&model=Q3", "mark=AUDI&model->*")
        ),
        testFrom
      )
      val expected = NonEmptyMap.of(EntryId("1") -> Some(fallback), EntryId("2") -> Some(fallback))
      for {
        _ <- runTx(RuleLogDao.upsert(NonEmptyList.of(rule1, rule2, fallback)))
        actual <- runTx(PriceStore.getPrices(request))
      } yield {
        assert(actual)(equalTo(expected))
      }
    }
  ) @@ sequential @@
    before(TestYdb.clean("rule_log") *> TestYdb.clean("actual_rule"))).provideCustomLayerShared {
    // передаём реальный Clock вместо тестового, чтобы случайно не попасть на TTL записей по actual_rule.to в YDB
    TestYdb.ydb >+> YdbRuleLogDao.live ++ YdbActualRuleDao.live >+> PriceStore.live ++ Ydb.txRunner ++ Clock.live
  }

  private def testEntry(context: String, otherContexts: String*) =
    GetPricesRequestEntry(
      MatrixId(testProject, testMatrixId),
      NonEmptyList.of(context, otherContexts: _*).map(Context.fromString)
    )
}
