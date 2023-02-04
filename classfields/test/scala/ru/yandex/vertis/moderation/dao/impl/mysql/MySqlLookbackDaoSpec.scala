package ru.yandex.vertis.moderation.dao.impl.mysql

import java.nio.ByteBuffer

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.impl.mysql.MySqlLookbackDaoSpec.settings
import ru.yandex.vertis.moderation.dao.{LookbackDao, LookbackDaoSpecBase}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class MySqlLookbackDaoSpec extends LookbackDaoSpecBase[Int] with MySqlSpecBase {

  override lazy val lookbackDao: LookbackDao[Int] =
    new MySqlLookbackDao[Int](
      db = database,
      settings = settings,
      service = Service.REALTY
    )

  override val payloadGen: Gen[Int] = Gen.chooseNum(-9999, +9999)
}

object MySqlLookbackDaoSpec {
  private val settings: LookbackDao.Settings[Int] =
    new LookbackDao.Settings[Int] {
      override def tableName: String = "owners_lookback"

      override def serialize(payload: Int): Array[Byte] = ByteBuffer.allocate(4).putInt(payload).array

      override def deserialize(bytes: Array[Byte]): Int = ByteBuffer.wrap(bytes).getInt
    }
}
