package ru.yandex.complaints.dao.complaints.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.complaints.dao.MySqlSpecTemplate
import ru.yandex.complaints.dao.complaints.{ComplaintsDao, ComplaintsDaoImpl, ComplaintsDaoSpec}
import ru.yandex.complaints.dao.offers.{OffersDao, OffersDaoImpl}
import ru.yandex.complaints.dao.users.{UsersDao, UsersDaoImpl}

/**
  * Runnable spec for [[ComplaintsDaoSpec]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class MySqlComplaintsDaoSpec
  extends ComplaintsDaoSpec
  with MySqlSpecTemplate {
  
  override def complaintsDao: ComplaintsDao = new ComplaintsDaoImpl(mySql)
  override def usersDao: UsersDao = new UsersDaoImpl(mySql)
  override def offersDao: OffersDao = new OffersDaoImpl(mySql)
}
