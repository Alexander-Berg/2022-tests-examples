package ru.yandex.vertis.billing.api

import ru.yandex.vertis.billing.model_core.User
import ru.yandex.vertis.billing.service.AccessDenyException

import scala.util.control.NoStackTrace

/**
  * @author ruslansd
  */
object Exceptions {

  def artificialAccessDenyException(user: User, message: String = "artificial") =
    new AccessDenyException(user, message) with NoStackTrace
}
