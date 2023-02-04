package ru.yandex.vertis.general.search.logic.test

import ru.yandex.vertis.general.search.logic.{PalmaQueryCleaner, QueryCleaner}
import ru.yandex.vertis.general.search.logic.QueryCleaner.QueryCleaner
import ru.yandex.vertis.general.search.resource.RedundantWordsSnapshot
import zio.{Ref, ZLayer}
import zio.test.Assertion._
import zio.test._

object QueryCleanerTest extends DefaultRunnableSpec {

  private val cleanerLayer: ZLayer[Any, Nothing, QueryCleaner] = (for {
    ref <- Ref.make(new RedundantWordsSnapshot(words = Seq("купить", "куплю")))
  } yield new PalmaQueryCleaner(ref)).toLayer

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("QueryClaner")(
      testM("Не изменяет текст без ненужных слов") {
        val initialText = "обувь для женщин бу nike"
        for {
          cleanedText <- QueryCleaner.cleanQuery(initialText)
        } yield assert(cleanedText)(equalTo(initialText))
      },
      testM("Вырезает ненужные слова") {
        val initialText = "Купить обувь nike куплю для женщин КУпИТь"
        for {
          cleanedText <- QueryCleaner.cleanQuery(initialText)
        } yield assert(cleanedText)(equalTo(" обувь nike  для женщин "))
      },
      testM("Не выразет части других слов") {
        val initialText = "обувь купитьдля женщин"
        for {
          cleanedText <- QueryCleaner.cleanQuery(initialText)
        } yield assert(cleanedText)(equalTo(initialText))
      }
    ).provideCustomLayerShared(cleanerLayer)
  }
}
