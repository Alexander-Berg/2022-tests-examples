package ru.yandex.vertis.vsquality.callgate.tasks.impl

import ru.yandex.vertis.vsquality.callgate.converters.ProtoFormat._
import ru.yandex.vertis.vsquality.callgate.model.TaskInfo
import ru.yandex.vertis.vsquality.callgate.proto.inner.{TaskInfo => InnerTaskInfo}
import ru.yandex.vertis.vsquality.callgate.tasks.util.TaskInfoBuilderUtil
import ru.yandex.vertis.hobo.proto.model.{Task => HoboTask}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

class RealtyCleanWebTaskInfoBuilderSpec extends SpecBase with TaskInfoBuilderUtil {

  override def loadRes(name: String): String =
    readResourceFileAsString(s"/tasks/task-info-builder/realty-offers/$name.json")

  private val builder = new RealtyCleanWebTaskInfoBuilder[F]

  "RealtyCleanWebTaskInfoBuilder" should {

    "correctly parse offer" in {
      val expected = loadEntity[TaskInfo, InnerTaskInfo]("clean-web/1/expected")

      val hoboTask = loadEntity[HoboTask, HoboTask]("clean-web/1/hobo-task")
      val actual = builder.build(hoboTask).value.await.getOrElse(???)

      actual shouldBe expected
    }

    "fail when json missing some field" in {
      val hoboTask = loadEntity[HoboTask, HoboTask]("clean-web/2/hobo-task")
      val actual = builder.build(hoboTask).value.await
      actual.isLeft shouldBe true
    }

    "fail on incorrect json field name" in {
      val hoboTask = loadEntity[HoboTask, HoboTask]("clean-web/3/hobo-task")
      val actual = builder.build(hoboTask).value.await
      actual.isLeft shouldBe true
    }
  }
}
