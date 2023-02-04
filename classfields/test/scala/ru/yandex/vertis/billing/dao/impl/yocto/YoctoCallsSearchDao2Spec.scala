package ru.yandex.vertis.billing.dao.impl.yocto

import java.io.File

import ru.yandex.vertis.billing.dao.{CallsSearchDao2Spec, CallsSearchDaoFactory}

/**
  * @author ruslansd
  */
class YoctoCallsSearchDao2Spec extends CallsSearchDao2Spec {

  val file = new File("./yocto-test")
  file.mkdirs()

  override protected def callRequestsDaoFactory: CallsSearchDaoFactory =
    new YoctoCallsSearchDaoFactory(file, "test")

}
