package ru.auto.salesman.billing.impl

import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.auto.salesman.billing.{BootstrapClient, BootstrapClientSpec}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.model.Dsl

import scala.collection.JavaConverters.setAsJavaSetConverter

/** Runnable spec on [[HttpBootstrapClient]]
  *
  * @author alex-kovalenko
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class HttpBootstrapClientSpec extends BootstrapClientSpec {

  val baseApiUrl = "http://alex-kovalenko-01-sas.dev.vertis.yandex.net:34150"

  def getClient(domain: String): BootstrapClient =
    HttpBootstrapClient(baseApiUrl, domain)

  lazy val product: Model.Product = Dsl.product(
    Set(
      Dsl.custom(
        "placement",
        Dsl.costPerIndexingDynamic(
          Dsl.constraints(Model.CostType.COSTPERINDEXING, -1, -1, -1)
        ),
        null,
        0
      )
    ).asJava,
    null
  )

  lazy val orderId: Long = ??? // 59696L
  lazy val clientId: Long = ??? // 8217239L
  lazy val (hasAgency, agencyId): (Boolean, Long) = ??? // false, 0L
  lazy val customerId: Model.CustomerId =
    Dsl.customer(clientId, hasAgency, agencyId)

  lazy val resource: Model.Resource =
    Dsl.userResource(Dsl.autoruUid("salesman"))

  private val defaultProductKey = "default"

  lazy val source: Model.BootstrapCampaignSource = Dsl.bootstrapCampaignSource(
    customerId,
    resource,
    orderId,
    product,
    defaultProductKey
  )

}
