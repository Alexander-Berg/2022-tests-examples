package ru.yandex.vertis.billing.service.cached

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory
import ru.yandex.vertis.billing.balance.model._
import ru.yandex.vertis.billing.balance.xmlrpc.model.Value
import ru.yandex.vertis.billing.model_core.gens.{ClientGen, Producer}
import ru.yandex.vertis.billing.service.cached.CachedBalanceSpec._

import scala.collection.mutable
import scala.util.{Random, Success, Try}

/**
  * @author @logab
  */
class CachedBalanceSpec extends AnyWordSpec with Matchers {
  val log = LoggerFactory.getLogger(classOf[CachedBalanceSpec])

  val cachedBalance = new DummyBalance with CachedBalance {
    override val support: Cache = new NeverExpireCache()

    override def serviceNamespace: String = "ns"

    override protected def countGetClientsByIdBatchCacheStat(expectedCount: ServiceId, missCount: ServiceId): Unit = ()

  }

  def check(actual: Iterable[Client], expected: Iterable[ClientId]) = {
    val actualMap = actual.map(c => c.id -> c).toMap
    val expectedMap = expected.map(id => id -> idToClient(id)).toMap
    actualMap shouldEqual expectedMap
  }

  "cached balance" should {
    "store in cache" in {
      val ids = 1L to 10000L
      val clients = cachedBalance.getClientsByIdBatch(ids).get.toSet
      check(clients, ids)
      val cached1 = cachedBalance.getClientsByIdBatch(ids.drop(5000)).get.toSet
      check(cached1, ids.drop(5000))
      (clients & cached1) should have size 5000
      val newIds = 5001L to 15000L
      val newCached = cachedBalance.getClientsByIdBatch(newIds).get.toSet
      check(newCached, newIds)
      clients.union(newCached) should have size 15000
      clients.intersect(newCached) should have size 5000

    }
  }
}

object CachedBalanceSpec {
  val idToClient = mutable.Map.empty[ClientId, Client]

  class DummyBalance extends Balance {

    def withCached[K, V](key: K, cache: mutable.Map[K, V], valueGen: () => V): V = {
      cache.getOrElse(
        key, {
          val value = valueGen()
          cache += key -> value
          value
        }
      )
    }

    override def getClientsByIdBatch(ids: Iterable[ClientId]): Try[Iterable[Client]] = {
      Success(Random.shuffle(ids).map(id => withCached(id, idToClient, () => ClientGen.next.copy(id = id))))
    }

    override def updateNotificationUrl(request: NotificationUrlChangeRequest): Try[Unit] = ???

    override def getPassportByLogin(login: String)(implicit operator: OperatorId): Try[Option[ClientUser]] = ???

    override def removeUserClientAssociation(
        clientId: ClientId,
        clientUid: UserId
      )(implicit operator: OperatorId): Try[Unit] = ???

    override def createUserClientAssociation(
        clientId: ClientId,
        clientUid: UserId
      )(implicit operator: OperatorId): Try[Unit] = ???

    override def createOrUpdateOrdersBatch(orders: Seq[Order])(implicit operator: OperatorId): Try[Seq[OrderResult]] =
      ???

    override def updateCampaigns(campaignSpendings: Iterable[CampaignSpending]): Try[Iterable[CampaignSpendingResult]] =
      ???

    override def listClientPassports(id: ClientId)(implicit operator: OperatorId): Try[Iterable[ClientUser]] = ???

    override def createRequest(
        clientId: ClientId,
        paymentRequest: PaymentRequest
      )(implicit operator: OperatorId): Try[PaymentRequestResult] = ???

    override def createClient(request: ClientRequest)(implicit operator: OperatorId): Try[ClientId] = ???

    override def getOrdersInfo(requests: Iterable[OrderRequest]): Try[Iterable[NotifyOrder2]] = ???

    override def getPassportByUid(uid: String)(implicit operator: OperatorId): Try[Option[ClientUser]] = ???

    override def createPerson(request: PersonRequest)(implicit operator: OperatorId): Try[PersonId] = ???

    override def getClientPersons(clientId: ClientId, isPartner: Boolean): Try[Iterable[Person]] = ???

    override def createInvoice(ir: InvoiceRequest)(implicit operatorId: OperatorId): Try[InvoiceId] = ???

    override def getRequestChoices(request: RequestChoices): Try[Value] = ???
  }
}
