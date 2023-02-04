package ru.yandex.vertis.promocoder.dao.impl.jdbc

import ru.yandex.vertis.promocoder.dao.PromocodeDaoSpec

/** Runnable specs on [[OrmPromocodeDao]]
  *
  * @author alex-kovalenko
  */
class OrmPromocodeDaoSpec extends PromocodeDaoSpec with JdbcContainerSpecTemplate {

  val dao = new OrmPromocodeDao(database)
}
