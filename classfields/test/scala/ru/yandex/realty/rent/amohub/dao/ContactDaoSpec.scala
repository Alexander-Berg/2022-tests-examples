package ru.yandex.realty.rent.amohub.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.amohub.model.{ContactPhone, ContactPhoneStatus}
import ru.yandex.realty.sharding.Shard
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ContactDaoSpec extends WordSpecLike with AmohubDaoSpecBase {

  "Contact DAOs" should {
    "create and find phones" in {
      val contacts = contactGen().next(15).toList
      contactDao.create(contacts).futureValue
      contacts.foreach { contact =>
        val findContact = contactDao.findByIdOpt(contact.contactId).futureValue
        assert(findContact.isDefined)
        assert(contact.contactId == findContact.get.contactId)
        contact.phones.foreach(contactPhone => assert(findContact.get.phones.contains(contactPhone)))
      }
    }

    "find by unified phones" in {
      val contacts = contactGen().next(7).toList
      contactDao.create(contacts).futureValue
      contacts.foreach { contact =>
        val findContacts = contactDao.findByUnifiedPhone(contact.phones.head.unifiedPhone.get).futureValue
        assert(findContacts.nonEmpty)
        findContacts.foreach(findContact => assert(contacts.exists(_.contactId == findContact.contactId)))
      }
    }

    "update contact" in {
      val contact = contactGen().next
      val newResponsibleUserId = Some(posNum[Long].next)
      contactDao.create(Seq(contact)).futureValue
      val contactPhone = contactPhoneGen(contact.contactId).next
      val updatedContact = contact.copy(
        updateTime = DateTimeUtil.now,
        phones = Seq(contactPhone),
        responsibleUserId = newResponsibleUserId
      )
      contactDao.update(contact.contactId)(_ => updatedContact).futureValue
      val contactOpt = contactDao.findByIdOpt(contact.contactId).futureValue
      assert(contactOpt.nonEmpty)
      assert(updatedContact == contactOpt.get)
      assert(updatedContact.contactId == contactOpt.get.contactId)
      updatedContact.phones.foreach(contactPhone => assert(contactOpt.get.phones.contains(contactPhone)))
    }

    "watch contacts" in {
      val contacts = contactGen(Some(0)).next(7).toList
      contactDao.create(contacts).futureValue

      val addedContactPhoneMap: Map[Long, ContactPhone] = contacts.map { contact =>
        val addedPhone = contactPhoneGen(contact.contactId)
          .filterNot(cp => contact.phones.exists(_.phone == cp.phone))
          .next
        contact.contactId -> addedPhone
      }.toMap
      val updatedContactPhoneMap: Map[Long, Seq[ContactPhone]] = contacts.map { contact =>
        contact.contactId -> contact.phones.map(_.copy(unifiedPhone = None, status = ContactPhoneStatus.New))
      }.toMap

      contactDao
        .watch(10, Shard(0, 2)) { contact =>
          Future.successful {
            contact.copy(phones = updatedContactPhoneMap(contact.contactId) :+ addedContactPhoneMap(contact.contactId))
          }
        }
        .futureValue

      contacts.foreach { contact =>
        val foundContactOpt = contactDao.findByIdOpt(contact.contactId).futureValue
        assert(foundContactOpt.isDefined)
        val foundContact = foundContactOpt.get

        val expectedContact =
          contact.copy(phones = updatedContactPhoneMap(contact.contactId) :+ addedContactPhoneMap(contact.contactId))
        assert(expectedContact.contactId == foundContact.contactId)
        expectedContact.phones.foreach(contactPhone => assert(foundContact.phones.contains(contactPhone)))
      }
    }
  }
}
