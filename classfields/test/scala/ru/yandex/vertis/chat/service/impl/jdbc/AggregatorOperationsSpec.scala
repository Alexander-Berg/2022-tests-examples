package ru.yandex.vertis.chat.service.impl.jdbc

import org.scalatest.OptionValues
import org.scalatest.words.ShouldVerb
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.components.dao.aggregators.AggregatorForUsers
import ru.yandex.vertis.chat.dao.jdbc.AggregatorsOperations
import ru.yandex.vertis.chat.service.impl.TestDomainAware
import ru.yandex.vertis.mockito.MockitoSupport

class AggregatorOperationsSpec
  extends SpecBase
  with JdbcSpec
  with TestDomainAware
  with OptionValues
  with MockitoSupport {

  "AggregatorOperations" should {
    "Add aggregators" in {
      val afu = AggregatorForUsers(Seq("1", "2", "3"), "hook", "token", "channelName");
      database.master.run(AggregatorsOperations.modify(afu)).futureValue
      val af2 = AggregatorForUsers(Seq("4"), "hook2", "token2", "channelName2");
      database.master.run(AggregatorsOperations.modify(af2)).futureValue

      val data = database.slave.run(AggregatorsOperations.getAggregatorInfoByUsers(Seq("1", "4"))).futureValue
      data.size shouldBe (2)
    }

  }

}
