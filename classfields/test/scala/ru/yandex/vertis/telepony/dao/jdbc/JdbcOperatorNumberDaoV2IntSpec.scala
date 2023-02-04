package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{OperatorNumberDaoV2, OperatorNumberDaoV2Spec}
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * Runnable specs on [[JdbcOperatorNumberDaoV2]].
  *
  * @author dimas
  */
class JdbcOperatorNumberDaoV2IntSpec extends OperatorNumberDaoV2Spec with JdbcSpecTemplate {
  val dao: OperatorNumberDaoV2 = new JdbcOperatorNumberDaoV2(TypedDomains.autoru_def)
}
