package ru.yandex.vos2.autoru.utils

import java.lang.annotation._

import org.junit.runner.Result
import org.junit.runner.notification.RunListener
import ru.yandex.vos2.autoru.utils.docker.DockerAutoruCoreComponentsBuilder

/**
  * Created by andrey on 11/7/16.
  */
class TestEventsListener extends RunListener {

  override def testRunFinished(result: Result): Unit = {
    println("TEST RUN FINISHED")
    DockerAutoruCoreComponentsBuilder.stopAll()
    super.testRunFinished(result)
  }

  //todo: что это?
  @Documented
  @Target(Array(ElementType.TYPE))
  @Retention(RetentionPolicy.RUNTIME) trait ThreadSafe {}
}
