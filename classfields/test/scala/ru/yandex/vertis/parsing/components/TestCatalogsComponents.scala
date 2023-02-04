package ru.yandex.vertis.parsing.components

import ru.yandex.vertis.parsing.components.extdata.{CatalogsSupport, TestExtDataSupport}
import ru.yandex.vertis.parsing.components.io.IOSupport

object TestCatalogsComponents extends TestApplicationSupport with IOSupport with TestExtDataSupport with CatalogsSupport
