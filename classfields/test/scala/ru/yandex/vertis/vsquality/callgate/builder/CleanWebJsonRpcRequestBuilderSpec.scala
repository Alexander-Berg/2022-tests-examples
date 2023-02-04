package ru.yandex.vertis.vsquality.callgate.builder

import ru.yandex.vertis.vsquality.callgate.apis.config.{CleanWebConfig, CleanWebRequestParamsConfig, JsonRpcConfig}
import ru.yandex.vertis.vsquality.callgate.generators.Arbitraries._
import ru.yandex.vertis.vsquality.callgate.model.Payload.{
  AutoruCleanWebInfo,
  AutoruOffers,
  RealtyCleanWebInfo,
  RealtyOffers
}
import ru.yandex.vertis.vsquality.callgate.model.jsonrpc.JsonRpcRequest
import ru.yandex.vertis.vsquality.callgate.model.{TaskDescriptor, TaskInfo}
import ru.yandex.vertis.hobo.proto.model.QueueId
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.http_client_utils.config.HttpClientConfig
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

class CleanWebJsonRpcRequestBuilderSpec extends SpecBase {

  private val cleanWebConfig =
    CleanWebConfig(
      true,
      HttpClientConfig("some_url"),
      JsonRpcConfig("2.0", "process"),
      CleanWebRequestParamsConfig("offers", "offer", true)
    )
  private val builder = new CleanWebJsonRpcRequestBuilder(cleanWebConfig)

  "CleanWebJsonRpcRequestBuilder" should {
    "correctly build request for AutoruCleanWebInfo" in {
      val payload = generate[AutoruCleanWebInfo]
      val taskDescriptor = TaskDescriptor(QueueId.AUTORU_CALLGATE_CLEAN_WEB, "the_unique_task_key")
      val taskInfo = TaskInfo(taskDescriptor, payload)
      builder.build(taskInfo).await shouldBe a[JsonRpcRequest[_]]
    }

    "correctly build request for RealtyCleanWebInfo" in {
      val payload = generate[RealtyCleanWebInfo]
      val taskDescriptor = TaskDescriptor(QueueId.REALTY_CALLGATE_CLEAN_WEB, "the_unique_task_key")
      val taskInfo = TaskInfo(taskDescriptor, payload)
      builder.build(taskInfo).await shouldBe a[JsonRpcRequest[_]]
    }

    "throws an exception for AutoruOffers payload " in {
      val payload = generate[AutoruOffers]
      val taskDescriptor = generate[TaskDescriptor]
      val taskInfo = TaskInfo(taskDescriptor, payload)
      builder.build(taskInfo).shouldFailWith[IllegalArgumentException]
    }

    "throws an exception for RealtyOffers payload " in {
      val payload = generate[RealtyOffers]
      val taskDescriptor = generate[TaskDescriptor]
      val taskInfo = TaskInfo(taskDescriptor, payload)
      builder.build(taskInfo).shouldFailWith[IllegalArgumentException]
    }
  }
}
