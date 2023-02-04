package vertis.palma.dao

import com.yandex.ydb.core.{StatusCode, UnexpectedResultException}
import ru.yandex.vertis.generators.ProducerProvider
import vertis.zio.ServerEnv
import vertis.palma.conf.MasterKeyConfig
import vertis.palma.dao.model.{
  ActualSecretKeyNotFound,
  SecretKeyAlreadyExistException,
  SecretKeyNotFound,
  UnexpectedYdbException
}
import vertis.palma.encrypted.EncryptedSpecBase
import vertis.palma.encrypted.cipher.AESGCMCipher
import vertis.palma.gen.EncryptedGenerators.generateKey
import vertis.palma.gen.ModelGenerators
import vertis.ydb.test.YdbTest
import zio.{RIO, Task, ZIO}

class EncryptedSecretsDaoIntSpec extends EncryptedSpecBase with YdbTest with ProducerProvider {

  def withDao(action: EncryptedSecretsDaoImpl => RIO[ServerEnv, _]): Unit = {
    val dao = new EncryptedSecretsDaoImpl(ydbWrapper, prometheusRegistry)
    ioTest(action(dao))
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    withDao(_.secrets.init())
  }

  private val cipher = new AESGCMCipher

  "EncryptedSecretsDao" should {

    "create new actual key if no one" in withDao { dao =>
      val dictId = ModelGenerators.StrGen.next
      for {
        keyToCreate <- generateKey(dictId)
        created <- dao.actualOrCreate(dictId)(Task(keyToCreate))
        stored <- dao.actual(dictId)

        _ <- check(keyToCreate shouldBe created)
        _ <- checkKeys(stored, keyToCreate)
      } yield ()
    }

    "not to create new actual key if exist" in withDao { dao =>
      val dictId = ModelGenerators.StrGen.next
      for {
        masterKey <- cipher.generatePlainKey
        keyToCreate <- generateKey(dictId, Some(masterKey))
        _ <- dao.actualOrCreate(dictId)(Task(keyToCreate))

        alternativeKey <- generateKey(dictId, Some(masterKey))
        alternativeKey2 <- generateKey(dictId, Some(masterKey))

        actual1 <- dao.actualOrCreate(dictId)(Task(alternativeKey))
        actual2 <- dao.actualOrCreate(dictId, keyToCreate.encryptedKeyContext.masterKeyHash)(Task(alternativeKey2))

        _ <- checkKeys(actual1, keyToCreate)
        _ <- checkKeys(actual2, keyToCreate)
      } yield ()
    }

    "not to create new key if exist (concurrent)" in withDao { dao =>
      val dictId = ModelGenerators.StrGen.next
      for {
        masterKey <- cipher.generatePlainKey
        hash = MasterKeyConfig.cryptoHash(masterKey.bytes)

        keys <- ZIO.collectAll(List.fill(10)(generateKey(dictId, Some(masterKey))))

        resOrErr <- ZIO.partitionPar(keys)(k => dao.actualOrCreate(dictId, hash)(Task(k)))
        err = resOrErr._1
        _ <- ZIO.foreach(err) { ex =>
          for {
            _ <- check(ex shouldBe a[UnexpectedYdbException])
            palmaEx = ex.asInstanceOf[UnexpectedYdbException]
            _ <- check(palmaEx.getCause shouldBe a[UnexpectedResultException])
            ydbEx = palmaEx.getCause.asInstanceOf[UnexpectedResultException]
            _ <- check(ydbEx.getStatusCode shouldBe StatusCode.ABORTED)
          } yield ()
        }
        res = resOrErr._2
        headKey = res.head
        headKeyId = headKey.encryptedKeyPair.keyId
        _ <- ZIO.foreach(res)(checkKeys(_, headKey))

        withoutActual = keys.filterNot(_.encryptedKeyPair.keyId == headKeyId).map(_.encryptedKeyPair.keyId)
        createdNonActive <- ZIO.collectAllSuccesses(withoutActual.map(id => dao.getKey(dictId, id)))
        _ <- check(createdNonActive shouldBe empty)
      } yield ()
    }

    "rotate key if master key changed" in withDao { dao =>
      val dictId = ModelGenerators.StrGen.next
      for {
        oldMasterKey <- cipher.generatePlainKey
        oldKey <- generateKey(dictId, Some(oldMasterKey))
        newMasterKey <- cipher.generatePlainKey
        newKey <- generateKey(dictId, Some(newMasterKey))

        _ <- dao.actualOrCreate(dictId)(Task(oldKey))
        beforeMasterKeyRotation <- dao.actual(dictId)
        _ <- checkKeys(beforeMasterKeyRotation, oldKey)

        _ <- dao.actualOrCreate(dictId, newKey.encryptedKeyContext.masterKeyHash)(Task(newKey))
        afterMasterKeyRotation <- dao.actual(dictId)
        _ <- checkKeys(afterMasterKeyRotation, newKey)
      } yield ()
    }

    "rotate and fetch actual key" in withDao { dao =>
      val dictId = ModelGenerators.StrGen.next
      for {
        keyToCreate <- generateKey(dictId)
        _ <- dao.actualOrCreate(dictId)(Task(keyToCreate))
        alternativeKey <- generateKey(dictId)
        _ <- dao.rotate(alternativeKey)
        actual <- dao.actualOrCreate(dictId)(Task(keyToCreate))
        _ <- checkKeys(actual, alternativeKey)
      } yield ()
    }

    "fetch old key by id" in withDao { dao =>
      val dictId = ModelGenerators.StrGen.next
      for {
        keyToCreate <- generateKey(dictId)
        _ <- dao.actualOrCreate(dictId)(Task(keyToCreate))
        alternativeKey <- generateKey(dictId)
        _ <- dao.rotate(alternativeKey)
        oldKey <- dao.getKey(dictId, keyToCreate.encryptedKeyPair.keyId)
        _ <- checkKeys(oldKey, keyToCreate)
      } yield ()
    }

    "fail to rotate with existent key" in intercept[SecretKeyAlreadyExistException] {
      withDao { dao =>
        val dictId = ModelGenerators.StrGen.next
        for {
          keyToCreate <- generateKey(dictId)
          _ <- dao.actualOrCreate(dictId)(Task(keyToCreate))
          _ <- dao.rotate(keyToCreate)
        } yield ()
      }
    }

    "fail to fetch non-existent key" in intercept[SecretKeyNotFound] {
      withDao { dao =>
        val dictId = ModelGenerators.StrGen.next
        for {
          keyToCreate <- generateKey(dictId)
          _ <- dao.getKey(dictId, keyToCreate.encryptedKeyPair.keyId)
        } yield ()
      }
    }

    "fail to fetch actual key if no one" in intercept[ActualSecretKeyNotFound] {
      withDao { dao =>
        dao.actual(ModelGenerators.StrGen.next)
      }
    }
  }
}
