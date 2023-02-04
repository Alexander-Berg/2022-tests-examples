package ru.yandex.hydra.profile.api.v2.limiter

import org.jetbrains.annotations.NotNull
import ru.yandex.hydra.profile.dao.limiter.service.impl.Forgetting

/** Always returns [[Forgetting]]
  *
  * @author incubos
  */
object ForgettingDAOFactory extends LimiterDAOFactory {

  @NotNull
  override def get(
      @NotNull
      service: String,
      @NotNull
      locale: String,
      @NotNull
      component: String) = Some(Forgetting)
}
