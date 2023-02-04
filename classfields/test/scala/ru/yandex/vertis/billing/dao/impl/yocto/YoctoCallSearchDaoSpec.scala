package ru.yandex.vertis.billing.dao.impl.yocto

import java.io.File

import ru.yandex.vertis.billing.dao._

class YoctoCallSearchDaoSpec extends CallsSearchDaoSpec {

  val file = new File("./yocto-test")
  file.mkdirs()

  protected val callRequestsDaoFactory: CallsSearchDaoFactory =
    new YoctoCallsSearchDaoFactory(file, "test")
}
