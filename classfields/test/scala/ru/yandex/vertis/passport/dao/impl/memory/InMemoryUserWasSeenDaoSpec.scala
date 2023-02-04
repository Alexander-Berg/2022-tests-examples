package ru.yandex.vertis.passport.dao.impl.memory

import ru.yandex.vertis.passport.service.session.UserWasSeenDaoSpec

class InMemoryUserWasSeenDaoSpec extends UserWasSeenDaoSpec {

  override val dao = new InMemoryUserWasSeenDao
}
