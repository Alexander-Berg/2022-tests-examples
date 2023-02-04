package ru.yandex.verba.core.pipeline

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.util.Timeout
import ru.yandex.verba.core.storage.meta.MetaStorage
import ru.yandex.verba.core.preprocessor.validation.Transformers
import ru.yandex.verba.core.util.Logging
import scala.concurrent.ExecutionContext

/**
  * TODO
  */
/*
class PipelineTest extends AnyFreeSpec with Logging {

  "Syntax check" in {
    pending

    implicit val timeout = Timeout(100000, TimeUnit.MINUTES)
    implicit val ec: ExecutionContext = ???

    val transformers: Transformers = ???
    val metaStorage: MetaStorage = ???
    val entityStorage: ActorRef = ???
    val attributesStorage: ActorRef = ???

    val pipeline: RequestResponse =
      createIfNotExists(entityStorage) {
        onSaveTransform(transformers, metaStorage) ~> checkConstraints(entityStorage) ~> saveAttributes(attributesStorage) ~> requestResponse(entityStorage) ~> loadAttributes(attributesStorage)
      }
    pipeline(???)
  }
}
 */
