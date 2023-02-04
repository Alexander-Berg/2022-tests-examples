package ru.auto.tests.publicapi.operations.dealer.whitelist

import io.qameta.allure.Step
import ru.auto.tests.passport.account.Account
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.api.DealerApi
import ru.auto.tests.publicapi.model.{AutoApiDealerSimplePhonesList, VertisPassportSession}
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

trait WhitelistOps {
  def api: ApiClient

  @Step("Добавляем телефоны в белый список")
  def addPhones(account: Account, phones: Seq[String])(
      implicit session: VertisPassportSession
  ): DealerApi.AddDealerPhonesToWhiteListOper = {
    val bodyPhones = new AutoApiDealerSimplePhonesList()
    phones.foreach(bodyPhones.addPhonesItem)

    api
      .dealer()
      .addDealerPhonesToWhiteList()
      .body(bodyPhones)
      .reqSpec(defaultSpec)
      .xUidHeader(account.getId)
      .xSessionIdHeader(session.getId)
  }

  @Step("Возвращаем текущие телефооны из белого списка")
  def listPhones(account: Account)(
      implicit session: VertisPassportSession
  ): DealerApi.GetDealerPhonesToWhiteListOper = {
    api
      .dealer()
      .getDealerPhonesToWhiteList
      .reqSpec(defaultSpec)
      .xUidHeader(account.getId)
      .xSessionIdHeader(session.getId)
  }

  @Step("Проверяем возможность добавлять телефоны в белый список")
  def checkAvailability(account: Account)(
      implicit session: VertisPassportSession
  ): DealerApi.AvailableDealerPhonesToWhiteListOper = {
    api
      .dealer()
      .availableDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xUidHeader(account.getId)
      .xSessionIdHeader(session.getId)
  }

  @Step("Убираем телефоны из белого списка дилера")
  def removePhones(account: Account, removePhones: Seq[String])(
      implicit session: VertisPassportSession
  ): DealerApi.DeleteDealerPhonesToWhiteListOper = {
    val bodyPhones = new AutoApiDealerSimplePhonesList()
    removePhones.foreach(bodyPhones.addPhonesItem)
    api
      .dealer()
      .deleteDealerPhonesToWhiteList()
      .body(bodyPhones)
      .reqSpec(defaultSpec)
      .xUidHeader(account.getId)
      .xSessionIdHeader(session.getId)
  }

  @Step("Проверяем, сколько еще телефонов можно добавить в белый список для этого дилера")
  def entriesLeft(account: Account)(
      implicit session: VertisPassportSession
  ): DealerApi.EntriesLeftDealerPhonesToWhiteListOper =
    api
      .dealer()
      .entriesLeftDealerPhonesToWhiteList()
      .reqSpec(defaultSpec)
      .xUidHeader(account.getId)
      .xSessionIdHeader(session.getId)

}
