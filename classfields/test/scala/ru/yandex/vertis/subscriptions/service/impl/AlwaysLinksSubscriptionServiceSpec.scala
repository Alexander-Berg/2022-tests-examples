package ru.yandex.vertis.subscriptions.service.impl

import java.util.concurrent.ConcurrentHashMap
import org.junit.runner.RunWith
import org.scalatest.Ignore
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import ru.yandex.vertis.subscriptions.model.owner.{Owner, OwnerGenerators}
import ru.yandex.vertis.subscriptions.service.OwnerLinksService

import scala.concurrent.Future

/**
  * Runnable specs on [[LinkedSubscriptionService]] when
  * there are always links.
  *
  * @author dimas
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class AlwaysLinksSubscriptionServiceSpec extends LinkedSubscriptionServiceSpecBase {

  lazy val ownerLinksService: OwnerLinksService = new OwnerLinksService {

    def find(alias: Owner): Future[Owner] =
      Future.successful(shift(alias))
  }

  private val links = new ConcurrentHashMap[Owner, Owner]()

  private def shift(owner: Owner) = {
    val fresh = OwnerGenerators.owner.next
    val old = links.putIfAbsent(owner, fresh)
    if (old != null) {
      old
    } else {
      fresh
    }
  }
}
