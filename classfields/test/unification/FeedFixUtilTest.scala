package ru.yandex.vertis.general.feed.processor.pipeline.test.unification

import general.feed.transformer.RawImage
import ru.yandex.vertis.general.feed.processor.pipeline.unification.FeedFixUtil
import zio.test.Assertion.equalTo
import zio.test._

object FeedFixUtilTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FeedFixUtilTest")(
      test("исправляет двойное экранирование xml символов") {
        val result = FeedFixUtil.fixXmlEscaping(
          "&quot;Не двигаться &amp;apos;&amp;amp;&amp;lt;это&gt;&amp;&apos; проверка эскейпинга&quot;"
        )
        assert(result)(equalTo("\"Не двигаться '&<это>&' проверка эскейпинга\""))
      },
      test("ничего не делает если уже есть схема") {
        val image = RawImage("https://ya.ru")
        val fixedImage = FeedFixUtil.enrichImageWithProtocol(image)
        assert(fixedImage.url)(equalTo("https://ya.ru"))
      },
      test("добавить схему если ее нет и url начинается с //") {
        val image = RawImage("//ya.ru")
        val fixedImage = FeedFixUtil.enrichImageWithProtocol(image)
        assert(fixedImage.url)(equalTo("http://ya.ru"))
      },
      test("добавить схему если ее нет вообще") {
        val image = RawImage("ya.ru")
        val fixedImage = FeedFixUtil.enrichImageWithProtocol(image)
        assert(fixedImage.url)(equalTo("http://ya.ru"))
      }
    )
}
