package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{TranscriptionTaskDao, TranscriptionTaskDaoSpec}
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author neron
  */
class JdbcTranscriptionTaskDaoIntSpec extends TranscriptionTaskDaoSpec with JdbcSpecTemplate {

  override def dao: TranscriptionTaskDao = new JdbcTranscriptionTaskDao(dualDb, TypedDomains.autoru_def)

}
