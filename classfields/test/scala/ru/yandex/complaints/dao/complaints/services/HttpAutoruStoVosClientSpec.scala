package ru.yandex.complaints.dao.complaints.services

import java.io.IOException

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Ignore, Matchers, WordSpec}
import ru.yandex.complaints.services.impl.HttpAutoruStoVosClient
import ru.yandex.complaints.services.instrumentation.LoggedAutoruStoVosClient

import scala.util.Failure

/**
  * Specs for [[HttpAutoruStoVosClient]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpAutoruStoVosClientSpec
  extends WordSpec
  with Matchers {

  "HttpAutoruStoVosClient" should {

    val Client =
      new HttpAutoruStoVosClient("http://vos2-rt-01-sas.test.vertis.yandex.net:36267")
      with LoggedAutoruStoVosClient

    "fail to delete unknown sto" in {
      Client.delete("unknown") match {
        case Failure(_: NoSuchElementException) => ()
        case other => fail(s"Unexpected $other")
      }
    }
  }

  "HttpAutoruStoVosClient" should {

    val Client =
      new HttpAutoruStoVosClient("http://unknown:36267")
      with LoggedAutoruStoVosClient

    "fail to delete if url is broken" in {
      Client.delete("123") match {
        case Failure(_: IOException) => ()
        case other => fail(s"Unexpected $other")
      }
    }
  }

}
