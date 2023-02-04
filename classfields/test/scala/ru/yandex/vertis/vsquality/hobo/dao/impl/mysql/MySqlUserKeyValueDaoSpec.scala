package ru.yandex.vertis.vsquality.hobo.dao.impl.mysql

import org.scalacheck.Gen

import ru.yandex.vertis.vsquality.hobo.dao.{KeyValueDao, KeyValueDaoSpecBase}
import ru.yandex.vertis.vsquality.hobo.model.User
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[MySqlUserKeyValueDao]]
  *
  * @author semkagtn
  */

class MySqlUserKeyValueDaoSpec extends KeyValueDaoSpecBase[User] with MySqlSpecBase {

  override val valueGen: Gen[User] = CoreGenerators.UserGen

  override val keyValueDao: KeyValueDao[User] = new MySqlUserKeyValueDao(database)
}
