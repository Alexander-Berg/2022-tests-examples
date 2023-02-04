package ru.yandex.vos2.services.phone

import ru.yandex.extdata.core.lego.Provider

trait TestingSmsPhoneWhitelistComponents {

  def testingSmsPhoneWhitelistProvider: Provider[TestingSmsPhoneWhitelist]
}
