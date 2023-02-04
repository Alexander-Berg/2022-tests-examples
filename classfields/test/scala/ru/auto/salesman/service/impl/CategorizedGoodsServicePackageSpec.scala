package ru.auto.salesman.service.impl

import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiOfferModel.Category.TRUCKS
import ru.auto.salesman.model.AdsRequestType
import ru.auto.salesman.model.AdsRequestTypes.Commercial

class CategorizedGoodsServicePackageSpec extends GoodsServicePackageSpec {

  override protected def category: Category =
    TRUCKS

  override protected def adsRequestType: AdsRequestType =
    Commercial
}
