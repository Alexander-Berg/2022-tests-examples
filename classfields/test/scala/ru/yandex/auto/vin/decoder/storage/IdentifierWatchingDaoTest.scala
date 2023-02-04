package ru.yandex.auto.vin.decoder.storage

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import org.springframework.dao.DuplicateKeyException
import ru.yandex.auto.vin.decoder.model.IdentifierGenerators.VinCodeGen
import ru.yandex.auto.vin.decoder.model.state.StateUtils
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.vertis.mockito.MockitoSupport

class IdentifierWatchingDaoTest extends AnyFunSuite with MockitoSupport {
  implicit val t = Traced.empty
  val shardedMySql = mock[ShardedMySql]
  val shard = mock[Shard]
  val database = mock[Database]
  val vinWatchingDao = new VinWatchingDao(shardedMySql)

  test("suppress DuplicateKeyException insertState") {
    val identifier = VinCodeGen.sample.get
    when(shardedMySql.getShard(eq(identifier))).thenReturn(shard)
    when(shard.master).thenReturn(database)
    when(database.update(?, ?)(?)).thenThrow(new DuplicateKeyException("message"))
    vinWatchingDao.insertState(identifier, StateUtils.getNewStateUpdate)
  }
}
