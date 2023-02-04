package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{CommonOperatorNumberDao, CommonOperatorNumberDaoV2Spec}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

class JdbcCommonOperatorNumberDaoIntSpec extends CommonOperatorNumberDaoV2Spec with JdbcSpecTemplate {

  val dao: CommonOperatorNumberDao = new JdbcCommonOperatorNumberDao
}
