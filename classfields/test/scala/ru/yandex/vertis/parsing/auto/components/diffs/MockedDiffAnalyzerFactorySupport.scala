package ru.yandex.vertis.parsing.auto.components.diffs

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.components.diffs.DiffAnalyzerFactoryAware
import ru.yandex.vertis.parsing.diffs.DiffAnalyzerFactory

/**
  * TODO
  *
  * @author aborunov
  */
trait MockedDiffAnalyzerFactorySupport extends DiffAnalyzerFactoryAware[ParsedRow] with MockitoSupport {
  override def diffAnalyzerFactory: DiffAnalyzerFactory[ParsedRow] = mock[DiffAnalyzerFactory[ParsedRow]]
}
