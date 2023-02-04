package ru.yandex.hydra.profile.api.v2.counter

import org.jetbrains.annotations.NotNull
import ru.yandex.hydra.profile.dao.counter.TokenCounterDAO
import ru.yandex.hydra.profile.dao.counter.impl.Forgetting

/** Always returns [[Forgetting]]
  *
  * @author incubos
  */
object ForgettingDAOFactory extends TokenCounterDAOFactory {

  @NotNull
  override def get(
      @NotNull
      service: String,
      @NotNull
      locale: String,
      @NotNull
      component: String): Some[TokenCounterDAO] = Some(Forgetting)
}
