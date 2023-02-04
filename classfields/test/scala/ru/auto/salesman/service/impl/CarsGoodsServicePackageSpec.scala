package ru.auto.salesman.service.impl

import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.salesman.model.AdsRequestType
import ru.auto.salesman.model.AdsRequestTypes.CarsUsed

class CarsGoodsServicePackageSpec extends GoodsServicePackageSpec {

  override protected def category: Category =
    CARS

  override protected def adsRequestType: AdsRequestType =
    CarsUsed
}
