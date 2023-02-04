package ru.auto.salesman.dao.jdbc

import org.scalatest.OptionValues
import ru.auto.salesman.dao.AutostrategiesDaoSpec
import ru.auto.salesman.dao.impl.jdbc.test.JdbcAutostrategiesDaoForTests
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import ru.yandex.vertis.generators.ProducerProvider.asProducer

class JdbcAutostrategiesDaoSpec
    extends AutostrategiesDaoSpec
    with SalesmanJdbcSpecTemplate
    with OptionValues {

  override val autostrategiesDao: JdbcAutostrategiesDaoForTests =
    new JdbcAutostrategiesDaoForTests(database)

  "JdbcAutostrategiesDao" should {

    "get autostrategy by id" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val id = autostrategiesDao.all().success.value.head.id
      autostrategiesDao.getById(id)
    }

    "get deleted autostrategy by id" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val id = autostrategiesDao.all().success.value.head.id
      autostrategiesDao.delete(Seq(autostrategy.id)).success
      autostrategiesDao
        .getById(id)
        .success
        .value
        .value
        .props shouldBe autostrategy
    }
  }
}
