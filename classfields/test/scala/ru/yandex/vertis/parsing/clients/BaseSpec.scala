package ru.yandex.vertis.parsing.clients

import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait BaseSpec extends Suite with ScalaFutures with Eventually with BeforeAndAfter with BeforeAndAfterAll {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true
}
