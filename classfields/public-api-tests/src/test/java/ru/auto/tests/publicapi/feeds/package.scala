package ru.auto.tests.publicapi

import ru.auto.tests.passport.account.Account

package object feeds {

  object Accounts {
    val TestAccount: Account = {
      Account.builder().login("feeds@regress.apiauto.ru").password("autoru").id("69998623").build()
    }

    val ReadOnlyTestAccount: Account = {
      Account.builder().login("feeds-readonly@regress.apiauto.ru").password("autoru").id("70003007").build()
    }

    val NoFeedsAclTestAccount: Account = {
      Account.builder().login("feeds-disabled@regress.apiauto.ru").password("autoru").id("70003008").build()
    }
  }

}
