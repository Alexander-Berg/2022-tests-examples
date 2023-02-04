package ru.yandex.auto.vin.decoder.partners.event

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.broker.EventManager
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.Partner
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class PartnerEventClientTest extends AsyncFunSuite with MockitoSupport with BeforeAndAfterEach {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val eventManager = mock[EventManager]
  private val partnerEventClient = new DefaultPartnerEventManager(eventManager)

  private val partner = Partner.AUTOCODE
  private val reportType = Some("main")
  private val identifier = VinCode("JTEBU29J005087645")

  def request[T](f: => T): Future[T] = Future(f)

  override def beforeEach(): Unit = {
    reset(eventManager)
  }

  test("successful request") {
    partnerEventClient
      .captureOrder(partner, identifier, reportType) { event =>
        request {
          event.setOrderId("23423")
          ""
        }
      }
      .map { _ =>
        succeed
      }
  }

  test("unsuccessful request") {
    recoverToSucceededIf[IllegalArgumentException] {
      partnerEventClient.captureReport(partner, 23423, identifier, reportType) { _ =>
        request {
          throw new IllegalArgumentException()
        }
      }
    }
  }
}
