package ru.yandex.vertis.telepony.service

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectServiceV2.CreateRequest
import ru.yandex.vertis.telepony.service.mts.InMemoryMtsClient
import ru.yandex.vertis.telepony.util.Range

import scala.concurrent.duration._

/**
  * @author evans
  */
trait BlockedCallServiceSpec extends SpecBase with BeforeAndAfterEach {

  def clean(): Unit

  def operatorNumberServiceV2: OperatorNumberServiceV2

  def redirectServiceV2: RedirectServiceV2

  def blockedCallServiceV2: BlockedCallService

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

  private def prepareRedirect(phone: Phone, create: CreateRequest) = {
    val numberRequest = createNumberRequest(phone).copy(status = Some(Status.Ready(None)))
    operatorNumberServiceV2.create(numberRequest).futureValue
    defaultMtsClient.registerNewUniversalNumber(phone)
    redirectServiceV2.create(create).futureValue
  }

  private def sampleFor(phone: Phone, target: Phone, redirect: ActualRedirect): CallService.CreateRequest =
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
      val call = blockedCallServiceV2.create(callRequest).futureValue
      call.externalId shouldEqual callRequest.externalId
      call.redirect.id shouldEqual redirect.id
    }
    "fail if wrong redirect for call" in {
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
      val call = blockedCallServiceV2.create(callRequest).futureValue
      val res = blockedCallServiceV2.list(CallService.Filter.Empty, Range.Full).futureValue
      res.head.id shouldEqual call.id
    }
    "success list limit call" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
      val call = blockedCallServiceV2.create(callRequest).futureValue
      val res = blockedCallServiceV2.list(CallService.Filter.Empty, 1).futureValue
      res.head.id shouldEqual call.id
    }
    "handle bad request" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
        .copy(duration = 0.seconds)
      blockedCallServiceV2.create(callRequest).futureValue
    }
    "truncate source if it too long" in {
      val phone = MoscowPhoneGen.next
      val create = gen.next
      val longSource = Iterator.continually('a').take(50).mkString
      val callRequest = sampleFor(phone, create.key.target, prepareRedirect(phone, create))
        .copy(duration = 0.seconds, source = Some(RefinedSource.from(longSource)))
      val call = blockedCallServiceV2.create(callRequest).futureValue
      call.source.get shouldEqual RefinedSource.from(longSource.take(40))
    }
  }
}
