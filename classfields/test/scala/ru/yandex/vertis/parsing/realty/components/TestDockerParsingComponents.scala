package ru.yandex.vertis.parsing.realty.components

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
import ru.yandex.vertis.parsing.realty.components.bunkerconfig.TestBunkerConfigSupport
import ru.yandex.vertis.parsing.realty.components.clients.MockedClientsSupport
import ru.yandex.vertis.parsing.realty.components.converters.ConvertersSupport
import ru.yandex.vertis.parsing.realty.components.dao.DaoSupport
import ru.yandex.vertis.parsing.realty.components.diffs.DiffAnalyzerFactorySupport
import ru.yandex.vertis.parsing.realty.components.features.TestFeaturesSupport
import ru.yandex.vertis.parsing.realty.components.holocron.TestHolocronConverterSupport
import ru.yandex.vertis.parsing.realty.components.parsers.ParsersSupport
import ru.yandex.vertis.parsing.realty.components.shards.ParsingRealtyShardSupport
import ru.yandex.vertis.parsing.realty.components.unexpectedvalues.UnexpectedRealtyValuesSupport
import ru.yandex.vertis.parsing.realty.components.watchers.WatchersSupport

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
  with ParsingRealtyShardSupport
  with WatchersSupport
  with MockedTimeSupport
  with TestBunkerConfigSupport
  with TestFeaturesSupport
  with DaoSupport
  with MockedClientsSupport
  with TestHolocronConverterSupport
  with UnexpectedRealtyValuesSupport
  with ConvertersSupport
