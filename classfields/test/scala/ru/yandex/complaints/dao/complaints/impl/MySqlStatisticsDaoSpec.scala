package ru.yandex.complaints.dao.complaints.impl

import ru.yandex.complaints.dao.MySqlSpecTemplate
import ru.yandex.complaints.dao.complaints.{ComplaintsDao, ComplaintsDaoImpl, StatisticsDaoSpec}
import ru.yandex.complaints.dao.offers.{OffersDao, OffersDaoImpl}
import ru.yandex.complaints.dao.statistics.StatisticsDao
import ru.yandex.complaints.dao.statistics.impl.MySqlStatisticsDao
import ru.yandex.complaints.dao.users.{UsersDao, UsersDaoImpl}

/**
  * Runnable spec for [[ru.yandex.complaints.dao.complaints.StatisticsDaoSpec]]
  *
  * @author potseluev
  */
class MySqlStatisticsDaoSpec
  extends StatisticsDaoSpec
    with MySqlSpecTemplate {
  override val statisticDao: StatisticsDao = new MySqlStatisticsDao(mySql)

  override val complaintsDao: ComplaintsDao = new ComplaintsDaoImpl(mySql)

  override val offersDao: OffersDao = new OffersDaoImpl(mySql)

  override val usersDao: UsersDao = new UsersDaoImpl(mySql)
}
