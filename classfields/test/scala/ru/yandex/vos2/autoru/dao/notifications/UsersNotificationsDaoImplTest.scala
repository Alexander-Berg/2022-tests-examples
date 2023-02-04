package ru.yandex.vos2.autoru.dao.notifications

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.model.UserRef

/**
  * Created by sievmi on 18.03.19
  */
class UsersNotificationsDaoImplTest extends AnyFunSuite with InitTestDbs with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initDbs()
  }

  private val dao = components.notificationsDao

  test("CRUD") {
    val userRef = UserRef.from("ac_123")
    val notificationType = NotificationType.U_DEALER_NEW_LOW_PRICE_BAN

    assert(dao.getNotSent(userRef, notificationType).isEmpty)

    assert(dao.append(userRef, notificationType, 100L)(UsersNotificationsDao.emptyMergeFunction))
    assert(dao.getNotSent(userRef, notificationType).nonEmpty)
  }

  test("action with identity function don change notification") {
    val userRef = UserRef.from("ac_123")
    val notificationType = NotificationType.U_DEALER_NEW_LOW_PRICE_BAN

    assert(dao.append(userRef, notificationType, 100L)(UsersNotificationsDao.emptyMergeFunction))
    val oldV = dao.getNotSent(userRef, notificationType)
    dao.doActionWithReady(100, _ => true) { notification =>
      notification
    }
    val newV = dao.getNotSent(userRef, notificationType)

    assert(newV === oldV)
  }

  test("action with update inc retry count") {
    val userRef = UserRef.from("ac_123")
    val notificationType = NotificationType.U_DEALER_NEW_LOW_PRICE_BAN

    assert(dao.append(userRef, notificationType, 100L)(UsersNotificationsDao.emptyMergeFunction))
    val oldV = dao.getNotSent(userRef, notificationType)
    dao.doActionWithReady(100, _ => true) { notification =>
      notification.copy(retryCount = notification.retryCount + 1)
    }
    assert(dao.getNotSent(userRef, notificationType).get.retryCount === 1)
    dao.doActionWithReady(100, _ => true) { notification =>
      notification.copy(retryCount = notification.retryCount + 1)
    }
    assert(dao.getNotSent(userRef, notificationType).get.retryCount === 2)
  }

  test("append to no sent notification") {
    val userRef = UserRef.from("ac_123")
    val notificationType = NotificationType.U_DEALER_NEW_LOW_PRICE_BAN

    assert(dao.append(userRef, notificationType, 100L)(UsersNotificationsDao.emptyMergeFunction))
    val oldV = dao.getNotSent(userRef, notificationType)
    assert(dao.append(userRef, notificationType, 100L)(UsersNotificationsDao.emptyMergeFunction))
    val newV = dao.getNotSent(userRef, notificationType)

    assert(oldV.get.id === newV.get.id)
  }

  test("merge function") {
    pending
    val userRef = UserRef.from("ac_123")
    val notificationType = NotificationType.U_DEALER_NEW_LOW_PRICE_BAN

    def serialise(value: Any): Array[Byte] = {
      val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(stream)
      oos.writeObject(value)
      oos.close()
      stream.toByteArray
    }

    def deserialise(bytes: Array[Byte]): Any = {
      val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
      val value = ois.readObject
      ois.close()
      value
    }

    def mergeFunction(a: Array[Byte], b: Array[Byte]): Array[Byte] = {
      val set1 = deserialise(a).asInstanceOf[Set[Int]]
      val set2 = deserialise(b).asInstanceOf[Set[Int]]

      serialise(set1 ++ set2)
    }

    assert(dao.append(userRef, notificationType, 100L, serialise(Set.empty[Int]))(mergeFunction))
  }
}
