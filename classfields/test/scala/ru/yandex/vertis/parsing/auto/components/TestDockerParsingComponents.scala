package ru.yandex.vertis.parsing.auto.components

import ru.yandex.vertis.parsing.auto.components.bunkerconfig.TestBunkerConfigSupport
import ru.yandex.vertis.parsing.auto.components.clients.MockedClientsSupport
import ru.yandex.vertis.parsing.auto.components.converters.ConvertersSupport
import ru.yandex.vertis.parsing.auto.components.dao.{CacheDaoSupport, ParsingDaoSupport}
import ru.yandex.vertis.parsing.auto.components.datareceivers.DataReceiverSupport
import ru.yandex.vertis.parsing.auto.components.diffs.DiffAnalyzerFactorySupport
import ru.yandex.vertis.parsing.auto.components.features.TestFeaturesSupport
import ru.yandex.vertis.parsing.auto.components.holocron.TestHolocronConverterSupport
import ru.yandex.vertis.parsing.auto.components.parsers.ParsersSupport
import ru.yandex.vertis.parsing.auto.components.shards.ParsingShardSupport
import ru.yandex.vertis.parsing.auto.components.unexpectedvalues.UnexpectedAutoValuesSupport
import ru.yandex.vertis.parsing.auto.components.watchers.WatchersSupport
import ru.yandex.vertis.parsing.clients.callcenterHelperApi.{CallCenterHelperClient, OfferUpload, OfferUploadDataElem}
import ru.yandex.vertis.parsing.components.akka.AkkaSupport
import ru.yandex.vertis.parsing.components.docker.TestDockerApplicationSupport
import ru.yandex.vertis.parsing.components.executioncontext.NonMeteredExecutionContextSupport
import ru.yandex.vertis.parsing.components.extdata.{CatalogsSupport, TestExtDataSupport}
import ru.yandex.vertis.parsing.components.io.IOSupport
import ru.yandex.vertis.parsing.components.monitor.MonitorSupport
import ru.yandex.vertis.parsing.components.operational.OperationalSupport
import ru.yandex.vertis.parsing.components.time.MockedTimeSupport
import ru.yandex.vertis.parsing.components.unexpectedvalues.SimpleUnexpectedValuesSupport
import ru.yandex.vertis.parsing.components.workersfactory.TestWorkersFactorySupport
import ru.yandex.vertis.parsing.components.zookeeper.TestZookeeperSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

/**
  * Created by andrey on 11/8/17.
  */
object TestDockerParsingComponents
  extends TestDockerApplicationSupport
  with IOSupport
  with AkkaSupport
  with NonMeteredExecutionContextSupport
  with TestExtDataSupport
  with CatalogsSupport
  with ParsersSupport
  with TestZookeeperSupport
  with TestWorkersFactorySupport
  with OperationalSupport
  with MonitorSupport
  with SimpleUnexpectedValuesSupport
  with DiffAnalyzerFactorySupport
  with ParsingShardSupport
  with WatchersSupport
  with MockedTimeSupport
  with TestBunkerConfigSupport
  with TestFeaturesSupport
  with CacheDaoSupport
  with ParsingDaoSupport
  with MockedClientsSupport
  with DataReceiverSupport
  with TestHolocronConverterSupport
  with UnexpectedAutoValuesSupport
  with ConvertersSupport
