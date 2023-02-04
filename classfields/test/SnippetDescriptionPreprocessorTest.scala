package ru.yandex.vertis.general.search.logic.test

import ru.yandex.vertis.general.search.logic.SnippetDescriptionPreprocessor
import ru.yandex.vertis.general.search.logic.SnippetDescriptionPreprocessor.{
  MaxDescriptionLength,
  TruncatedDescriptionSuffix
}
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object SnippetDescriptionPreprocessorTest extends DefaultRunnableSpec {

  private def runTest(initialDescription: String, expectedDescription: String, title: String = "") =
    for {
      result <- SnippetDescriptionPreprocessor.preprocessDescription(description = initialDescription, title = title)
    } yield assert(result)(equalTo(expectedDescription))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SnippetDescriptionPreprocessor")(
      testM("убирает перенос строк после точки") {
        runTest(
          initialDescription = "Привет.\nЭто собака",
          expectedDescription = "Привет. Это собака"
        )
      },
      testM("убирает много whitespace и переносов строк после точки") {
        runTest(
          initialDescription = "Привет..   \n\n  \n \n   Это собака",
          expectedDescription = "Привет.. Это собака"
        )
      },
      testM("ставит точку, если убираются переносы строк") {
        runTest(
          initialDescription = "Привет   \n\n  \n \n   Это собака\n\n\n\n",
          expectedDescription = "Привет. Это собака"
        )
      },
      testM("работает с html переносами строк") {
        runTest(
          initialDescription = "\n Новый<p>телефон. <br><br> продам \n в мск<br />",
          expectedDescription = "Новый. телефон. продам. в мск"
        )
      },
      testM("вытаскивает текст из невалидного HTML") {
        runTest(
          initialDescription = "<br><p><em><strong>Канекалоны<p>модные косички 1<2&lt;3",
          expectedDescription = "Канекалоны. модные косички 1<2<3"
        )
      },
      testM("обрезает длинное описание") {
        runTest(
          initialDescription = Iterator.continually("a").take(MaxDescriptionLength * 2).mkString(""),
          expectedDescription = Iterator
            .continually("a")
            .take(MaxDescriptionLength - TruncatedDescriptionSuffix.length)
            .mkString("") + TruncatedDescriptionSuffix
        )
      },
      testM("не обрезает короткое описание") {
        runTest(
          initialDescription = "  \n  " + Iterator.continually("a").take(MaxDescriptionLength).mkString("") + "\n\n",
          expectedDescription = Iterator.continually("a").take(MaxDescriptionLength).mkString("")
        )
      },
      testM("не вырезает тайтл из описания, если описание не полностью совпадает с тайтлом") {
        runTest(
          initialDescription = "Джинсы мужские Levis",
          expectedDescription = "Джинсы мужские Levis",
          title = "Джинсы Levis"
        )
      },
      testM("Не вырезает тайтл из середины описания") {
        runTest(
          initialDescription = "Продам джинсы Levis",
          expectedDescription = "Продам джинсы Levis",
          title = "джинсы"
        )
      },
      testM("Вырезает тайтл из описания, если описание начинается с тайтла") {
        runTest(
          initialDescription = "Джинсы Levis, ., \n новые. С чеком.",
          expectedDescription = "Новые. С чеком.",
          title = "Джинсы Levis"
        )
      },
      testM(
        "Вырезает тайтл из описания, даже если описание или тайтл начинается с whitespace или не совпадает регистр"
      ) {
        runTest(
          initialDescription = "\n  \n \t   дЖиНсЫ Levis, новые. С чеком.",
          expectedDescription = "Новые. С чеком.",
          title = "   ДжИнсы levis"
        )
      },
      testM(
        "Вырезает тайтл из описания при наличии повторяющихся пробелов и специальных символов"
      ) {
        runTest(
          initialDescription = "ОЧКИ ДЛЯ ПЛАВАНИЯ<>  (АРТ.RD-3300). + ЧЕХОЛ В ПОДАРОК.\n\nОчки  из прочных материалов",
          expectedDescription = "ЧЕХОЛ В ПОДАРОК. Очки из прочных материалов",
          title = "ОЧКИ ДЛЯ ПЛАВАНИЯ<>  (АРТ.RD-3300)."
        )
      }
    )
  }
}
