package ru.yandex.vertis.chat.api.v1.domain.aggregators

import ru.yandex.vertis.chat.components.dao.aggregators.{AggregatorForUsers, JvmAggregatorsService}
import ru.yandex.vertis.chat.model.ModelGenerators.{dealerId, userId}
import ru.yandex.vertis.chat.model.api.ApiModel
import ru.yandex.vertis.chat.service.ServiceProtoFormats
import akka.http.scaladsl.model.StatusCodes
import ru.yandex.vertis.chat.api.HandlerSpecBase
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._

class AggregatorsHandlerSpec extends HandlerSpecBase with ProducerProvider with ServiceProtoFormats {

  private val service = new JvmAggregatorsService()
  private val route = seal(new AggregatorsHandler(service).route)
  import AggregatorsHandler._

  /* path("add") {
    createOrEdit
  } ~
    path("add" / "jivosite") {
      install
    } ~
    getOne ~
    deleteOne*/

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
  val savedAggregator: AggregatorForUsers = AggregatorForUserFormat.read(aggregator)

  s"POST ${root}add" should {
    "create aggregator for user" in {

      supportedMediaTypes.foreach { mediaType =>
        Post(s"${root}add")
          .withUser(user)
          .withSomePassportUser
          .withEntity(aggregator.toByteArray)
          .accepting(mediaType) ~> route ~>
          check {
            status should be(StatusCodes.OK)
            savedAggregator.asAggregatorList.toSet should equal(
              service.getAggregatorInfoByUsers(Seq(user, user2)).futureValue.toSet
            )
          }
      }
      service.delete(user).futureValue
      service.delete(user2).futureValue
    }
  }

  s"GET $root" should {
    "get aggregator for user" in {
      val xxx = service.createOrEdit(savedAggregator).futureValue
      supportedMediaTypes.foreach { mediaType =>
        Get(s"$root")
          .withUser(user)
          .withSomePassportUser
          .withQueryParam(UserParam, user)
          .accepting(mediaType) ~> route ~>
          check {
            status should be(StatusCodes.OK)
            val base = responseAs[AggregatorForUsers].asAggregatorInfo
            savedAggregator.asAggregatorInfo should equal(base)
          }
      }
      service.delete(user).futureValue
      service.delete(user2).futureValue
    }
  }

  s"DELETE $root" should {
    "delete aggregator for user" in {

      supportedMediaTypes.foreach { mediaType =>
        service.createOrEdit(savedAggregator).futureValue
        Delete(s"$root")
          .withUser(user)
          .withSomePassportUser
          .withQueryParam(UserParam, user)
          .accepting(mediaType) ~> route ~>
          check {
            status should be(StatusCodes.OK)
            savedAggregator.asAggregatorList.tail.toSet should equal(
              service.getAggregatorInfoByUsers(Seq(user, user2)).futureValue.toSet
            )
          }
        service.delete(user2).futureValue
      }

    }
  }

  s"POST ${root}add/jivosite" should {
    "install for user" in {
      val user3: UserId = userId.next
      val aggregator = ApiModel.AggregatorInstallRequest
        .newBuilder()
        .addUsers(user3)
        .setChannelName("channel")
        .build()
      supportedMediaTypes.foreach { mediaType =>
        Post(s"${root}add/jivosite")
          .withUser(user)
          .withSomePassportUser
          .withEntity(aggregator.toByteArray)
          .accepting(mediaType) ~> route ~>
          check {
            status should be(StatusCodes.OK)
            service.getAggregatorInfoByUser(user3).futureValue should not be None
          }
        service.delete(user3).futureValue
      }
    }
  }
}
