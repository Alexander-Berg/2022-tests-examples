package ru.yandex.realty.componenttest.data.offers.common

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName
import ru.yandex.realty.model.offer.Offer

abstract class ComponentTestOffer extends Offer {

  setId(extractIdFromClassName(getClass))

}
