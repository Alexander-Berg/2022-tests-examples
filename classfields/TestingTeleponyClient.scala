package ru.yandex.realty.telepony

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, OFormat}
import ru.yandex.common.monitoring.error.{ErrorReservoirs, ExpiringWarningErrorPercentileReservoir}
import ru.yandex.common.monitoring.{Described, HealthChecks}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.context.v2.bunker.BunkerStorage
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.model.phone.{PhoneRedirect, TeleponyInfo}
import ru.yandex.realty.monitoring.ReservoirUtil.withReservoir
import ru.yandex.realty.tracing.{TeleponyTracing, Traced}
import ru.yandex.vertis.telepony.model.proto.{
  CallbackOrder,
  CallbackOrderCreateRequest,
  NumbersCountersRequest,
  NumbersCountersResponse
}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class TestingTeleponyClient(teleponyClient: TeleponyClient, bunkerProvider: Provider[BunkerStorage])
  extends TeleponyClient
  with Logging {

  import TestingTeleponyClient._

  private def loadPhoneNumbers(
    bunkerStorage: BunkerStorage,
    path: String,
    reservoir: ExpiringWarningErrorPercentileReservoir
  ): Seq[TestingPhoneNumber] =
    withReservoir[Seq[TestingPhoneNumber]](reservoir)(bunkerStorage.get[Seq[TestingPhoneNumber]](path)).getOrElse {
      log.error(s"Error while parsing testing phone numbers on bunker path: $path")
      Seq[TestingPhoneNumber]()
    }

  private def testingPhoneNumbers: Seq[TestingPhoneNumber] = {
    val bunkerStorage = bunkerProvider.get
    loadPhoneNumbers(bunkerStorage, PathPrefix + TestingPhoneNumbers, phoneNumbersParsingReservoir)
  }

  override def getOrCreate(teleponyInfo: TeleponyInfo, antifraud: Option[TeleponyClient.Antifraud])(
    implicit trace: Traced
  ): Future[PhoneRedirect] = {
    if (testingPhoneNumbers.exists(_.phone == teleponyInfo.target)) {
      teleponyClient.getOrCreate(teleponyInfo, antifraud)
    } else {
      TeleponyTracing.tagTelepony(trace, teleponyInfo)
      import teleponyInfo._
      Future.successful(
        PhoneRedirect(
          domain = domain,
          id = target,
          objectId,
          tag,
          DateTime.now,
          None,
          source = target,
          target,
          phoneType = phoneType,
          geoId = geoId,
          ttl = ttl
        )
      )
    }
  }

  override def findCalls(
    domain: String,
    from: Option[DateTime],
    to: Option[DateTime],
    objectIdPrefix: Option[TeleponyObjectIdPrefix.Value],
    objectIdSuffix: Option[String],
    tagSubstring: Option[String],
    pageNum: Int,
    pageSize: Int
  )(implicit trace: Traced): Try[TeleponyClient.CallsResult] =
    teleponyClient.findCalls(domain, from, to, objectIdPrefix, objectIdSuffix, tagSubstring, pageNum, pageSize)

  override def findAvailableRedirects(domain: String, geoId: Option[Int], phone: Option[String])(
    implicit trace: Traced
  ): Try[Int] =
    teleponyClient.findAvailableRedirects(domain, geoId, phone)

  override def deleteRedirect(
    domain: String,
    objectId: String,
    redirectId: String,
    downtime: Option[FiniteDuration]
  )(implicit trace: Traced = Traced.empty): Future[Unit] =
    teleponyClient.deleteRedirect(domain, objectId, redirectId, downtime)

  override def createCallbackOrder(request: CallbackOrderCreateRequest, domain: String)(
    implicit trace: Traced
  ): Future[CallbackOrder] = {
    val allNumbersIsTesting =
      (testingPhoneNumbers.exists(_.phone == request.getTargetInfo.getNumber)
        && testingPhoneNumbers.exists(_.phone == request.getSourceInfo.getNumber))
    if (allNumbersIsTesting) {
      teleponyClient.createCallbackOrder(request, domain)
    } else {
      val testingRequest = request.toBuilder.setDryRun(true).build()
      teleponyClient.createCallbackOrder(testingRequest, domain)
    }

  }

  def updateRedirectOptions(
    redirectOptions: GetOrCreateRequest.RedirectOptions,
    domain: String,
    objectId: String,
    target: String,
    tag: Option[String]
  )(implicit trace: Traced): Try[JsValue] =
    teleponyClient.updateRedirectOptions(redirectOptions, domain, objectId, target, tag)

  override def obtainNumberPoolStats(request: NumbersCountersRequest): Future[NumbersCountersResponse] =
    teleponyClient.obtainNumberPoolStats(request)
}

object TestingTeleponyClient {
  private val PathPrefix = "/realty/telepony"

  private lazy val TestingPhoneNumbers = "/testing-phone-numbers"

  private val phoneNumbersParsingReservoir = new ExpiringWarningErrorPercentileReservoir(
    errorPercent = 1,
    windowSize = 100
  ) with Described {
    override def getGuideLine: String = "Errors while parsing testing phone numbers in TeleponyTestingInfoProvider"
  }

  ErrorReservoirs.register(
    "telepony-testing-phone-numbers-parse",
    phoneNumbersParsingReservoir,
    HealthChecks.compoundRegistry()
  )
}

case class TestingPhoneNumber(phone: String, description: Option[String])

object TestingPhoneNumber {
  implicit val TestingPhoneNumberReads: OFormat[TestingPhoneNumber] = Json.format[TestingPhoneNumber]
}
