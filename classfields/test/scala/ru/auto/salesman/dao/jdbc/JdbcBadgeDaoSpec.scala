package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.JdbcBadgeDao
import ru.auto.salesman.dao.{BadgeDao, BadgeDaoSpec}
import ru.auto.salesman.test.template.BadgeJdbcSpecTemplate

class JdbcBadgeDaoSpec extends BadgeDaoSpec with BadgeJdbcSpecTemplate {
  val badgeDao: BadgeDao = new JdbcBadgeDao(database)
}
