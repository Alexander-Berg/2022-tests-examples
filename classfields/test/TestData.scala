package ru.yandex.vertis.moisha.impl.autoru_users.gens.test

import org.joda.time.DateTime
import ru.yandex.vertis.moisha.impl.autoru_users.AutoRuUsersPolicy.{
  AutoRuUsersPoint,
  AutoRuUsersRequest,
  AutoRuUsersResponse
}
import ru.yandex.vertis.moisha.impl.autoru_users.model._
import ru.yandex.vertis.moisha.model.{DateTimeInterval, Funds}

object TestData {

  def createReq(product: Products.Value): AutoRuUsersRequest = {
    val offer = AutoRuUsersOffer(
      Categories.Cars,
      Sections.New,
      mark = "",
      model = "",
      generation = None,
      year = 2020,
      geoId = Seq(),
      price = 1000000,
      creationTs = DateTime.now
    )

    val context = AutoRuUsersContext(
      offerType = OfferTypes.Regular,
      canAddFree = true,
      numByMark = None,
      numByModel = None,
      invalidVin = false,
      experiment = "",
      autoApply = false,
      UserTypes.Usual,
      paymentReason = None
    )

    AutoRuUsersRequest(
      product,
      offer: AutoRuUsersOffer,
      context: AutoRuUsersContext,
      DateTimeInterval(DateTime.now, DateTime.now.plusDays(1).minus(1))
    )
  }

  def createResponse(request: AutoRuUsersRequest, prolongPrice: Option[Funds]): AutoRuUsersResponse = {
    val autoruUserGood = AutoRuUsersGood(Goods.Custom, Costs.PerIndexing, price = 200, prolongPrice)
    val autoruUsersProduct =
      AutoRuUsersProduct(Products.Placement, duration = None, tariff = None, goods = Set(autoruUserGood))
    val point = AutoRuUsersPoint(
      "1",
      DateTimeInterval(DateTime.now, DateTime.now.plusDays(1)),
      autoruUsersProduct,
      experimentId = None
    )
    AutoRuUsersResponse(request, List(point))
  }
}
