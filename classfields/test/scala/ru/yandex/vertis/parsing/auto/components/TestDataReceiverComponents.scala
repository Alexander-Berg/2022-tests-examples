package ru.yandex.vertis.parsing.auto.components

import ru.yandex.vertis.parsing.auto.components.bunkerconfig.TestBunkerConfigSupport
import ru.yandex.vertis.parsing.auto.components.clients.MockedClientsSupport
import ru.yandex.vertis.parsing.auto.components.datareceivers.DataReceiverSupport
import ru.yandex.vertis.parsing.auto.components.features.TestFeaturesSupport
import ru.yandex.vertis.parsing.auto.components.parsers.ParsersSupport
import ru.yandex.vertis.parsing.auto.components.unexpectedvalues.UnexpectedAutoValuesSupport
import ru.yandex.vertis.parsing.components.TestApplicationSupport
import ru.yandex.vertis.parsing.components.extdata.{CatalogsSupport, TestExtDataSupport}
import ru.yandex.vertis.parsing.components.io.IOSupport
import ru.yandex.vertis.parsing.components.operational.OperationalSupport
import ru.yandex.vertis.parsing.components.time.MockedTimeSupport
import ru.yandex.vertis.parsing.components.unexpectedvalues.SimpleUnexpectedValuesSupport

/**
  * TODO
  *
  * @author aborunov
  */
trait TestDataReceiverComponents
  extends TestApplicationSupport
  with IOSupport
  with OperationalSupport
  with TestExtDataSupport
  with CatalogsSupport
  with MockedClientsSupport
  with DataReceiverSupport
  with MockedTimeSupport
  with TestBunkerConfigSupport
  with ParsersSupport
  with TestFeaturesSupport
  with SimpleUnexpectedValuesSupport
  with UnexpectedAutoValuesSupport
object TestDataReceiverComponents extends TestDataReceiverComponents
