package ru.yandex.vertis.chat.components.dao.aggregators

import ru.yandex.vertis.chat.RequestContext
import ru.yandex.vertis.chat.components.ComponentsSpecBase
import ru.yandex.vertis.chat.model.ModelGenerators.{dealerId, userId}
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.vertis.chat.service.ServiceProtoFormats._
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.mockito.MockitoSupport

class JvmAggregatorsServiceSpec extends ComponentsSpecBase with ProducerProvider with MockitoSupport {
  implicit private val rc = mock[RequestContext]
  private val user: UserId = dealerId.next
  private val user2: UserId = userId.next

  private val aggregator = ApiModel.AggregatorInfo
    .newBuilder()
    .addUsers(user)
    .addUsers(user2)
    .setHook("hook/token")
    .setChannelName("channel")
    .setToken("token")
    .build()
  private val savedAggregator: AggregatorForUsers = AggregatorForUserFormat.read(aggregator)

  private val aggregatorsService: AggregatorsService = new JvmAggregatorsService()

  "AggregatorsService" should {

    //TODO make us whole
    "install" in {
      val aggregator = ApiModel.AggregatorInstallRequest
        .newBuilder()
        .addUsers(user)
        .setChannelName("channel")
        .build()

      aggregatorsService.install(null, aggregator).futureValue
    }

    "create" in {
      aggregatorsService.createOrEdit(savedAggregator).futureValue
    }

    "edit" in {
      aggregatorsService.createOrEdit(savedAggregator).futureValue
    }

    "getByToken" in {
      val data = aggregatorsService.getByToken("token").futureValue
      data.toSet should equal(savedAggregator.asAggregatorList.toSet)
    }

    "getUserIdsByToken" in {
      val data = aggregatorsService.getUserIdsByToken("token").futureValue
      data.toSet should equal(savedAggregator.users.toSet)
    }

    "getAggregatorInfoByUser" in {
      val data = aggregatorsService.getAggregatorInfoByUser(user).futureValue
      data should equal(savedAggregator.asAggregatorInfo)
    }

    "getAggregatorInfoByUsers" in {
      val data = aggregatorsService.getAggregatorInfoByUsers(Seq(user, user2, "dummy")).futureValue
      data.toSet should equal(savedAggregator.asAggregatorList.toSet)
    }

    "delete" in {
      aggregatorsService.delete(user)
    }
  }
}
