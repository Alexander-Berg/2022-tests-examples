package ru.yandex.realty.searcher.suggest.tags

import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters.setAsJavaSetConverter

/**
  * Test for tag prefix tree
  *
  * @author nstaroverova
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class TagNameIndexSpecBase extends FlatSpec with Matchers {

  val tagIndex = new TagNameIndex(TestTagProvider)
  tagIndex.update()

  "TagNameIndex" should "return several tags with the common string" in {
    test("окна", 2)
    test("ижс", 1)
    test("база отдыха", 1)
    test("водоём", 1)
    test("хлеб", 1)
  }

  "TagNameIndex" should "return several tags with the common string in uppercase" in {
    test("ОКНА", 2)
    test("ИЖС", 1)
    test("БАЗА ОТДЫХА", 1)
    test("ВОДОЁМ", 1)
    test("ХЛЕБ", 1)
  }

  "TagNameIndex" should "return several tags for string typed in wrong keyboard layout" in {
    test("jryf", 2)
    test("b;c", 1)
    test(",fpf jnls[f", 1)
    test("djlj`v", 1)
    test("djlj\\v", 1)
    test("[kt,", 1)
  }

  "TagNameIndex" should "return several tags for string typed in wrong keyboard layout in uppercase" in {
    test("JRYF", 2)
    test("B:C", 1)
    test("<FPF JNLS{F", 1)
    test("DJLJ~V", 1)
    test("DJLJ|V", 1)
    test("{KT<", 1)
  }

  it should "return empty Set of tags if tags do not exist for input suggest" in {
    test("абракадабра", 0)
  }

  private def test(suggest: String, expectedSize: Int) {
    val searchResult = tagIndex.find(suggest)
    searchResult.size() should be(expectedSize)
  }
}
