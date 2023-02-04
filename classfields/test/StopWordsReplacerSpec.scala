package auto.dealers.calltracking.storage.test

import auto.dealers.calltracking.storage.StopWordsReplacerRepository
import auto.dealers.calltracking.storage.testkit.TestStopWordsReplacer
import zio.test._
import zio.test.Assertion._

object StopWordsReplacerSpec extends DefaultRunnableSpec {

  val stopWords = Set("test", "testful", "word", "сочетания слов", "case", "язык")

  override def spec: ZSpec[Environment, Failure] =
    suite("StopWordsReplacerSpec")(
      simpleReplaceTest,
      substringsTest,
      caseAndLanguageTest,
      punctuationAndTagsTest
    ).provideCustomLayer(TestStopWordsReplacer.make(stopWords))

  val simpleReplaceTest =
    testM("replace words") {
      val string = "test testful words"
      val expected = "*** *** words"
      assertM(StopWordsReplacerRepository.replaceIn(string, "***"))(equalTo(expected))
    }

  val substringsTest =
    testM("substrings are prone to replacement") {
      val string = "test testful testfullness testless"
      val expected = "*** *** testfullness testless"
      assertM(StopWordsReplacerRepository.replaceIn(string, "***"))(equalTo(expected))
    }

  val caseAndLanguageTest =
    testM("neither case nor language matters") {
      val string = "Язык и CaSe don't MaTter"
      val expected = "*** и *** don't MaTter"
      assertM(StopWordsReplacerRepository.replaceIn(string, "***"))(equalTo(expected))
    }

  val punctuationAndTagsTest =
    testM("punctuation and html tags are ok with replacement") {
      val string = "<b>Test</b> and \"testful\" are replaced, but <b>testfullness</b> is not"
      val expected = "<b>***</b> and \"***\" are replaced, but <b>testfullness</b> is not"
      assertM(StopWordsReplacerRepository.replaceIn(string, "***"))(equalTo(expected))
    }

}
