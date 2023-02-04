package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{RecordDao, RecordDaoSpec}
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author evans
  */
class JdbcRecordDaoIntSpec extends RecordDaoSpec with JdbcSpecTemplate {

  override val dao: RecordDao = new JdbcRecordDao(TypedDomains.autoru_def)
}
