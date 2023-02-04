package ru.yandex.vertis.vsquality.techsupport.dao.impl.ydb

import ru.yandex.vertis.vsquality.techsupport.dao.{AppealDao, AppealDaoSpecBase}
import ru.yandex.vertis.vsquality.techsupport.util.ydb.YdbSpecBase

/**
  * @author potseluev
  */
class YdbAppealDaoSpec extends AppealDaoSpecBase with YdbSpecBase {

  override def appealDao: AppealDao[F] =
    new YdbAppealDao(ydb)(new YdbAppealSerialization)
}
