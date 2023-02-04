package ru.auto.tests.utils

import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor

trait Authorization {

  def adaptor: PublicApiAdaptor

  final def getSessionId(account: Account): String = {
    adaptor.login(account).getSession.getId
  }

}
