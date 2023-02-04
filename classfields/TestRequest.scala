package ru.yandex.realty.request

import ru.yandex.realty.auth.{Application, AuthInfo}
import ru.yandex.realty.model.user._

/**
  * Factory of [[Request]] instances for testing purposes.
  *
  * @author dimas
  */
object TestRequest {

  def fromUser(ref: UserRef): Request = {
    ref match {
      case AppUser(uuid) => fromApp(uuid)
      case PassportUser(uid) => fromPassport(uid)
      case WebUser(yandexUid) => fromWeb(yandexUid)
      case NoUser => fromAuthInfo(AuthInfo())
    }
  }

  def fromApp(uuid: String): Request = {
    fromAuthInfo(AuthInfo(uuid = Some(uuid)))
  }

  def fromWeb(yandexUid: String): Request = {
    fromAuthInfo(AuthInfo(yandexUid = Some(yandexUid)))
  }

  def fromPassport(uid: Long): Request = {
    fromAuthInfo(AuthInfo(uidOpt = Some(uid.toString)))
  }

  private def fromAuthInfo(auth: AuthInfo): Request = {
    val result = new RequestImpl
    result.setApplication(Application.UnitTests)
    result.setAuthInfo(auth)
    result
  }
}
