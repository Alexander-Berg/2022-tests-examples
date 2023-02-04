package ru.yandex.verba.core.index

import akka.util.Timeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.application
import ru.yandex.verba.core.indexing.IndexManager
import ru.yandex.verba.core.util.FutureUtils._
import ru.yandex.verba.core.util.JsonUtils

import scala.concurrent.duration._

/**
  * Author: Evgeny Vanslov (evans@yandex-team.ru)
  * Created: 26.06.14
  */
class IndexManagerTest /*extends AnyFlatSpec */ {
  /* implicit val timeout: FiniteDuration = 60.seconds
  implicit val tout = Timeout(timeout)
  implicit val ec = application.system.dispatcher

  "IndexManager " should " work with this simple query " in {
    val queryResult = IndexManager.ref.query("auto-emission-euro-class").await
    println(JsonUtils.prettyRender(queryResult.asJson))
  }

  "IndexManager " should " successfully handle empty result" in {
    val queryResult = IndexManager.ref.query("балконыsdfsdfsdfasdfsdfasdfsadfsadfdsfds").await
    println(JsonUtils.prettyRender(queryResult.asJson))
  }*/

}
