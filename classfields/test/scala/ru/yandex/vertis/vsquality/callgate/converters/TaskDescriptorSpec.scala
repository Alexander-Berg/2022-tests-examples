package ru.yandex.vertis.vsquality.callgate.converters

import ru.yandex.vertis.vsquality.callgate.generators.Arbitraries._
import ru.yandex.vertis.vsquality.callgate.model.TaskDescriptor
import ru.yandex.vertis.hobo.proto.model.QueueId
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

/**
  * @author mpoplavkov
  */
class TaskDescriptorSpec extends SpecBase {

  private val queueMock = mock[QueueId]
  private val queueId = generate[QueueId]
  private val taskKey = generate[String]

  "TaskDescriptor" should {
    "be successfully created with correct queue id" in {
      val descriptor = TaskDescriptor(queueId, taskKey)
      val queueValue = queueId.value
      val charSeq =
        for {
          i <- Seq(100, 10, 1)
        } yield (queueValue / i) % 10

      descriptor.toString shouldBe s"${charSeq.mkString("")}$taskKey"
    }

    "fail to create for big queue id" in {
      when(queueMock.value).thenReturn(1000)
      assertThrows[IllegalArgumentException] {
        TaskDescriptor(queueMock, taskKey)
      }
    }
  }
}
