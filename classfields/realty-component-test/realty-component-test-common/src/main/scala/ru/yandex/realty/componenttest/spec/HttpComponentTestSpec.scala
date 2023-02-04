package ru.yandex.realty.componenttest.spec

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.componenttest.env.ComponentTestEnvironment
import ru.yandex.realty.http.HandlerSpecBase

trait HttpComponentTestSpec[T <: ComponentTestEnvironment[_]]
  extends ComponentTestSpec[T]
  with HandlerSpecBase
  with AsyncSpecBase
