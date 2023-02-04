package ru.yandex.vertis.parsing.api.components

import ru.yandex.vertis.parsing.api.components.handlers.MockedHandlersSupport
import ru.yandex.vertis.parsing.api.components.handlers.v1.V1HandlersSupport
import ru.yandex.vertis.parsing.api.components.managers.MockedManagersSupport
import ru.yandex.vertis.parsing.api.components.route.RouteSupport
import ru.yandex.vertis.parsing.components.TestApplicationSupport
import ru.yandex.vertis.parsing.components.akka.AkkaSupport
import ru.yandex.vertis.parsing.auto.components.bunkerconfig.TestBunkerConfigSupport
import ru.yandex.vertis.parsing.auto.components.clients.MockedClientsSupport
import ru.yandex.vertis.parsing.auto.components.converters.MockedConvertersSupport
import ru.yandex.vertis.parsing.auto.components.dao.MockedDaoSupport
import ru.yandex.vertis.parsing.components.executioncontext.NonMeteredExecutionContextSupport
import ru.yandex.vertis.parsing.auto.components.features.TestFeaturesSupport
import ru.yandex.vertis.parsing.components.operational.OperationalSupport
import ru.yandex.vertis.parsing.auto.components.parsers.ParsersSupport
import ru.yandex.vertis.parsing.auto.components.shards.MockedParsingShardSupport
import ru.yandex.vertis.parsing.clients.callcenterHelperApi.CallCenterHelperClient
import ru.yandex.vertis.parsing.components.time.MockedTimeSupport
import ru.yandex.vertis.parsing.components.tracing.TracingSupport
import ru.yandex.vertis.parsing.util.api.components.decider.DeciderSupport

/**
  * TODO
  *
  * @author aborunov
  */
object TestApiComponents
  extends TestApplicationSupport
  with AkkaSupport
  with NonMeteredExecutionContextSupport
  with OperationalSupport
  with MockedClientsSupport
  with MockedDaoSupport
  with ParsersSupport
  with TestFeaturesSupport
  with TracingSupport
  with MockedManagersSupport
  with MockedHandlersSupport
  with MockedConvertersSupport
  with MockedParsingShardSupport
  with MockedTimeSupport
  with V1HandlersSupport
  with DeciderSupport
  with RouteSupport
  with TestBunkerConfigSupport
