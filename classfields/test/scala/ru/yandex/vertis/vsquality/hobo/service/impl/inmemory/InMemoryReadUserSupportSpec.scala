package ru.yandex.vertis.vsquality.hobo.service.impl.inmemory

import ru.yandex.vertis.vsquality.hobo.model.User
import ru.yandex.vertis.vsquality.hobo.service.{ReadUserSupport, ReadUserSupportSpecBase}

/**
  * Specs on [[InMemoryReadUserSupport]]
  *
  * @author semkagtn
  */

class InMemoryReadUserSupportSpec extends ReadUserSupportSpecBase {

  override def newReadUserSupport(users: User*): ReadUserSupport = new InMemoryReadUserSupport(users)
}
