package ru.auto.tests.publicapi.testdata

import ru.auto.tests.passport.account.Account

case class DealerData(login: String, phone: String, clientId: String, userId: String) {
  def account = Account.builder().login(login).password("autoru").id(userId).build()
  def formattedPhone = phone.replaceAll(" ", "")
}

object WhitelistDealerAccounts {

  val addPhoneDealer = DealerData(
    login = "vavilon-auto013@mail.ru",
    phone = "+7 987 654 33 10",
    clientId = "9967",
    userId = "12642412"
  )

  val removeDealer = DealerData(
    login = "mariya.mazhirina@hyundai.inchcape.ru",
    phone = "+7 987 654 32 11",
    clientId = "41982",
    userId = "46262844"
  )

  val listDealer = DealerData(
    login = "larghin@bk.ru",
    phone = "+7 987 654 32 12",
    clientId = "1694",
    userId = "37560437"
  )

  val demo = DealerData(
    login = "demo@auto.ru",
    phone = "+7 987 654 32 13",
    clientId = "20101",
    userId = "11296277"
  )

  val phonesLeftDealer = DealerData(
    login = "9677725@alex-car.ru",
    phone = "+7 987 654 32 14",
    clientId = "104",
    userId = "741063"
  )
}
