package ru.yandex.realty.telepony

import org.scalamock.scalatest.MockFactory
import ru.yandex.realty.model.phone.{PhoneRedirect, PhoneType, TeleponyInfo}
import ru.yandex.realty.telepony.TeleponyClient.Antifraud
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Try}

trait TeleponyClientMockComponents extends MockFactory {

  val teleponyClient: TeleponyClient = mock[TeleponyClient]

  def expectTeleponyCallFailed(redirect: PhoneRedirect): Unit = {
    expectTeleponyCallFailed(
      tag = redirect.tag,
      phoneType = redirect.phoneType,
      exception = new Exception("telepony call failed"),
      geoId = redirect.geoId
    )
  }

  def expectTeleponyCallFailed(
    tag: Option[String],
    phoneType: Option[PhoneType],
    exception: Exception,
    geoId: Option[Int] = None
  ): Unit = {
    (teleponyClient
      .getOrCreate(
        _: TeleponyInfo,
        _: Option[Antifraud]
      )(_: Traced))
      .expects(where { (teleponyInfo: TeleponyInfo, _, _) =>
        teleponyInfo.geoId == geoId &&
        teleponyInfo.phoneType == phoneType &&
        teleponyInfo.tag == tag
      })
      .anyNumberOfTimes()
      .returning(Future.failed(exception))
  }

  def expectTeleponyCall(redirect: PhoneRedirect): Unit = {
    expectTeleponyCall(
      tag = redirect.tag,
      phoneType = redirect.phoneType,
      geoId = redirect.geoId,
      result = Success(redirect)
    )
  }

  def expectTeleponyCall(
    tag: Option[String],
    phoneType: Option[PhoneType],
    result: Try[PhoneRedirect],
    geoId: Option[Int] = None
  ): Unit = {
    (teleponyClient
      .getOrCreate(
        _: TeleponyInfo,
        _: Option[Antifraud]
      )(_: Traced))
      .expects(where { (teleponyInfo: TeleponyInfo, _, _) =>
        teleponyInfo.geoId == geoId &&
        teleponyInfo.phoneType == phoneType &&
        teleponyInfo.tag == tag
      })
      .anyNumberOfTimes()
      .returning(Future.fromTry(result))
  }

  def expectTeleponyDeleteCall(): Unit = {
    (teleponyClient
      .deleteRedirect(_: String, _: String, _: String, _: Option[FiniteDuration])(_: Traced))
      .expects(*, *, *, *, *)
      .anyNumberOfTimes()
      .returning(Future.unit)
  }

  def expectTeleponyDeleteCall(redirect: PhoneRedirect): Unit = {
    (teleponyClient
      .deleteRedirect(_: String, _: String, _: String, _: Option[FiniteDuration])(_: Traced))
      .expects(redirect.domain, redirect.objectId, redirect.id, *, *)
      .anyNumberOfTimes()
      .returning(Future.unit)
  }

}
