package ru.yandex.vertis.vsquality.techsupport.util

import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao

/**
  * @author potseluev
  */
trait ClearableAppealDaoProvider {
  implicit protected def clearableAppealDao[F[_]]: Clearable[AppealDao[F]]
}
