package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.ClientDao.ClientRef
import ru.yandex.vertis.billing.model_core.{CustomerHeader, CustomerId, PartnerRef}
import ru.yandex.vertis.billing.util.{Page, SlicedResult}

import scala.util.Success

/**
  * Specs on [[ClientDao]]
  */
trait ClientDaoSpec extends AnyWordSpec with Matchers {

  protected def clientDao: ClientDao

  protected def customerDao: CustomerDao

  import ru.yandex.vertis.billing.service.ClientService.Query

  "ClientDao" should {
    "find nothing in" in {
      val page = Page(0, 1)
      clientDao.find(Query.All, page) should be(Success(SlicedResult.empty(page)))
    }
    "upsert direct client" in {
      clientDao.upsert(1, None).get should be(ClientRef(1, None, None))
      clientDao.upsert(1, None).get should be(ClientRef(1, None, None))
    }
    "upsert client under agency" in {
      clientDao.upsert(2, Some(5)).get should be(ClientRef(2, Some(5), None))
      clientDao.upsert(2, Some(5)).get should be(ClientRef(2, Some(5), None))
    }
    "upsert agency" in {
      clientDao.upsert(3, Some(3)).get should be(ClientRef(3, Some(3), None))
      clientDao.upsert(3, Some(3)).get should be(ClientRef(3, Some(3), None))
    }
    "provide upserted clients" in {
      val page = Page(0, 10)
      val result = clientDao.find(Query.All, page).get
      result.slice should be(page)
      result.values.map(_.id).toSet should be(Set(1, 2, 3))
    }
    "provide agency" in {
      val page = Page(0, 10)
      val result = clientDao.find(Query.ByType(isAgency = true), page).get
      result.slice should be(page)
      result.values.map(_.id).toSet should be(Set(3))
    }
    "provide direct client" in {
      val page = Page(0, 10)
      val result = clientDao.find(Query.ByType(isAgency = false), page).get
      result.slice should be(page)
      result.values.map(_.id).toSet should be(Set(1))
    }
    "provide client under agency" in {
      val page = Page(0, 10)
      val result = clientDao.find(Query.ForAgency(5), page).get
      result.slice should be(page)
      result.values.map(_.id).toSet should be(Set(2))
    }
    "provide client with resource" in {
      val id = CustomerId(2, Some(5))
      val resourceRef = PartnerRef("123")
      customerDao.create(CustomerHeader(id, resourceRef)).get
      val page = Page(0, 10)
      val result = clientDao.find(Query.ForAgency(5), page).get
      result.slice should be(page)
      result.values.head.resource should be(Some(resourceRef))
    }
  }
}
