package ru.yandex.vertis.punisher.tasks

import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.AutoruUser

/**
  * @author devreggs
  */
trait FindAndPunishTaskSpec extends BaseSpec {

  def findAndPunishTask: FindAndPunishTask[F, AutoruUser]

  "FindAndPunish" should {
    "successful ended" in {
      findAndPunishTask.payload.await
    }
  }
}
