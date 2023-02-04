package ru.yandex.auto.searchline.query

import java.util
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

import junit.framework.TestCase._
import org.junit.Assert
import org.mockito
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike, Matchers}
import org.springframework.test.AbstractDependencyInjectionSpringContextTests
import ru.yandex.auto.core.AutoLang
import ru.yandex.auto.core.dictionary.{MapBasedDictionaryImpl, Type}
import ru.yandex.auto.core.dictionary.query.auto.{MarkType, ModelType}
import ru.yandex.auto.searchline.api.directive.DebugParams
import ru.yandex.auto.searchline.model.SearchQuery
import ru.yandex.common.util.collections.{CollectionFactory, Pair}
import ru.yandex.common.util.functional.Function

import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
  * @author pnaydenov
  */
class AntlrQueryProcessorTest extends AbstractDependencyInjectionSpringContextTests
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterEach {
  private var antlrQueryProcessor: QueryProcessor =
    getApplicationContext.getBean("antlrQueryProcessor").asInstanceOf[QueryProcessor]
  private var markDictionary: Function[Pair[String, AnyRef], MarkType] =
    getApplicationContext.getBean("markDictionary").asInstanceOf[Function[Pair[String, AnyRef], MarkType]]
  private var modelDictionary: Function[Pair[String, AnyRef], util.List[ModelType]] =
    getApplicationContext.getBean("modelDictionary").asInstanceOf[Function[Pair[String, AnyRef], util.List[ModelType]]]

  override protected def beforeEach(): Unit = {
    Mockito.reset(markDictionary, modelDictionary)
  }

  private implicit def stringToSearchQuery(text: String): SearchQuery = SearchQuery(text, -1, None, DebugParams.empty)

  test("MarkModelQuery") {
    mockMarkTypes(createMarkType("FORD", list("форд"), list(createModelType("FOCUS", list("фокус")))))
    val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor("форд фокус")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.marks.size, 1)
    assertEquals(visitor.models.size, 1)
    assertEquals(visitor.marks.head._1.getCode, "FORD")
    assertEquals(visitor.models.head._1.getCode, "FOCUS")
  }

  test("YearInterval") {
    val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor("1999-2010")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.yearIntervals.size, 1)
    assertEquals(visitor.yearIntervals.head._1.get, 1999)
    assertEquals(visitor.yearIntervals.head._2.get, 2010)
  }

  test("AmbiguousAliases") {
    mockMarkTypes(createMarkType("GINETTA", list("джинетта"), list(createModelType("G", list("г")))))
    val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor("г 1998 г")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.models.head._1.getCode, "G")
    assertEquals(visitor.yearIntervals.head._1.get, 1998)
    assertEquals(visitor.yearIntervals.head._2.get, 1998)
  }

  test("AmbiguousAliases2") {
    mockMarkTypes(createMarkType("FORD", list("форд"), list(createModelType("500", list("500")))))
    val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor
      .processQueryAndGetVisitor("форд 500 от 500 тысяч рублей")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.marks.head._1.getCode, "FORD")
    assertEquals(visitor.models.head._1.getCode, "500")
    assertEquals(visitor.priceIntervals.head._1._1.get, 500000)
    assertEquals(visitor.priceIntervals.head._1._2, Option.empty)
    assertEquals(visitor.priceIntervals.head._2, "RUR")
  }

  test("Number") {
    val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor("от 500 рублей")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.priceIntervals.head._1._1.get, 500)
    val visitor2: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor("до 3 литров")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor2.displacementIntervals.head._1, Option.empty)
    assertEquals(visitor2.displacementIntervals.head._2.get, 3000)
  }

  test("LongDash") {
    val start: Long = System.currentTimeMillis
    antlrQueryProcessor.processQueryAndGetVisitor("-" * 1140)
    Assert.assertTrue((System.currentTimeMillis - start) < 1000)
  }

  test("YearsAndModel") {
    mockMarkTypes(createMarkType("FORD", list("форд"), list(createModelType("500", list("500")))))
    val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor("форд 2007-2010")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.yearIntervals.head._1.get, 2007)
    assertEquals(visitor.yearIntervals.head._2.get, 2010)
    assertEquals(visitor.marks.head._1.getCode, "FORD")
  }

  test("UnknownMarkModel") {
    val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor("big legend")
      .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.marks.size, 0)
    assertEquals(visitor.models.size, 0)
    assertEquals(visitor.unknownFragments.size, 2)
    assertEquals(visitor.unknownFragments.apply(0).getText, "big")
    assertEquals(visitor.unknownFragments.apply(1).getText, "legend")
  }

  test("LetterBasedNumbers") {
    val visitor: SearchlineQueryParameterVisitor =
      antlrQueryProcessor.processQueryAndGetVisitor("двадцать два литра")
        .asInstanceOf[SearchlineQueryParameterVisitor]
    assertEquals(visitor.displacementIntervals.size, 1)
    visitor.displacementIntervals.head shouldEqual (Some(22000), Some(22000))
  }

  test("ComplicatedLetterBasedNums") {
    def testNum(query: String, num: Int): Unit = {
      val visitor: SearchlineQueryParameterVisitor = antlrQueryProcessor.processQueryAndGetVisitor(query)
        .asInstanceOf[SearchlineQueryParameterVisitor]
      assertEquals(visitor.priceIntervals.size, 1)
      visitor.priceIntervals.head shouldEqual ((Some(num), Some(num)), "RUR")
    }

    testNum("две тысячи пятсот семь рублей", 2507)
    testNum("две тысячи семь рублей", 2007)
    testNum("две тысячи пятсот двадцать семь рублей", 2527)
    testNum("миллион двести тысяч рублей", 1200000)
    testNum("семь миллионов двести шестнадцать тысяч рублей", 7216000)
    testNum("семь сотен рублей", 700)
    testNum("три сотни миллионов двести тысяч рублей", 300200000)
    testNum("триста миллионов двести тысяч рублей", 300200000)
    testNum("два миллиарда сто сорок три миллиона двести восемьдесят пять тысяч триста семнадцать рублей", 2143285317)
  }

  private def mockMarkTypes(markTypes: MarkType*): Unit = {
    val markTypeMapBasedDictionary: MapBasedDictionaryImpl[MarkType] = new MapBasedDictionaryImpl[MarkType]
    val modelListMapBasedDictionary = new MapBasedDictionaryImpl[util.List[ModelType]]
    val markMap: ConcurrentHashMap[String, MarkType] = new ConcurrentHashMap[String, MarkType]
    val modelMap: ConcurrentHashMap[String, util.List[ModelType]] = new ConcurrentHashMap[String, util.List[ModelType]]
    for (markType <- markTypes) {
      for (str <- markType.getStringVariants.asScala) {
        markMap.put(str, markType)
      }
      for (modelType <- markType.getModels.asScala) {
        for (str <- modelType.getStringVariants.asScala) {
          modelMap.put(str, Collections.singletonList(modelType))
        }
      }
    }
    markTypeMapBasedDictionary.setMap(markMap)
    modelListMapBasedDictionary.setMap(modelMap)
    Mockito.when(this.markDictionary.apply(mockito.Matchers.any())).thenAnswer(new Answer[MarkType]() {
      override def answer(invocationOnMock: InvocationOnMock): MarkType =
        markTypeMapBasedDictionary.apply(invocationOnMock.getArguments()(0).asInstanceOf[String])
    })
    Mockito.when(this.modelDictionary.apply(mockito.Matchers.any())).thenAnswer(new Answer[util.List[ModelType]]() {
      override def answer(invocationOnMock: InvocationOnMock): util.List[ModelType] =
        modelListMapBasedDictionary.apply(invocationOnMock.getArguments()(0).asInstanceOf[String])
    })
  }

  private def createMarkType(code: String, aliases: util.List[String], modelTypes: util.List[ModelType]): MarkType = {
    val `type`: Type = createType(code, aliases)
    val markType: MarkType = new MarkType(`type`, null)
    for (modelType <- modelTypes.asScala) {
      modelType.setMark(markType)
    }
    markType.setModels(modelTypes)
    markType
  }

  private def createModelType(code: String, aliases: util.List[String]): ModelType =
    new ModelType(createType(code, aliases), null, null)

  private def createType(code: String, aliases: util.List[String]): Type = {
    val `type`: Type = new Type(code, Collections.emptyMap[AutoLang, String], Collections.emptyMap[AutoLang, String])

    for (alias <- aliases.asScala) {
      `type`.addStringVariant(alias)
    }
    `type`
  }

  override protected def getConfigPaths: Array[String] = Array("/context-test.xml")

  private def list[T](values: T*) = {
    val res = CollectionFactory.newArrayList[T](values.length)
    for (t <- values) {
      res.add(t)
    }
    res
  }
}
