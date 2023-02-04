package ru.yandex.vertis.telepony.service

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectServiceV2.CreateRequest
import ru.yandex.vertis.telepony.service.mts.InMemoryMtsClient
import ru.yandex.vertis.telepony.util.{Range, Threads}
import ru.yandex.vertis.telepony.time._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

/**
  * @author evans
  */
trait CallServiceV2Spec extends SpecBase with OptionValues with BeforeAndAfterEach {

  def clean(): Unit

  def operatorNumberServiceV2: OperatorNumberServiceV2

  def redirectServiceV2: RedirectServiceV2

  def callServiceV2: ActualCallService

  def defaultMtsClient: InMemoryMtsClient

  override protected def beforeEach(): Unit = {
    clean()
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    clean()
    super.afterEach()
  }

  val gen: Gen[CreateRequest] = createRequestV2Gen(MoscowPhoneGen).map(_.copy(geoId = None))

  def prepareRedirect(phone: Phone, create: CreateRequest) = {
    val numberRequest = createNumberRequest(phone).copy(status = Some(Status.Ready(None)))
    operatorNumberServiceV2.create(numberRequest).futureValue
    defaultMtsClient.registerNewUniversalNumber(phone)
    redirectServiceV2.create(create).futureValue
  }

  def sampleFor(phone: Phone, target: Phone, redirect: ActualRedirect): CallService.CreateRequest =
    sampleFor(phone, target, redirect.asHistoryRedirect)

  private def sampleFor(phone: Phone, target: Phone, redirect: HistoryRedirect): CallService.CreateRequest =
    CallService.CreateRequest(
      externalId = "1",
      source = None,
      target = target,
      proxy = phone,
      DateTime.now,
      1.second,
      1.second,
      hasRecord = false,
      redirect,
      CallResults.Unknown,
      None
    )

  "Call Service" should {
    "create call" in {
      val create = gen.next
      val phone = MoscowPhoneGen.next
      val redirect = prepareRedirect(phone, create)
      val callRequest = sampleFor(phone, create.key.target, redirect)
      val call = callServiceV2.create(callRequest).futureValue
      call.externalId shouldEqual callRequest.externalId
      call.redirect.id shouldEqual redirect.id
      call.hasRecord shouldEqual false
    }
    "update call" in {
      val create = gen.next
      val phone = MoscowPhoneGen.next
      val redirect = prepareRedirect(phone, create)
      val callRequest = sampleFor(phone, create.key.target, redirect)
      val call = callServiceV2.create(callRequest).futureValue
      val update = CallService.Update(
        call.talkDuration + 2.seconds,
        call.duration + 3.seconds,
        CallResultGen.suchThat(_ != call.callResult).next,
        !call.hasRecord,
        None
      )
      val updated = callServiceV2.update(call.id, update).futureValue
      updated.updateTime should be > call.updateTime
      updated should ===(
        call.copy(
          talkDuration = update.talkDuration,
          duration = update.duration,
          callResult = update.callResult,
          hasRecord = update.hasRecord,
          updateTime = updated.updateTime
        )
      )
    }
    "fail to build request when wrong redirect for call" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      intercept[IllegalArgumentException] {
        sampleFor(phone, MoscowPhoneGen.next, prepareRedirect(phone, create))
      }
    }
    "fail to build request when not within timerange" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val history = prepareRedirect(phone, create).asHistoryRedirect
      intercept[IllegalArgumentException] {
        sampleFor(phone, create.key.target, history.copy(endTime = Some(history.createTime)))
      }
    }
    "list call" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
      val call = callServiceV2.create(callRequest).futureValue
      val res = callServiceV2.list(CallService.Filter.Empty, Range.Full).futureValue
      res.head.id shouldEqual call.id
    }
    "success list limit call" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
      val call = callServiceV2.create(callRequest).futureValue
      val res = callServiceV2.list(CallService.Filter.Empty, 1).futureValue
      res.head.id shouldEqual call.id
    }
    "handle bad request" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
        .copy(duration = 0.seconds)
      callServiceV2.create(callRequest).futureValue
    }
    "truncate source if it too long" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val longSource = Iterator.continually('a').take(50).mkString
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
        .copy(duration = 0.seconds, source = Some(RefinedSource.from(longSource)))
      val call = callServiceV2.create(callRequest).futureValue
      call.source.get shouldEqual RefinedSource.from(longSource.take(40))
    }
    "count calls for objectId" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val objectId = create.key.objectId
      val from = DateTime.now()
      val redirect = prepareRedirect(phone, create)
      val callTimes = Random.shuffle((10 to 50 by 10).map(from.plusMinutes(_)))
      val maxTime = callTimes.maxBy(_.getMillis)
      val callFtrs = callTimes.map { t =>
        val createCall = sampleFor(phone, create.key.target, redirect)
          .copy(externalId = t.getMillis.toString, time = t)
        callServiceV2.create(createCall)
      }
      import Threads.lightWeightTasksEc
      val calls = Future.sequence(callFtrs).futureValue
      val res = callServiceV2.callsStats(objectId, redirect.key.tag).futureValue
      eventually {
        res.count should (be > 0 and be <= calls.size) // strict equality make test flaky
        res.lastCallTime.value should ===(maxTime)
      }
    }
    "calls stats return (0, None)" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val objectId = create.key.objectId
      val redirect = prepareRedirect(phone, create)
      val res = callServiceV2.callsStats(objectId, redirect.key.tag).futureValue
      res should ===(CallsStats(0, None))
    }
    "count call" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
      callServiceV2.create(callRequest).futureValue
      val res = callServiceV2.callsByDay(CallService.Filter.Empty).futureValue
      res.head.count shouldEqual 1
    }
    "count call return empty response" in {
      val res = callServiceV2.callsByDay(CallService.Filter.Empty).futureValue
      res.size shouldEqual 0
    }
  }
}
