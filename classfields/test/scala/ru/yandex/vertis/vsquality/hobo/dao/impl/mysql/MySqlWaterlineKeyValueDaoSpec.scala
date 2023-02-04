package ru.yandex.vertis.vsquality.hobo.dao.impl.mysql

import org.joda.time.DateTime

import org.scalacheck.Gen

import ru.yandex.vertis.vsquality.hobo.dao.{KeyValueDao, KeyValueDaoSpecBase}
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[MySqlWaterlineKeyValueDao]]
  *
  * @author semkagtn
  */

class MySqlWaterlineKeyValueDaoSpec extends KeyValueDaoSpecBase[DateTime] with MySqlSpecBase {

  override val valueGen: Gen[DateTime] = CoreGenerators.DateTimeGen

  override val keyValueDao: KeyValueDao[DateTime] = new MySqlWaterlineKeyValueDao(database)
}
