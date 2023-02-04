package ru.auto.api.managers

import ru.auto.api.auth.Application
import ru.auto.api.model.{AnonymousUser, AutoruDealer, AutoruUser, RequestParams, UserLocation, UserRef}
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.passport.model.api.ApiModel.SessionResult
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus
import ru.yandex.vertis.tracing.Traced

/**
  *
  * @author zvez
  */
trait TestRequest {

  implicit def request: Request = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.swagger)
    r.setUser(UserRef.user(1))
    r
  }

  def dealerRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", userLocation = Some(UserLocation(30f, 40f, 100f))))
    r.setApplication(Application.iosApp)
    r.setDealer(AutoruDealer(123))
    r.setUser(AutoruUser(123))
    r.setTrace(Traced.empty)
    r
  }

  def userRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(AutoruUser(123))
    r.setTrace(Traced.empty)
    r
  }

  def resellerRequest(userModerationStatus: UserModerationStatus): Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", userLocation = Some(UserLocation(30f, 40f, 100f))))
    r.setApplication(Application.iosApp)
    r.setUser(AutoruUser(123))
    r.setTrace(Traced.empty)
    val sessionBuilder = SessionResult.newBuilder()
    sessionBuilder.getUserBuilder.setModerationStatus(userModerationStatus)
    r.setSession(sessionBuilder.build())
    r
  }

  def anonymousRequest: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(AnonymousUser("123"))
    r.setTrace(Traced.empty)
    r
  }
}
