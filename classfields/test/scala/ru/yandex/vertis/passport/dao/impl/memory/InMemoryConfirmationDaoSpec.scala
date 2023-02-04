package ru.yandex.vertis.passport.dao.impl.memory

import ru.yandex.vertis.passport.dao.{ConfirmationDao, ConfirmationDaoSpec}

/**
  * todo
  *
  * @author zvez
  */
class InMemoryConfirmationDaoSpec extends ConfirmationDaoSpec {
  override val confirmationDao: ConfirmationDao = new InMemoryConfirmationDao
}
