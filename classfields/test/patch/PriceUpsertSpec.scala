package ru.yandex.vertis.billing.howmuch.model.patch

import billing.common_model.Project
import common.time.Interval
import common.zio.testkit._
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.error.Conflict._
import ru.yandex.vertis.billing.howmuch.model.error.Defect._
import ru.yandex.vertis.billing.howmuch.model.patch.PatchPricesRequestEntry._
import zio.Exit
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object PriceUpsertSpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testMatrixId = "call"
  private val testKey = RuleKey(MatrixId(testProject, testMatrixId), Context.fromString("mark=AUDI&model=TT"))
  private val beforeTestFrom = Instant.parse("2021-04-29T21:00:00Z")
  private val testFrom = Instant.parse("2021-04-30T21:00:00Z")
  private val afterTestFrom = Instant.parse("2021-07-30T21:00:00Z")
  private val testSource = Source.StartrekTicket("VSMONEY-2750")

  private def mkRule(interval: Interval, price: Kopecks) =
    Rule(testKey, interval, price, testSource)

  private val testRule = mkRule(Interval(testFrom, to = None), Kopecks(6000))

  override def spec: ZSpec[TestEnvironment, Any] = suite("PriceUpsertSpec")(
    testM("create rule if not exists") {
      val expected = PriceUpsert(testRule)
      val actual = PriceUpsert(existing = None, Create(testRule))
      assertM(actual)(equalTo(Some(expected)))
    },
    testM("do nothing if rule already exists, but Create request received") {
      val effect = PriceUpsert(existing = Some(testRule), Create(testRule)).run
      assertM(effect)(equalTo(Exit.Success(None)))
    },
    testM(
      "update rule if key matches & patch.from is after existing.from & previous price matches & existing.to is empty"
    ) {
      val existing = mkRule(Interval(beforeTestFrom, to = None), Kopecks(1000))
      val patch = Update(previous = Kopecks(1000), mkRule(Interval(testFrom, to = None), Kopecks(2000)))
      val expected = PriceUpsert(
        mkRule(Interval(beforeTestFrom, Some(testFrom)), Kopecks(1000)),
        mkRule(Interval(testFrom, to = None), Kopecks(2000))
      )
      val actual = PriceUpsert(Some(existing), patch)
      assertM(actual)(equalTo(Some(expected)))
    },
    testM("fail if existing.price != update.previous") {
      val existing = mkRule(Interval(beforeTestFrom, to = None), Kopecks(1500))
      val patch = Update(previous = Kopecks(1000), mkRule(Interval(testFrom, to = None), Kopecks(2000)))
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(failsWith[DifferentPreviousPrice])
    },
    testM("fail if existing.from == update.from") {
      val existing = mkRule(Interval(testFrom, to = None), Kopecks(1000))
      val patch = Update(previous = Kopecks(1000), mkRule(Interval(testFrom, to = None), Kopecks(2000)))
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(failsWith[AlreadyExistsFrom])
    },
    testM("die if existing.from > update.from") {
      val existing = mkRule(Interval(afterTestFrom, to = None), Kopecks(1000))
      val patch = Update(previous = Kopecks(1000), mkRule(Interval(testFrom, to = None), Kopecks(2000)))
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(diesWith[InactiveRuleUpdate])
    },
    // возможно, в будущем будем разрешать это; но пока для простоты запрещаем
    testM("fail if try to update existing with non-empty to") {
      val existing = mkRule(Interval(beforeTestFrom, to = Some(afterTestFrom)), Kopecks(1000))
      val patch = Update(previous = Kopecks(1000), mkRule(Interval(testFrom, to = None), Kopecks(2000)))
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(failsWith[PatchRuleWithTo])
    },
    testM("fail if rule doesn't exist, but update requested") {
      val patch = Update(previous = Kopecks(1000), mkRule(Interval(testFrom, to = None), Kopecks(2000)))
      val effect = PriceUpsert(existing = None, patch).run
      assertM(effect)(failsWith[PatchNonExistingRule])
    },
    testM("die if existing rule and update don't relate to the same key") {
      val existing = mkRule(Interval(beforeTestFrom, to = None), Kopecks(1000))
        .copy(key = testKey.copy(context = Context.fromString("mark=AUDI&model=Q7")))
      val patch = Update(
        previous = Kopecks(1000),
        mkRule(Interval(testFrom, to = None), Kopecks(2000))
          .copy(key = testKey.copy(context = Context.fromString("mark=AUDI&model=A3")))
      )
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(diesWith[WrongKeyPatch])
    },
    // возможно, в будущем будем разрешать это; но пока для простоты запрещаем
    testM("die if requested update with non-empty to") {
      val existing = mkRule(Interval(beforeTestFrom, to = None), Kopecks(1000))
      val patch = Update(previous = Kopecks(1000), mkRule(Interval(testFrom, to = Some(afterTestFrom)), Kopecks(2000)))
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(diesWith[UpdateWithTo])
    },
    testM(
      "stop rule if key matches & patch.from is after existing.from & previous price matches & existing.to is empty"
    ) {
      val existing = mkRule(Interval(beforeTestFrom, to = None), Kopecks(1000))
      val patch = Stop(previous = Kopecks(1000), testKey, testFrom)
      val expected = PriceUpsert(
        mkRule(Interval(beforeTestFrom, Some(testFrom)), Kopecks(1000))
      )
      val actual = PriceUpsert(Some(existing), patch)
      assertM(actual)(equalTo(Some(expected)))
    },
    testM("fail if existing.price != stop.previous") {
      val existing = mkRule(Interval(beforeTestFrom, to = None), Kopecks(1500))
      val patch = Stop(previous = Kopecks(1000), testKey, testFrom)
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(failsWith[DifferentPreviousPrice])
    },
    testM("fail if existing.from == stop.from") {
      val existing = mkRule(Interval(testFrom, to = None), Kopecks(1000))
      val patch = Stop(previous = Kopecks(1000), testKey, testFrom)
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(failsWith[AlreadyExistsFrom])
    },
    testM("die if existing.from > stop.from") {
      val existing = mkRule(Interval(afterTestFrom, to = None), Kopecks(1000))
      val patch = Stop(previous = Kopecks(1000), testKey, testFrom)
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(diesWith[InactiveRuleUpdate])
    },
    // возможно, в будущем будем разрешать это; но пока для простоты запрещаем
    testM("fail if try to stop existing with non-empty to") {
      val existing = mkRule(Interval(beforeTestFrom, to = Some(afterTestFrom)), Kopecks(1000))
      val patch = Stop(previous = Kopecks(1000), testKey, testFrom)
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(failsWith[PatchRuleWithTo])
    },
    testM("fail if rule doesn't exist, but stop requested") {
      val patch = Stop(previous = Kopecks(1000), testKey, testFrom)
      val effect = PriceUpsert(existing = None, patch).run
      assertM(effect)(failsWith[PatchNonExistingRule])
    },
    testM("die if existing rule and stop don't relate to the same key") {
      val existing = mkRule(Interval(beforeTestFrom, to = None), Kopecks(1000))
        .copy(key = testKey.copy(context = Context.fromString("mark=AUDI&model=Q7")))
      val patch = Stop(
        previous = Kopecks(1000),
        testKey.copy(context = Context.fromString("mark=AUDI&model=A3")),
        testFrom
      )
      val effect = PriceUpsert(Some(existing), patch).run
      assertM(effect)(diesWith[WrongKeyPatch])
    }
  )
}
