package ru.yandex.hydra.profile.api.v2.clicker

import org.jetbrains.annotations.NotNull
import ru.yandex.hydra.profile.dao.clicker.impl.Forgetting
import ru.yandex.hydra.profile.dao.clicker.service.ClickerService

/** Always returns [[Forgetting]]
  *
  * @author incubos
  */
object ForgettingDAOFactory extends ClickerDAOFactory {

  @NotNull
  override def get(
      @NotNull
      service: String,
      @NotNull
      locale: String,
      @NotNull
      component: String): Option[ClickerService] = Some(Forgetting)
}
