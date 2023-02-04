package ru.yandex.vos2.autoru.utils

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.InitTestDbs

/**
  * Created by andrey on 11/2/16.
  */
@RunWith(classOf[JUnitRunner])
class StopWordsTest extends AnyFunSuite with InitTestDbs {
  private val stopWords = new StopWords()

  test("init stop words") {
    assert(stopWords.nonEmpty)
    assert(!stopWords.check("individualkiexercises.org"))
    assert(!stopWords.check("dosugcz.com"))
    assert(!stopWords.check("авито.ру"))
    assert(stopWords.check(""))
    assert(stopWords.check("колёса"))
  }
}
