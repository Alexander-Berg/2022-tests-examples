package ru.yandex.vertis.billing.push.impl

import org.scalatest.Ignore
import ru.yandex.vertis.akka.ning.pipeline.Pipeline
import ru.yandex.vertis.billing.async.ActorSystemSpecBase
import ru.yandex.vertis.billing.push.AsyncPushClientSpec
import ru.yandex.vertis.util.ahc.{AsyncHttpClientBuilder, HttpSettings}

import scala.concurrent.duration.DurationInt

/**
  *
  * Runnable spec on [[AsyncPushClientImpl]]
  *
  * @author ruslansd
  */
@Ignore //todo(darl) тест давно не запускался.
class AsyncPushClientImplSpec extends AsyncPushClientSpec with ActorSystemSpecBase {

  private val DefaultHttpSettings =
    HttpSettings(
      5.seconds,
      60.seconds,
      3,
      100
    )

  private val resolver =
    DefaultPushPathResolver("http://visharder01ht.vs.yandex.net:34330")
  val pipeline = new Pipeline(AsyncHttpClientBuilder.createClient(DefaultHttpSettings))

  val client =
    new AsyncPushClientImpl(pipeline, resolver)

  override protected def name: String = "AsyncPushClientImplSpec"
}
