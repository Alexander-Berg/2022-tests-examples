package ru.yandex.complaints.dao.complaints.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.complaints.dao.MySqlSpecTemplate
import ru.yandex.complaints.dao.complaints.OffersDaoSpec
import ru.yandex.complaints.dao.offers.{OffersDao, OffersDaoImpl}

/**
  * Runnable spec for [[OffersDaoSpec]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class MysqlOffersDaoSpecSpec
  extends OffersDaoSpec
  with MySqlSpecTemplate {
  
  override def offersDao: OffersDao = new OffersDaoImpl(mySql)
}
