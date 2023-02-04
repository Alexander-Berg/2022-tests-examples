package ru.yandex.vertis.billing.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.billing.BindingClientSpec

/**
  * Runnable spec on [[HttpBindingClient]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpBindingClientSpec extends BindingClientSpec {

  val bindingClient = new HttpBindingClient(Uri)
}
