package vertis.palma.dao.ydb.storage

import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.ydb.Ydb
import vertis.palma.dao.model.{ActualSecretKeyNotFound, SecretKeyAlreadyExistException, SecretKeyNotFound}
import vertis.palma.encrypted.EncryptedSpecBase
import vertis.palma.encrypted.cipher.AESGCMCipher
import vertis.palma.gen.EncryptedGenerators._
import vertis.palma.gen.ModelGenerators
import vertis.ydb.YEnv
import vertis.ydb.test.YdbTest
import zio.RIO

class EncryptedSecretsYdbStorageIntSpec extends EncryptedSpecBase with YdbTest with ProducerProvider {

  def withStorage(action: EncryptedSecretsYdbStorage => RIO[YEnv, _]): Unit = {
    val storage = new EncryptedSecretsYdbStorage(ydbWrapper, prometheusRegistry)
    zioRuntime.unsafeRunTask(action(storage))
    ()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    withStorage(_.init())
  }

  private val cipher = new AESGCMCipher

  "EncryptedSecretsYdbStorage" should {
    "fetch created key by id" in withStorage { storage =>
      val dictId = ModelGenerators.StrGen.next
      for {
        toCreate <- generateKey(dictId)
        _ <- Ydb.runTx(storage.createOrReplace(toCreate))
        fetched <- Ydb.runTx(storage.fetch(dictId, toCreate.encryptedKeyPair.keyId))
        _ <- checkKeys(fetched, toCreate)
      } yield ()
    }

    "failed to fetch non-existent key" in intercept[SecretKeyNotFound] {
      withStorage { storage =>
        Ydb.runTx(storage.fetch(ModelGenerators.StrGen.next, ModelGenerators.StrGen.next))
      }
    }

    "fetch actual key by dict id" in withStorage { storage =>
      val dictId = ModelGenerators.StrGen.next
      for {
        oneKey <- generateKey(dictId)
        _ <- Ydb.runTx(storage.createOrReplace(oneKey))
        _ <- Ydb.runTx(storage.invalidateByDictionaryId(dictId))
        secondKey <- generateKey(dictId)
        _ <- Ydb.runTx(storage.createOrReplace(secondKey))
        actual <- Ydb.runTx(storage.fetchActual(dictId))
        _ <- checkKeys(actual, secondKey)
      } yield ()
    }

    "failed to fetch actual key if no one" in intercept[ActualSecretKeyNotFound] {
      withStorage { storage =>
        val dictId = ModelGenerators.StrGen.next
        for {
          toCreate <- generateKey(dictId)
          _ <- Ydb.runTx(storage.createOrReplace(toCreate))
          _ <- Ydb.runTx(storage.invalidateByDictionaryId(dictId))
          _ <- Ydb.runTx(storage.fetchActual(dictId))
        } yield ()
      }
    }

    "fetch actual by dict id and master key hash" in withStorage { storage =>
      val dictId = ModelGenerators.StrGen.next
      for {
        masterKey1 <- cipher.generatePlainKey
        key1 <- generateKey(dictId, Some(masterKey1))
        _ <- Ydb.runTx(storage.createOrReplace(key1))

        masterKey2 <- cipher.generatePlainKey
        key2 <- generateKey(dictId, Some(masterKey2))
        _ <- Ydb.runTx(storage.createOrReplace(key2))

        actual <- Ydb.runTx(storage.fetchActual(dictId, key1.encryptedKeyContext.masterKeyHash))
        _ <- checkKeys(actual, key1)
      } yield ()
    }

    "failed to fetch actual key by master key if no one" in intercept[ActualSecretKeyNotFound] {
      withStorage { storage =>
        val dictId = ModelGenerators.StrGen.next
        for {
          masterKey1 <- cipher.generatePlainKey
          key1 <- generateKey(dictId, Some(masterKey1))
          _ <- Ydb.runTx(storage.createOrReplace(key1))

          masterKey2 <- cipher.generatePlainKey
          key2 <- generateKey(dictId, Some(masterKey2))
          _ <- Ydb.runTx(storage.fetchActual(dictId, key2.encryptedKeyContext.masterKeyHash))
        } yield ()
      }
    }

    "not failed on checkKeyNonExist with non-existed key" in withStorage { storage =>
      Ydb.runTx(storage.checkKeyNonExist(ModelGenerators.StrGen.next, ModelGenerators.StrGen.next))
    }

    "failed on checkKeyNonExist with existed key" in intercept[SecretKeyAlreadyExistException] {
      withStorage { storage =>
        val dictId = ModelGenerators.StrGen.next
        for {
          key <- generateKey(dictId)
          _ <- Ydb.runTx(storage.createOrReplace(key))
          _ <- Ydb.runTx(storage.checkKeyNonExist(dictId, key.encryptedKeyPair.keyId))
        } yield ()
      }
    }

    "invalidate keys" in intercept[ActualSecretKeyNotFound] {
      withStorage { storage =>
        val dictId = ModelGenerators.StrGen.next
        for {
          oneKey <- generateKey(dictId)
          secondKey <- generateKey(dictId)
          _ <- Ydb.runTx(storage.createOrReplace(oneKey))
          _ <- Ydb.runTx(storage.createOrReplace(secondKey))

          _ <- Ydb.runTx(storage.invalidateByDictionaryId(dictId))

          _ <- Ydb.runTx(storage.fetchActual(dictId))
        } yield ()
      }
    }

  }
}
