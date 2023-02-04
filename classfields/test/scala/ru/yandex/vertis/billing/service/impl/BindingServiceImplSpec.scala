package ru.yandex.vertis.billing.service.impl

import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.gens.CampaignSourceGen
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcBindingDao, JdbcCampaignDao, JdbcCustomerDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core.gens.{CustomerHeaderGen, OrderPropertiesGen, Producer}
import ru.yandex.vertis.billing.model_core.{CampaignHeader, CustomerId}
import ru.yandex.vertis.billing.service.BindingServiceSpec

import scala.util.Try

/**
  * Runnable spec on [[ru.yandex.vertis.billing.dao.impl.jdbc.JdbcBindingDao]]
  *
  * @author alex-kovalenko
  */
class BindingServiceImplSpec extends BindingServiceSpec with JdbcSpecTemplate {

  val service = new JdbcBindingDao(billingDatabase)

  private val customerDao = new JdbcCustomerDao(billingDatabase)
  private val orderDao = new JdbcOrderDao(billingDualDatabase)
  private val campaignDao = new JdbcCampaignDao(billingDatabase)

  override def campaignFor(customerId: CustomerId): CampaignHeader =
    (for {
      customer <- Try(CustomerHeaderGen.next.copy(id = customerId))
      _ <- customerDao.upsert(customer)
      orderProps = OrderPropertiesGen.next
      order <- orderDao.create(customer.id, orderProps)
      source = CampaignSourceGen.next.copy(orderId = order.id)
      campaign <- campaignDao.create(customerId, source, DuplicationPolicy.AllowDuplicates)
    } yield campaign).get
}
