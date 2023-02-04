package ru.yandex.vertis.telepony.service.impl

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.CallerIdUsageDao
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.{Phone, RedirectKey, TouchRedirectRequest}
import ru.yandex.vertis.telepony.service.{OneTimeRedirectService, RedirectServiceV2}
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext, Threads}

import scala.concurrent.Future

class OneTimeRedirectServiceSpec extends SpecBase with MockitoSupport with ScalaCheckPropertyChecks {

  import Threads.lightWeightTasksEc

  trait Test {
    val rs = mock[RedirectServiceV2]
    val cd = mock[CallerIdUsageDao]
    val trj = mock[WriteJournal[TouchRedirectRequest]]
    val service = new OneTimeRedirectService(rs, cd, trj)

    implicit val rc: RequestContext = AutomatedContext("test")
  }

  private val BelarusianPhone = Phone("+375291542220")

  "OneTimeRedirectService" should {
    // TODO: disignore after experiment
    "create first" ignore new Test {
      val req = OneTimeRedirectCreateRequestGen.next
      val proxy = PhoneGen.next
      val redirect = ActualRedirectGen.next
      when(rs.get(?[RedirectKey])(?)).thenReturn(Future.successful(Nil))
      when(cd.find(?)).thenReturn(Future.successful(List(proxy)))
      when(cd.markUsed(equ(req.usageId), equ(proxy))).thenReturn(Future.unit)
      when(rs.getOrCreate(?)(?)).thenReturn(Future.successful(redirect))
      val oneTimeRedirect = service.create(req).futureValue
      oneTimeRedirect.proxy should ===(redirect.source.number)
      oneTimeRedirect.redirect should ===(redirect.asHistoryRedirect)
    }

    "not create if already exists" in new Test {
      val req = OneTimeRedirectCreateRequestGen.next
      val redirect = ActualRedirectGen.next
      when(rs.get(?[RedirectKey])(?)).thenReturn(Future.successful(List(redirect)))
      when(trj.send(?)).thenReturn(Future.successful(null))
      val oneTimeRedirect = service.create(req).futureValue
      oneTimeRedirect.proxy should ===(redirect.source.number)
      oneTimeRedirect.redirect should ===(redirect.asHistoryRedirect)
    }
    "fail create if target is not Russian number" in new Test {
      val req = OneTimeRedirectCreateRequestGen.map { r =>
        r.copy(key = r.key.copy(target = BelarusianPhone))
      }.next
      val f = service.create(req).failed.futureValue
      f shouldBe a[IllegalArgumentException]
    }
  }

}
