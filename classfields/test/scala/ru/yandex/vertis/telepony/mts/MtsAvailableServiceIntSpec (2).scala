package ru.yandex.vertis.telepony.mts

import ru.yandex.vertis.telepony.SampleHelper._
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.mts.{CallDetailRecord, UniversalNumber}
import ru.yandex.vertis.telepony.model.{CallRecord, Operators, Phone}
import ru.yandex.vertis.telepony.mts.MtsAvailableServiceIntSpec.{AlwaysFailMtsClient, AlwaysSuccessMtsClient}
import ru.yandex.vertis.telepony.service.MtsClient._
import ru.yandex.vertis.telepony.service.impl.OperatorAvailableServiceImpl
import ru.yandex.vertis.telepony.service.impl.OperatorAvailableServiceImpl._
import ru.yandex.vertis.telepony.service.mts.{InMemoryMtsClient, RegisteringFailureMtsClient}
import ru.yandex.vertis.telepony.service.{MtsClient, OperatorAvailableService}
import ru.yandex.vertis.telepony.util.RequestContext
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.concurrent.Future
import scala.util.{Random, Try}

/**
  * @author @logab
  */
class MtsAvailableServiceIntSpec extends SpecBase with IntegrationSpecTemplate {

  val phone = Generator.PhoneGen

  def request(phone: Phone): UpdateRequest =
    UpdateRequest(phone, sayAni = Some(false), description = Some("Updated description " + Random.nextInt()), crm = None)

  private def testDomain = "test-" + Random.nextLong().toString

  "mts available service" should {

    "not register a failure" in {
      val mtsAvailable = new OperatorAvailableServiceImpl(
        hydraClient,
        failureLimit = 0.1,
        DefaultThreshold,
        Component,
        testDomain,
        Operators.Mts
      )

      val mtsClient = new AlwaysSuccessMtsClient with RegisteringFailureMtsClient {
        override def mtsAvailableService: OperatorAvailableService = mtsAvailable
      }
      phone.next(100).foreach(updateUniversalNumber(_, mtsClient))
      mtsAvailable.isAvailable.futureValue shouldEqual true

    }

    "register a failure" in {
      val threshold = 20
      val mtsAvailable = new OperatorAvailableServiceImpl(
        hydraClient,
        0.1,
        threshold,
        OperatorAvailableServiceImpl.Component,
        testDomain,
        Operators.Mts
      )
      val brokenClient = new AlwaysFailMtsClient with RegisteringFailureMtsClient {
        override def mtsAvailableService: OperatorAvailableService = mtsAvailable
      }
      phone.next(2 * threshold).foreach { p =>
        Try { updateUniversalNumber(p, brokenClient) }
      }
      eventually {
        mtsAvailable.isAvailable.futureValue shouldEqual false
      }
    }

    "stay available on first errors" in {
      val threshold = 10
      val mtsAvailable = new OperatorAvailableServiceImpl(
        hydraClient = hydraClient,
        domain = testDomain,
        operator = Operators.Mts,
        threshold = threshold
      )

      val brokenClient = new AlwaysFailMtsClient with RegisteringFailureMtsClient {
        override def mtsAvailableService: OperatorAvailableService = mtsAvailable
      }
      phone.next(threshold / 2).foreach(p => Try(updateUniversalNumber(p, brokenClient)))
      mtsAvailable.isAvailable.futureValue shouldEqual true
    }

    "become available when ok surpass failure limit" in {
      val failureLimit = 0.1
      val threshold = 5
      val mtsAvailable = new OperatorAvailableServiceImpl(
        hydraClient = hydraClient,
        failureLimit = failureLimit,
        threshold = threshold,
        domain = testDomain,
        operator = Operators.Mts
      )

      val mtsClient = new AlwaysSuccessMtsClient with RegisteringFailureMtsClient {
        override def mtsAvailableService: OperatorAvailableService = mtsAvailable
      }

      val brokenClient = new AlwaysFailMtsClient with RegisteringFailureMtsClient {
        override def mtsAvailableService: OperatorAvailableService = mtsAvailable
      }

      val failureRequests = threshold * 2

      phone.next(failureRequests).foreach(p => Try(updateUniversalNumber(p, brokenClient)))
      eventually {
        mtsAvailable.isAvailable.futureValue shouldEqual false
      }

      phone
        .next((failureRequests / failureLimit * 2).toInt)
        .foreach(updateUniversalNumber(_, mtsClient))
      eventually {
        mtsAvailable.isAvailable.futureValue shouldEqual true
      }
    }

    "work with zero error tolerance" in {
      val mtsAvailable = new OperatorAvailableServiceImpl(
        hydraClient = hydraClient,
        failureLimit = 0,
        domain = testDomain,
        operator = Operators.Mts
      )
      val mtsClient = new AlwaysSuccessMtsClient with RegisteringFailureMtsClient {
        override def mtsAvailableService: OperatorAvailableService = mtsAvailable
      }

      mtsAvailable.isAvailable.futureValue shouldBe true

      phone.next(100).foreach(updateUniversalNumber(_, mtsClient))
      mtsAvailable.isAvailable.futureValue shouldEqual true
    }

  }

  def updateUniversalNumber(i: Phone, client: MtsClient): Unit = {
    val req = request(i)
    client.updateUniversalNumber(req).futureValue
  }
}

object MtsAvailableServiceIntSpec {
  val Failed = Future.failed(new scala.UnsupportedOperationException("Intentionally broken"))

  class AlwaysFailMtsClient extends MtsClient {

    override def getUniversalNumber(number: Phone)(implicit rc: RequestContext): Future[UniversalNumber] =
      Failed

    override def updateUniversalNumber(request: UpdateRequest)(implicit rc: RequestContext): Future[Unit] =
      Failed

    override def getUniversalNumbers()(implicit rc: RequestContext): Future[Set[Phone]] =
      Failed

    override def getSounds(phone: Phone)(implicit rc: RequestContext): Future[Seq[Sound]] =
      Failed

    override def getCallDetailRecords(
        request: CdrRequest
      )(implicit rc: RequestContext): Future[Iterable[CallDetailRecord]] =
      Failed

    override def deleteSound(sound: Sound)(implicit rc: RequestContext): Future[Unit] =
      Failed

    override def getMenu(phone: Phone)(implicit rc: RequestContext): Future[Menu] =
      Failed

    override def putMenuSound(request: Sound.UpdateRequest)(implicit rc: RequestContext): Future[Sound] =
      Failed

    override def putSound(request: Sound.UpdateRequest)(implicit rc: RequestContext): Future[Sound] =
      Failed

    override def getCallRecord(url: String)(implicit rc: RequestContext): Future[Option[CallRecord]] =
      Failed

    override def updateMenu(menu: Menu)(implicit rc: RequestContext): Future[Unit] =
      Failed

    override def setIvr(
        phone: Phone,
        filename: String,
        sayAni: Option[Boolean]
      )(implicit rc: RequestContext): Future[Unit] =
      Failed

    override def setMaster(request: SetMasterRequest): Future[Unit] =
      Failed

    override def deleteMaster(phone: Phone): Future[Unit] =
      Failed
  }

  class AlwaysSuccessMtsClient extends InMemoryMtsClient {

    override def getUniversalNumber(number: Phone)(implicit rc: RequestContext): Future[UniversalNumber] =
      Failed

    override def updateUniversalNumber(request: UpdateRequest)(implicit rc: RequestContext): Future[Unit] =
      Future.unit

    override def getUniversalNumbers()(implicit rc: RequestContext): Future[Set[Phone]] =
      Failed

    override def getSounds(phone: Phone)(implicit rc: RequestContext): Future[Seq[Sound]] =
      Failed

    override def getCallDetailRecords(
        request: CdrRequest
      )(implicit rc: RequestContext): Future[Iterable[CallDetailRecord]] =
      Failed

    override def deleteSound(sound: Sound)(implicit rc: RequestContext): Future[Unit] =
      Failed

    override def getMenu(phone: Phone)(implicit rc: RequestContext): Future[Menu] =
      Failed

    override def putMenuSound(request: Sound.UpdateRequest)(implicit rc: RequestContext): Future[Sound] =
      Failed

    override def putSound(request: Sound.UpdateRequest)(implicit rc: RequestContext): Future[Sound] =
      Failed

    override def getCallRecord(url: String)(implicit rc: RequestContext): Future[Option[CallRecord]] =
      Failed

    override def updateMenu(menu: Menu)(implicit rc: RequestContext): Future[Unit] =
      Failed
  }
}
