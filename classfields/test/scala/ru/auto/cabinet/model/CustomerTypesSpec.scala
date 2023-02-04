package ru.auto.cabinet.model

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers

class CustomerTypesSpec extends FlatSpec with Matchers {

  it should "get access customer type for manager" in {
    val customerId = CustomerId(20101L, parentAgencyId = None, companyId = None)
    val user = User(123L, Seq(Role.Client, Role.Manager))

    CustomerTypes(customerId, user) shouldBe CustomerTypes.Client
  }

  it should "get access customer type for agency self" in {
    val customerId = CustomerId(101L, agencyId = 101L)
    val user = User(123, Seq(Role.Agency))

    CustomerTypes(customerId, user) shouldBe CustomerTypes.Agency
  }

  it should "get access customer type for agency client" in {
    val customerId = CustomerId(20101L, agencyId = 101L)
    val user = User(123, Seq(Role.Agency))

    CustomerTypes(customerId, user) shouldBe CustomerTypes.Client
  }

  it should "get access customer type for company group" in {
    val customerId = CustomerId(20101L, companyId = 100L)
    val user = User(123, Seq(Role.Company))

    CustomerTypes(customerId, user) shouldBe CustomerTypes.Client
  }

  it should "get access customer type for client" in {
    val customerId = CustomerId(20101L, agencyId = 101L)
    val user = User(123, Seq(Role.Client))

    CustomerTypes(customerId, user) shouldBe CustomerTypes.Client
  }

  it should "fail on get access customer type on unknown type" in {
    val customerId = CustomerId(20101L, agencyId = 101L)
    val user = User(123, roles = Seq())

    a[NoSuchElementException] should be thrownBy CustomerTypes(customerId, user)
  }

}
