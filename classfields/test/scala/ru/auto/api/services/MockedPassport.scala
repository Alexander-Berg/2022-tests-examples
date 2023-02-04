package ru.auto.api.services

import ru.auto.api.ApiSuite
import ru.auto.api.managers.passport.PassportManager

/**
  * Provides mocked PassportManager with mocked by default getClient method
  *
  * @author zvez
  */
trait MockedPassport extends ApiSuite {

  override lazy val passportManager = mock[PassportManager]

}
