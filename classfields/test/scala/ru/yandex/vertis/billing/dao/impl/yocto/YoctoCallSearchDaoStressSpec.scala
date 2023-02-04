package ru.yandex.vertis.billing.dao.impl.yocto

import java.io.File

import ru.yandex.vertis.billing.dao._

class YoctoCallSearchDaoStressSpec extends CallRequestsDaoStressSpec {

  val file = new File("./yocto-stress-test")
  file.mkdirs()

  protected val callRequestsDaoFactory: CallsSearchDaoFactory =
    new YoctoCallsSearchDaoFactory(file, "stress-test")
}
