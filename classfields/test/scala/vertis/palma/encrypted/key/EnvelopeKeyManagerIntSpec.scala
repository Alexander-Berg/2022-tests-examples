package vertis.palma.encrypted.key

import ru.yandex.vertis.generators.ProducerProvider
import vertis.zio.ServerEnv
import vertis.palma.conf.{EncryptionConfig, MasterKeyConfig}
import vertis.palma.dao.EncryptedSecretsDaoImpl
import vertis.palma.dao.ydb.storage.EncryptedSecretsYdbStorage
import vertis.palma.encrypted.EncryptedSpecBase
import vertis.palma.encrypted.cipher.AESGCMCipher
import vertis.palma.gen.ModelGenerators
import vertis.ydb.test.YdbTest
import zio.RIO

class EnvelopeKeyManagerIntSpec extends EncryptedSpecBase with YdbTest with ProducerProvider {

  private val cipher = new AESGCMCipher

  private lazy val currentMasterKey = zioRuntime.unsafeRunTask(cipher.generatePlainKey)
  private lazy val oldMasterKey = zioRuntime.unsafeRunTask(cipher.generatePlainKey)

  private lazy val config: EncryptionConfig = {
    EncryptionConfig(
      MasterKeyConfig(
        MasterKeyConfig.base64Encoder.encodeToString(currentMasterKey.bytes),
        Seq(MasterKeyConfig.base64Encoder.encodeToString(oldMasterKey.bytes))
      )
    )
  }

  private lazy val previousConfig: EncryptionConfig = {
    EncryptionConfig(
      MasterKeyConfig(
        MasterKeyConfig.base64Encoder.encodeToString(oldMasterKey.bytes),
        Seq.empty
      )
    )
  }

  def withKeyManager(action: (KeyManager, KeyManager) => RIO[ServerEnv, _]): Unit = {
    val dao = new EncryptedSecretsDaoImpl(ydbWrapper, prometheusRegistry)
    val keyManager = new EnvelopeKeyManager(dao, config, cipher)
    val oldKeyManager = new EnvelopeKeyManager(dao, previousConfig, cipher)
    ioTest(action(keyManager, oldKeyManager))
  }

  def withCachingKeyManager(action: CachingKeyManager => RIO[ServerEnv, _]): Unit = {
    val dao = new EncryptedSecretsDaoImpl(ydbWrapper, prometheusRegistry)
    val keyManager = zioRuntime.unsafeRunTask(EnvelopeKeyManager.make(dao, config, cipher))
    ioTest(action(keyManager))
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val storage = new EncryptedSecretsYdbStorage(ydbWrapper, prometheusRegistry)
    zioRuntime.unsafeRunTask(storage.init())
  }

  "EnvelopeKeyManager" should {
    "fetch current key" in withKeyManager { (current, _) =>
      val dictId = ModelGenerators.StrGen.next
      for {
        createdKey <- current.getLatestOrCreate(dictId)
        fetched <- current.getDataKey(dictId, createdKey.keyId)
        _ <- checkBytes(fetched.bytes, createdKey.key.bytes)
      } yield ()
    }

    "fetch old key" in withKeyManager { (current, old) =>
      val dictId = ModelGenerators.StrGen.next
      for {
        oldKey <- old.getLatestOrCreate(dictId)
        _ <- current.getLatestOrCreate(dictId)
        fetched <- current.getDataKey(dictId, oldKey.keyId)
        _ <- checkBytes(fetched.bytes, oldKey.key.bytes)
      } yield ()
    }

    "create key once" in withKeyManager { (current, _) =>
      val dictId = ModelGenerators.StrGen.next
      for {
        created <- current.getLatestOrCreate(dictId)
        latest <- current.getLatestOrCreate(dictId)
        _ <- checkKeyPair(latest, created)
      } yield ()
    }

    "rotate old key" in withKeyManager { (current, old) =>
      val dictId = ModelGenerators.StrGen.next
      for {
        _ <- old.getLatestOrCreate(dictId)
        previous <- old.getLatestOrCreate(dictId)

        _ <- current.getLatestOrCreate(dictId)
        current <- current.getLatestOrCreate(dictId)

        _ <- check(current.keyId should not be previous.keyId)
        _ <- check(
          current.key.bytes should not contain theSameElementsInOrderAs(previous.key.bytes)
        )
      } yield ()
    }

    "fetch key from dao and add to cached keys" in withCachingKeyManager { keyManager =>
      val dictId = ModelGenerators.StrGen.next
      for {
        createdKey <- keyManager.getLatestOrCreate(dictId)
        prevState <- keyManager.state
        keyId = createdKey.keyId
        _ <- check(prevState.get((dictId, keyId)) shouldBe empty)
        fetched <- keyManager.getDataKey(dictId, keyId)
        _ <- checkBytes(fetched.bytes, createdKey.key.bytes)
        newState <- keyManager.state
        _ <- checkBytes(newState((dictId, keyId)).bytes, fetched.bytes)
      } yield ()
    }

    "re-fetch key from cache" in withCachingKeyManager { keyManager =>
      val dictId = ModelGenerators.StrGen.next
      for {
        createdKey <- keyManager.getLatestOrCreate(dictId)
        keyId = createdKey.keyId
        fetchedFromDao <- keyManager.getDataKey(dictId, keyId)
        cache <- keyManager.state
        _ <- checkBytes(cache((dictId, keyId)).bytes, fetchedFromDao.bytes)
        fetchedFromCache <- keyManager.getDataKey(dictId, keyId)
        _ <- checkBytes(fetchedFromCache.bytes, createdKey.key.bytes)
      } yield ()
    }
  }

}
