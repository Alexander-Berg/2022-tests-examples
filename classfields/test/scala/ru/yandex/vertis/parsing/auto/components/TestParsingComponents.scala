package ru.yandex.vertis.parsing.auto.components

import ru.yandex.vertis.parsing.auto.components.bunkerconfig.TestBunkerConfigSupport
import ru.yandex.vertis.parsing.auto.components.clients.MockedClientsSupport
import ru.yandex.vertis.parsing.auto.components.converters.MockedConvertersSupport
import ru.yandex.vertis.parsing.auto.components.dao.MockedDaoSupport
import ru.yandex.vertis.parsing.auto.components.diffs.MockedDiffAnalyzerFactorySupport
import ru.yandex.vertis.parsing.auto.components.features.TestFeaturesSupport
import ru.yandex.vertis.parsing.auto.components.holocron.SimpleHolocronConverterSupport
import ru.yandex.vertis.parsing.auto.components.parsers.ParsersSupport
import ru.yandex.vertis.parsing.clients.callcenterHelperApi.{CallCenterHelperClient, OfferUpload, OfferUploadDataElem}
import ru.yandex.vertis.parsing.components.TestApplicationSupport
import ru.yandex.vertis.parsing.components.akka.AkkaSupport
import ru.yandex.vertis.parsing.components.executioncontext.NonMeteredExecutionContextSupport
import ru.yandex.vertis.parsing.components.extdata.{CatalogsSupport, TestExtDataSupport}
import ru.yandex.vertis.parsing.components.io.IOSupport
import ru.yandex.vertis.parsing.components.time.MockedTimeSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
object TestParsingComponents
  extends TestApplicationSupport
  with IOSupport
  with AkkaSupport
  with NonMeteredExecutionContextSupport
  with TestExtDataSupport
  with CatalogsSupport
  with MockedDaoSupport
  with MockedClientsSupport
  with MockedConvertersSupport
  with TestBunkerConfigSupport
  with ParsersSupport
  with MockedDiffAnalyzerFactorySupport
  with TestFeaturesSupport
  with MockedTimeSupport
  with SimpleHolocronConverterSupport
