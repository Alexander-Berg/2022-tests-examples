package ru.yandex.realty.rent.amohub.backend

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.amohub.clients.amocrm.model.{
  ContactEmbedded,
  CustomField,
  CustomSimpleStringValue,
  Contact => AmoContact
}
import ru.yandex.realty.amohub.manager.ContactAmocrmEntitiesMappingUtils
import ru.yandex.realty.amohub.model.{Contact, ContactPhone, ContactPhoneStatus}
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.vertis.util.time.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class AmocrmEntitiesMappingUtilsSpec extends SpecBase {

  "AmocrmEntitiesMappingUtils" should {
    "map raw phones" in new Fixture {
      val amoContact: AmoContact = buildAmoContact(Seq(firstPhone, secondPhone))
      val mappedContact: Contact = mapContactToEntity(amoContact)

      val expectedPhones: Map[String, Option[String]] = Map(firstPhone -> None, secondPhone -> None)
      ensureMappedContactPhones(mappedContact, expectedPhones)
    }

    "merge with non-unified existing phone" in new Fixture {
      val amoContact: AmoContact = buildAmoContact(Seq(firstPhone, secondPhone))
      val existingPhones: Map[String, Option[String]] = Map(firstPhone -> None)
      val existingContact: Contact = buildContact(existingPhones)
      val mappedContact: Contact = mapContactToEntity(amoContact, Some(existingContact))

      val expectedPhones: Map[String, Option[String]] = Map(firstPhone -> None, secondPhone -> None)
      ensureMappedContactPhones(mappedContact, expectedPhones)
    }

    "merge with unified existing phone" in new Fixture {
      val amoContact: AmoContact = buildAmoContact(Seq(firstPhone, secondPhone))
      val existingPhones: Map[String, Option[String]] = Map(firstPhone -> Some(firstUnifiedPhone))
      val existingContact: Contact = buildContact(existingPhones)
      val mappedContact: Contact = mapContactToEntity(amoContact, Some(existingContact))

      val expectedPhones: Map[String, Option[String]] = Map(firstPhone -> Some(firstUnifiedPhone), secondPhone -> None)
      ensureMappedContactPhones(mappedContact, expectedPhones)
    }

    "merge with initially unified phone" in new Fixture {
      val amoContact: AmoContact = buildAmoContact(Seq(firstUnifiedPhone))
      val mappedContact: Contact = mapContactToEntity(amoContact, initiallyUnifiedPhone = Some(firstUnifiedPhone))

      val expectedPhones: Map[String, Option[String]] = Map(firstUnifiedPhone -> Some(firstUnifiedPhone))
      ensureMappedContactPhones(mappedContact, expectedPhones)
    }
  }

  trait Fixture extends ContactAmocrmEntitiesMappingUtils with TracedLogging {

    val firstPhone = "89001234567"
    val firstUnifiedPhone = "+79001234567"
    val secondPhone = "89007654321"

    def buildAmoContact(phones: Seq[String]): AmoContact = {
      val phoneFieldValues = phones.map { phone =>
        CustomField(fieldId = None, fieldCode = Some(PhoneFieldCode), Seq(CustomSimpleStringValue(phone)))
      }
      AmoContact(
        id = 1,
        name = "",
        firstName = None,
        lastName = None,
        responsibleUserId = None,
        createdAt = 0,
        updatedAt = 0,
        customFieldsValues = Some(phoneFieldValues),
        _embedded = ContactEmbedded(Seq.empty, None)
      )
    }

    def buildContact(phonesMap: Map[String, Option[String]]): Contact = {
      val contactPhones = phonesMap.map {
        case (phone, unifiedPhone) =>
          val status = unifiedPhone.map(_ => ContactPhoneStatus.Unified).getOrElse(ContactPhoneStatus.New)
          ContactPhone(1, phone, unifiedPhone, status)
      }.toSeq

      Contact(
        contactId = 1,
        phones = contactPhones,
        deleted = false,
        responsibleUserId = None,
        name = None,
        createTime = DateTimeUtil.now(),
        updateTime = DateTimeUtil.now(),
        visitTime = None,
        shardKey = 1,
        amoResponse = None
      )
    }

    def ensureMappedContactPhones(mappedContact: Contact, expectedPhonesMap: Map[String, Option[String]]): Unit = {
      mappedContact.phones.size shouldEqual expectedPhonesMap.size
      expectedPhonesMap.foreach {
        case (expectedPhone, expectedUnifiedPhone) =>
          mappedContact.phones.exists(_.phone == expectedPhone) shouldEqual true
          mappedContact.phones.find(_.phone == expectedPhone).get.unifiedPhone shouldEqual expectedUnifiedPhone
      }
    }
  }
}
