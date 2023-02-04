package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.NumberUpdateDao.NumberWithOperator
import ru.yandex.vertis.telepony.dao.jdbc.SharedOperatorNumberDao.Filter
import ru.yandex.vertis.telepony.dao.{NumberUpdateDao, NumberUpdateDaoSpec}
import ru.yandex.vertis.telepony.generator.Generator.{originOperatorGen, OperatorAccountGen, SharedOperatorNumberGen}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{Operator, SharedOperatorNumber}
import ru.yandex.vertis.telepony.util.db.SlickUtils.RichDatabasePublisher

/**
  * @author tolmach
  */
class JdbcSharedNumberUpdateDaoIntSpec extends NumberUpdateDaoSpec[SharedOperatorNumber] with JdbcSpecTemplate {

  override val dao: NumberUpdateDao = new JdbcSharedNumberUpdateDao(sharedDualDb)

  private val sharedNumber = new SharedOperatorNumberDao(sharedDualDb)

  override def clear(): Unit = {
    sharedNumber.clear().futureValue
  }

  override def numbersWithOperators(
      count: Int,
      operator: Operator,
      withOriginOperator: Boolean): Seq[SharedOperatorNumber] = {
    val account = OperatorAccountGen.filter(_.operator == operator).next
    val base = SharedOperatorNumberGen.map(_.copy(account = account))
    val gen = if (withOriginOperator) {
      base.map(_.copy(originOperator = Some(originOperatorGen(account.operator).next)))
    } else {
      base.map(_.copy(originOperator = None))
    }
    gen.nextUniqueBy(count)(_.number).toSeq
  }

  override def toNumberWithOperator(number: SharedOperatorNumber): NumberWithOperator = {
    NumberWithOperator(number.number, number.account.operator)
  }

  override def addNumbers(numbers: Seq[SharedOperatorNumber]): Unit = {
    numbers.foreach(sharedNumber.upsert(_).futureValue)
  }

  override def allNumbers(): Seq[NumberWithOperators] = {
    sharedNumber.list(Filter.All, useDbStream = false).runToSeq().futureValue.map { n =>
      NumberWithOperators(n.number, n.account.operator, n.originOperator)
    }
  }

}
