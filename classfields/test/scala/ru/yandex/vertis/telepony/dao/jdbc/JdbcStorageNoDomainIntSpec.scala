package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{Storage, StorageSpec}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate
import ru.yandex.vertis.telepony.util.serializer.CustomSerializer

/**
  * @author evans
  */
class JdbcStorageNoDomainIntSpec extends StorageSpec with JdbcSpecTemplate {

  override val kvDao: Storage[String] =
    new JdbcStorage(dualDb, None, "test", new CustomSerializer[String])
}
