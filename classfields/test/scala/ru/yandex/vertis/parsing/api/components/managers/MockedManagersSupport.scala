package ru.yandex.vertis.parsing.api.components.managers

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.api.managers.PhotoManager
import ru.yandex.vertis.parsing.api.managers.offers.OffersManager

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedManagersSupport extends ManagersAware with MockitoSupport {
  override val offersManager: OffersManager = mock[OffersManager]

  override val photoManager: PhotoManager = mock[PhotoManager]
}
