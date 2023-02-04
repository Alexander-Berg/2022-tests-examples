package ru.yandex.realty.rent.amohub.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.amohub.model.ContactLead

@RunWith(classOf[JUnitRunner])
class LeadDaoSpec extends WordSpecLike with AmohubDaoSpecBase {

  "Lead DAOs" should {
    "create lead with contact" in {
      val contact = contactGen().next
      contactDao.create(Seq(contact)).futureValue
      val lead = leadGen.next
      val contactLead = ContactLead(contact.contactId, lead.leadId, isMainContact = true)
      leadDao.createWithContact(lead, contactLead).futureValue
      val findContact = contactDao.findByIdOpt(contact.contactId).futureValue
      assert(findContact.isDefined)
      assert(contact.contactId == findContact.get.contactId)
      contact.phones.foreach(contactPhone => assert(findContact.get.phones.contains(contactPhone)))
      val findLead = leadDao.findByIdOpt(lead.leadId).futureValue
      assert(lead == findLead.get)
      val findContactLead = contactLeadDao.getByLeadId(lead.leadId).futureValue
      assert(findContactLead.size == 1)
      assert(findContactLead.head == contactLead)
    }
  }

  "LeadDao.findByShowingIds" should {
    "return defined lead for available flat and showing ids" in new Data {
      // setup
      leadDao.create(sampleLeads).futureValue

      // test cases
      leadDao.findByShowingIds(Seq(sampleShowingId0_0)).futureValue.head.leadId shouldEqual l0.leadId
      leadDao.findByShowingIds(Seq(sampleShowingId0_1)).futureValue.head.leadId shouldEqual l1.leadId
      leadDao.findByShowingIds(Seq(sampleShowingId1_0)).futureValue.head.leadId shouldEqual l2.leadId
      leadDao.findByShowingIds(Seq(sampleShowingId1_1)).futureValue.head.leadId shouldEqual l3.leadId
    }
  }

  trait Data {
    val sampleFlatId0: String = readableString.next
    val sampleFlatId1: String = readableString.next
    val sampleShowingId0_0: String = readableString.next
    val sampleShowingId0_1: String = readableString.next
    val sampleShowingId1_0: String = readableString.next
    val sampleShowingId1_1: String = readableString.next

    val sampleLeads @ List(l0, l1, l2, l3, _, _) = List(
      leadGen.next.copy(leadId = posNum[Long].next, flatId = Some(sampleFlatId0), showingId = Some(sampleShowingId0_0)),
      leadGen.next.copy(leadId = posNum[Long].next, flatId = Some(sampleFlatId0), showingId = Some(sampleShowingId0_1)),
      leadGen.next.copy(leadId = posNum[Long].next, flatId = Some(sampleFlatId1), showingId = Some(sampleShowingId1_0)),
      leadGen.next.copy(leadId = posNum[Long].next, flatId = Some(sampleFlatId1), showingId = Some(sampleShowingId1_1)),
      // with flat ids and without showing ids
      leadGen.next.copy(flatId = Some(sampleFlatId0), showingId = None),
      leadGen.next.copy(flatId = Some(sampleFlatId1), showingId = None)
    )
  }
}
