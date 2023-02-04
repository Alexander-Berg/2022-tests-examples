package ru.yandex.vertis.passport.dao.impl.mysql

import org.scalacheck.Gen
import ru.yandex.vertis.passport.dao.FullUserDaoSpec
import ru.yandex.vertis.passport.model.FullUser
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  * Tests for LegacyAutoruUserDao
  *
  * @author zvez
  */
class LegacyAutoruUserDaoSpec extends FullUserDaoSpec with MySqlSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val userDao = new LegacyAutoruUserDao(
    DualDatabase(dbs.legacyUsers),
    DualDatabase(dbs.legacyOffice)
  )

  override val userGenerator: Gen[FullUser] =
    ModelGenerators.legacyUser
      .map(_.copy(active = true))
}
