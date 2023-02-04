package ru.yandex.vertis.vsquality.techsupport.dao

import cats.data.Validated
import org.scalacheck.Arbitrary
import cats.instances.list._
import cats.syntax.traverse._
import ru.yandex.vertis.vsquality.techsupport.dao.KeyValueDao.Record
import ru.yandex.vertis.vsquality.techsupport.model.ClientRequestContext
import ru.yandex.vertis.vsquality.techsupport.util.{ClearableKeyValueDaoProvider, SpecBase}
import ru.yandex.vertis.vsquality.techsupport.util.Clearable._
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable.AwaitableSyntax

/**
  * @author potseluev
  */
trait KeyValueDaoSpec[K, V] extends SpecBase with ClearableKeyValueDaoProvider {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._

  protected def keyValueDao: KeyValueDao[F, K, V]

  implicit protected def keyArb: Arbitrary[K]

  implicit protected def valueArb: Arbitrary[V]

  before {
    keyValueDao.clear()
  }

  implicit private val rc: ClientRequestContext = generate()

  "put" should {
    "add new value correctly" in {
      val key: K = generate()
      val value: V = generate()
      implicit val rc: ClientRequestContext = generate()
      keyValueDao.put(key, value).await
      val expectedValueRecord = Record(
        value = value,
        updateTime = rc.processingTime,
        updatedBy = rc.userInfo.userId
      )
      keyValueDao.getAll.await shouldBe Map(key -> expectedValueRecord)
    }

    "replace existent value correctly" in {
      val key: K = generate()
      val value1: V = generate()
      val rc1: ClientRequestContext = generate()
      val value2: V = generate[V](_ != value1)
      val rc2: ClientRequestContext = generate()
      keyValueDao.put(key, value1)(rc1).await
      val expectedValueRecord1 = Record(value1, rc1.processingTime, rc1.userInfo.userId)
      keyValueDao.getAll.await shouldBe Map(key -> expectedValueRecord1)

      keyValueDao.put(key, value2)(rc2).await
      val expectedValueRecord2 = Record(value2, rc2.processingTime, rc2.userInfo.userId)
      keyValueDao.getAll.await shouldBe Map(key -> expectedValueRecord2)
    }
  }

  "getAll" should {
    "get all values correctly" in {
      val data: Map[K, V] = generate[Map[K, V]]().take(100)
      data.toList.traverse { case (k, v) => keyValueDao.put(k, v) }.await
      keyValueDao.getAll.await.map { case (k, v) => (k, v.value) } shouldBe data
    }

    "return empty result if no data" in {
      keyValueDao.getAll.await shouldBe empty
    }
  }

  "putWithValidation" should {
    "add new value correctly" in {
      val key: K = generate()
      val value: V = generate()
      implicit val rc: ClientRequestContext = generate()
      keyValueDao.putWithValidation(key, value)(Validated.valid).await
      val expectedValueRecord = Record(
        value = value,
        updateTime = rc.processingTime,
        updatedBy = rc.userInfo.userId
      )
      keyValueDao.getAll.await shouldBe Map(key -> expectedValueRecord)
    }

    "rollback transaction if state is invalid" in {
      val key: K = generate()
      val value: V = generate()
      implicit val rc: ClientRequestContext = generate()
      val e = new Exception("")
      keyValueDao.putWithValidation(key, value)(_ => Validated.invalid(e)).attempt.await shouldBe Left(e)
      keyValueDao.getAll.await shouldBe Map.empty
    }

    "replace existent value correctly" in {
      val key: K = generate()

      val value1: V = generate()
      val rc1: ClientRequestContext = generate()
      keyValueDao.putWithValidation(key, value1)(Validated.valid)(rc1).await
      val expectedValueRecord1 = Record(value1, rc1.processingTime, rc1.userInfo.userId)
      keyValueDao.getAll.await shouldBe Map(key -> expectedValueRecord1)

      val value2: V = generate[V](_ != value1)
      val rc2: ClientRequestContext = generate()
      keyValueDao.putWithValidation(key, value2)(Validated.valid)(rc2).await
      val expectedValueRecord2 = Record(value2, rc2.processingTime, rc2.userInfo.userId)
      keyValueDao.getAll.await shouldBe Map(key -> expectedValueRecord2)
    }
  }
}
