package ru.yandex.vertis.vsquality.techsupport.util

import ru.yandex.vertis.vsquality.techsupport.dao.KeyValueDao

/**
  * @author potseluev
  */
trait ClearableKeyValueDaoProvider {
  implicit protected def clearableDao[F[_], K, V]: Clearable[KeyValueDao[F, K, V]]
}
