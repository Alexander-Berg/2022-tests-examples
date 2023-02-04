package ru.yandex.vertis.billing.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.billing.microcore_model.Dsl
import ru.yandex.vertis.billing.{BootstrapClient, BootstrapClientSpec, Model}

import scala.jdk.CollectionConverters._

/**
  * Runnable spec on [[HttpBootstrapClient]]
  *
  * @author alex-kovalenko
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpBootstrapClientSpec extends BootstrapClientSpec {

  val baseApiUrl = "http://alex-kovalenko-01-sas.dev.vertis.yandex.net:34150"

  def getClient(domain: String): BootstrapClient = {
    HttpBootstrapClient(baseApiUrl, domain)
  }

  lazy val product = Dsl.product(
    Set(
      Dsl.custom(
        "placement",
        Dsl.costPerIndexingDynamic(Dsl.constraints(Model.CostType.COSTPERINDEXING, -1, -1, -1)),
        null,
        0
      )
    ).asJava,
    null
  )

  lazy val orderId: Long = ??? // 59696L
  lazy val clientId: Long = ??? // 8217239L
  lazy val (hasAgency, agencyId): (Boolean, Long) = ??? // false, 0L
  lazy val customerId = Dsl.customer(clientId, hasAgency, agencyId)
  lazy val resource = Dsl.userResource(Dsl.autoruUid("salesman"))

  private val defaultProductKey = "default"
  lazy val source = Dsl.bootstrapCampaignSource(customerId, resource, orderId, product, defaultProductKey)

}
