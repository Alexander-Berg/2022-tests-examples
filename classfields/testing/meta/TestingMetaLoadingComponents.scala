package ru.yandex.vos2.testing.meta

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.application.components.BunkerComponents
import ru.yandex.realty.application.extdata.ExtdataControllerProvider
import ru.yandex.realty.application.extdata.providers.forceInit

trait TestingMetaLoadingComponents {
  def testingMetaLoadingUsersProvider: Provider[TestingMetaLoadingList]
}

trait TestingMetaLoadingComponentsImpl extends TestingMetaLoadingComponents {
  self: BunkerComponents with ExtdataControllerProvider =>
  override lazy val testingMetaLoadingUsersProvider: Provider[TestingMetaLoadingList] =
    new TestingMetaLoadingListProvider(controller, bunkerProvider)

  forceInit(testingMetaLoadingUsersProvider)
}
