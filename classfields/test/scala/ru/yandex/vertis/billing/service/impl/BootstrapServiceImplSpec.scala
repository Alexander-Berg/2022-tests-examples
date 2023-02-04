package ru.yandex.vertis.billing.service.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.{Client, ClientProperties}
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.{
  AutoRuUid,
  BootstrapCampaignSource,
  BootstrapOrderSource,
  CampaignHeader,
  CampaignSettings,
  CostPerIndexing,
  Customer,
  CustomerHeader,
  CustomerId,
  Order,
  OrderProperties,
  Placement,
  Product,
  Uid,
  UserResource,
  UserResourceRef
}
import ru.yandex.vertis.billing.service.CampaignService.Filter.ForCustomer
import ru.yandex.vertis.billing.service.CampaignService.Source
import ru.yandex.vertis.billing.service.{CampaignService, CustomerService, OrderService, ResourceService}
import ru.yandex.vertis.billing.util.AutomatedContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.{Failure, Success}

/**
  * Specs on [[BootstrapServiceImpl]]
  *
  * @author alesavin
  */
class BootstrapServiceImplSpec extends AnyWordSpec with Matchers with MockitoSupport {

  val User1 = AutoRuUid("1")

  val Source1 = BootstrapCampaignSource(
    CustomerId(111L, None),
    UserResource(User1),
    3333L,
    Product(Placement(CostPerIndexing(3L)))
  )
  val User2 = Uid(555L)

  val Source2 = BootstrapCampaignSource(
    CustomerId(222L, Some(333L)),
    UserResource(User2),
    4444L,
    Product(Placement(CostPerIndexing(5L)))
  )

  val OrderSource1 = BootstrapOrderSource(
    CustomerId(111L, None),
    Iterable(UserResource(User1)),
    3333L
  )

  val OrderSource2 = BootstrapOrderSource(
    CustomerId(222L, Some(333L)),
    Iterable(UserResource(User2)),
    4444L
  )

  val props = OrderProperties(OrderProperties.DefaultText, None)

  implicit val ac = AutomatedContext("test")

  val customerService = {
    val m = mock[CustomerService]
    when(m.resources(Source1.customerId)(ac))
      .thenReturn(Success(Seq(Source1.resource)))
    when(m.resources(Source2.customerId)(ac)).thenReturn(Failure(new NoSuchElementException("artificial")))
    when(m.create(Source2.customerId, Source2.resource)(ac)).thenReturn(
      Success(
        Customer(
          Source2.customerId,
          Seq(UserResourceRef(User2)),
          Client(Source2.customerId.clientId, ClientProperties()),
          None,
          Seq(Source2.resource)
        )
      )
    )
    m
  }

  val resourceService = {
    val m = mock[ResourceService]
    when(m.getRef(Source1.resource)(ac)).thenReturn(Success(UserResourceRef(User1)))
    m
  }

  val orderService = {
    val m = mock[OrderService]
    when(m.get(Source1.customerId, Source1.orderId)(ac)).thenReturn(
      Success(
        Order(
          Source1.orderId,
          Source1.customerId,
          props
        )
      )
    )
    when(m.get(Source2.customerId, Source2.orderId)(ac)).thenReturn(Failure(new NoSuchElementException("artificial")))
    when(m.attach(Source2.customerId, Source2.orderId, props)(ac)).thenReturn(
      Success(
        Order(
          Source2.orderId,
          Source2.customerId,
          props
        )
      )
    )
    m
  }

  val campaignService = {
    val m = mock[CampaignService]

    when(m.get(ForCustomer(Source1.customerId))(ac)).thenReturn(
      Success(
        Iterable(
          CampaignHeader(
            "Test1",
            None,
            CustomerHeader(Source1.customerId, Seq(UserResourceRef(User1))),
            Order(
              Source1.orderId,
              Source1.customerId,
              props
            ),
            Source1.product,
            CampaignSettings.Default,
            None
          )
        )
      )
    )

    when(m.get(ForCustomer(Source2.customerId))(ac)).thenReturn(Success(Iterable.empty[CampaignHeader]))

    val source = Source(None, Source2.orderId, Source2.product, CampaignSettings.Default, None, Iterable.empty)
    when(m.create(Source2.customerId, source)(ac)).thenReturn(
      Success(
        CampaignHeader(
          "Test2",
          None,
          CustomerHeader(Source2.customerId, Seq(UserResourceRef(User2))),
          Order(
            Source2.orderId,
            Source2.customerId,
            props
          ),
          Source2.product,
          CampaignSettings.Default,
          None
        )
      )
    )
    m
  }

  protected def bootstrapService = new BootstrapServiceImpl(
    customerService,
    orderService,
    campaignService
  )

  "BootstrapServiceImpl" should {

    "return exist campaign" in {
      bootstrapService.campaign(Source1) match {
        case Success(c) =>
          c.id should be("Test1")
          c.customer.id should be(Source1.customerId)
          c.product should be(Source1.product)
        case other => fail(s"Unexpected $other")
      }
    }
    "create new customer, order, campaign if no exist" in {
      bootstrapService.campaign(Source2) match {
        case Success(c) =>
          c.id should be("Test2")
          c.customer.id should be(Source2.customerId)
          c.product should be(Source2.product)
        case other => fail(s"Unexpected $other")
      }
    }

    "return exist order" in {
      bootstrapService.order(OrderSource1) match {
        case Success(c) =>
          c.id shouldBe OrderSource1.orderId
          c.owner should be(OrderSource1.customerId)
        case other => fail(s"Unexpected $other")
      }
    }
    "create new customer, order if no exist" in {
      bootstrapService.order(OrderSource2) match {
        case Success(c) =>
          c.id shouldBe OrderSource2.orderId
          c.owner should be(OrderSource2.customerId)
        case other => fail(s"Unexpected $other")
      }
    }

  }
}
