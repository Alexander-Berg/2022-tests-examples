package ru.yandex.vertis.personal

import ru.yandex.vertis.personal.generators.Producer
import ru.yandex.vertis.personal.model.ModelGenerators.SettingsGen
import ru.yandex.vertis.personal.model.UserRef
import ru.yandex.vertis.personal.model.settings.Settings

/**
  * Specs on [[JvmPropertyDao]].
  *
  * @author dimas
  */
class JvmPropertyDaoSpec extends PropertyDaoSpecBase[Settings] {

  val dao: PropertyDao[Settings] = new JvmPropertyDao[Settings](emptyProperty)

  def emptyProperty(user: UserRef): Settings = Settings.empty(user)

  def nextProperty(user: UserRef): Settings = SettingsGen.next
}
