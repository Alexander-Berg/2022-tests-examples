package ru.yandex.vertis.subscriptions.storage.ydb

import com.dimafeng.testcontainers.ForAllTestContainer
import ru.yandex.vertis.ydb.YdbContainer
import ru.yandex.vertis.ydb.zio.{LoggedYdbZioWrapper, YdbZioWrapper}
import zio.clock.Clock
import zio.{Runtime, ZLayer}

import scala.concurrent.duration.DurationInt

trait TestYdb extends ForAllTestContainer {
  this: org.scalatest.Suite =>

  override val container: YdbContainer = {
    val c = YdbContainer.stable
    c.container.start()
    c
  }

  val ydbWrapper: YdbZioWrapper = {
    new LoggedYdbZioWrapper(
      YdbZioWrapper.make(container.tableClient, "/local", 30.seconds)
    )
  }

  implicit val zioRuntime: Runtime.Managed[YEnv] = Runtime.unsafeFromLayer(
    Clock.live +!+ ZLayer.succeed(ydbWrapper)
  )

  override def beforeStop(): Unit = {
    container.tableClient.close()
    container.rpc.close()
    super.beforeStop()
  }

}
