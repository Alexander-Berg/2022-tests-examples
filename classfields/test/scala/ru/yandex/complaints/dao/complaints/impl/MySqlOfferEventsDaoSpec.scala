package ru.yandex.complaints.dao.complaints.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.complaints.dao.MySqlSpecTemplate
import ru.yandex.complaints.dao.complaints.OfferEventsDaoSpec
import ru.yandex.complaints.dao.events.MySqlOfferEventsDao
import ru.yandex.complaints.dao.offers.{OffersDao, OffersDaoImpl}

@RunWith(classOf[JUnitRunner])
class MySqlOfferEventsDaoSpec
  extends OfferEventsDaoSpec
    with MySqlSpecTemplate {

  override val offersDao: OffersDao = new OffersDaoImpl(mySql)

  override val offerEventsDao = new MySqlOfferEventsDao(mySql)
}
