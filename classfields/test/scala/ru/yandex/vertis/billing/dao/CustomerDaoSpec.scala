package ru.yandex.vertis.billing.dao

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CustomerDao.HeaderFilter.{All, ForCustomerAndResourceRef}
import ru.yandex.vertis.billing.dao.CustomerDao.UpdateResponse
import ru.yandex.vertis.billing.dao.CustomerDao.UpdateResponse.Updated
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.util.Page
import ru.yandex.vertis.billing.model_core.gens.CustomerIdGen
import ru.yandex.vertis.billing.model_core.gens.ResourceRefGen
import ru.yandex.vertis.billing.model_core.gens.Producer

import scala.annotation.nowarn
import scala.collection.mutable
import scala.util.Success

/**
  * Specs on [[CustomerDao]]
  */
trait CustomerDaoSpec extends AnyWordSpec with Matchers {

  protected def customerDao: CustomerDao

  private def deleteCustomer(customerId: CustomerId) = {
    customerDao.delete(customerId) should
      be(Success(CustomerDao.DeleteResponse.Deleted(customerId)))

    intercept[NoSuchElementException] {
      customerDao.get(customerId).get
    }

    customerDao.delete(customerId) should
      be(Success(CustomerDao.DeleteResponse.NotExists(customerId)))
  }

  private def checkRemoveResource(
      customerHeader: CustomerHeader,
      resource: ResourceRef,
      expectedHeader: CustomerHeader): Assertion = {
    customerDao.deleteResources(customerHeader.copy(resources = Seq(resource))) should
      be(Success(CustomerDao.UpdateResponse.Updated(expectedHeader)))
  }

  private def checkFindHeader(customerHeader: CustomerHeader, resource: ResourceRef): Assertion = {
    val customerId = customerHeader.id
    val inPlace = customerHeader.resources.contains(resource)
    customerDao.headers(ForCustomerAndResourceRef(customerId, resource)) match {
      case Success(headers) if headers.nonEmpty && inPlace =>
        headers should contain(CustomerHeader(customerId, Seq(resource)))
      case Success(headers) if headers.isEmpty =>
        inPlace shouldBe false
      case Success(headers) if headers.nonEmpty =>
        fail(s"$resource should be removed from ${customerHeader.resources}, headers = $headers")
      case other =>
        fail(s"Unexpected $other")
    }
  }

  @nowarn("msg=discarded non-Unit value")
  private def checkHeaders(customerHeader: CustomerHeader, resource: ResourceRef): Unit = {
    checkFindHeader(customerHeader, resource)
    customerDao.deleteResources(customerHeader.copy(resources = Seq(resource))).get match {
      case Updated(updatedCustomerHeader) =>
        checkFindHeader(updatedCustomerHeader, resource)
      case _ =>
        fail(s"$resource should be removed")
    }
    customerDao.appendResources(customerHeader.copy(resources = Seq(resource))).get
  }

  private def retriveOrFail(response: UpdateResponse): CustomerHeader = response match {
    case Updated(updatedCustomerHeader) =>
      updatedCustomerHeader
    case _ =>
      fail(s"$response should be updated")
  }

  private def checkHeaders(customerHeader: CustomerHeader): Unit = {
    val resources = Seq(PartnerRef("1"), PartnerRef("2"), PartnerRef("3"))
    for (resource <- resources) {
      val updated = retriveOrFail(
        customerDao.appendResources(customerHeader.copy(resources = Seq(resource))).get
      )
      checkHeaders(updated, resource)
    }
    for (resource <- resources) {
      val updated = retriveOrFail(
        customerDao.deleteResources(customerHeader.copy(resources = Seq(resource))).get
      )
      checkHeaders(updated, resource)
    }
  }

  private def checkAllFilter(expected: Seq[CustomerHeader]): Assertion = {
    customerDao.headers(All) match {
      case Success(headers) =>
        expected.forall(headers.toSeq.contains) shouldBe true
      case other => fail(s"Unexpected $other")
    }
  }

  "CustomerDao" should {
    val resource1 = PartnerRef("1")
    val resource2 = PartnerRef("2")
    val resource3 = PartnerRef("3")
    val resources = Seq(resource1, resource2)

    val directCustomerId = CustomerId(1, None)
    val directCustomer = CustomerHeader(directCustomerId, resources)

    val agencyCustomerId = CustomerId(1, Some(2))
    val agencyCustomer = CustomerHeader(agencyCustomerId, resources)

    "create and get direct customer" in {
      customerDao.create(directCustomer) should
        be(Success(directCustomer))

      customerDao.get(directCustomerId) should
        be(Success(directCustomer))

      intercept[IllegalArgumentException] {
        customerDao.create(directCustomer).get
      }
    }

    "create and get customer under agency" in {
      customerDao.create(agencyCustomer) should
        be(Success(agencyCustomer))

      customerDao.get(agencyCustomerId) should
        be(Success(agencyCustomer))

      intercept[IllegalArgumentException] {
        customerDao.create(agencyCustomer).get
      }
    }

    "find customer by client" in {
      customerDao.find(CustomerDao.Query.ForClient(1), Page(0, 10)).map(_.toSet) should
        be(Success(Set(directCustomer, agencyCustomer)))
    }

    "find customer by agency" in {
      customerDao.find(CustomerDao.Query.ForAgency(2), Page(0, 10)).map(_.toSet) should
        be(Success(Set(agencyCustomer)))
    }

    "find all customers" in {
      customerDao.find(CustomerDao.Query.All, Page(0, 10)).map(_.toSet) should
        be(Success(Set(directCustomer, agencyCustomer)))
    }

    "get nothing by unknown customer ID" in {
      intercept[NoSuchElementException] {
        customerDao.get(CustomerId(3, None)).get
      }
    }

    "append resource" in {
      val customerWithAppendedRes =
        directCustomer.copy(resources = directCustomer.resources ++ Seq(resource3))

      customerDao.appendResources(directCustomer.copy(resources = Seq(resource3))) should
        be(Success(CustomerDao.UpdateResponse.Updated(customerWithAppendedRes)))

      val nonExist = CustomerId(555, None)
      customerDao.appendResources(directCustomer.copy(id = nonExist)) should
        be(Success(CustomerDao.UpdateResponse.NotExists(nonExist)))
    }

    "delete resource" in {
      checkRemoveResource(directCustomer, resource3, directCustomer.copy(resources = Seq(resource1, resource2)))
      checkRemoveResource(directCustomer, resource2, directCustomer.copy(resources = Seq(resource1)))
      checkRemoveResource(directCustomer, resource1, directCustomer.copy(resources = Seq.empty))

      checkRemoveResource(agencyCustomer, resource3, agencyCustomer.copy(resources = Seq(resource1, resource2)))
      checkRemoveResource(agencyCustomer, resource2, agencyCustomer.copy(resources = Seq(resource1)))
      checkRemoveResource(agencyCustomer, resource1, agencyCustomer.copy(resources = Seq.empty))

      val nonExist = CustomerId(666, None)
      customerDao.deleteResources(directCustomer.copy(id = nonExist)) should
        be(Success(CustomerDao.UpdateResponse.NotExists(nonExist)))
    }

    "delete customers" in {
      deleteCustomer(agencyCustomerId)
      deleteCustomer(directCustomerId)
    }

    "get header for direct client by ForCustomersAndResourceRef filter" in {
      val directEmptyCustomer = CustomerHeader(directCustomerId, Seq.empty)
      checkHeaders(customerDao.create(directEmptyCustomer).get)
      customerDao.delete(directEmptyCustomer.id).get
    }

    "get headers for agency client by ForCustomersAndResourceRef filter" in {
      val agencyEmptyCustomer = CustomerHeader(agencyCustomerId, Seq.empty)
      checkHeaders(customerDao.create(agencyEmptyCustomer).get)
      customerDao.delete(agencyEmptyCustomer.id).get
    }

    "get headers" in {
      val expected = mutable.ArrayBuffer.empty[CustomerHeader]
      checkAllFilter(expected.toSeq)
      expected += customerDao.create(agencyCustomer).get
      checkAllFilter(expected.toSeq)
      expected += customerDao.create(directCustomer).get
      checkAllFilter(expected.toSeq)
    }

    "check group concat max len" in {
      val id = CustomerIdGen.next
      val resources = (1 to 120).map(id => PartnerRef(id.toString))
      val customer = CustomerHeader(id, resources)
      (customerDao.upsert(customer) should be).a(Symbol("Success"))

      val result = customerDao.get(id).get
      result.id shouldBe id
      result.resources should contain theSameElementsAs resources
    }
  }
}
