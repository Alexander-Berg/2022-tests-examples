package ru.yandex.vertis.subscriptions.service.impl

import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.model.owner.Owner
import ru.yandex.vertis.subscriptions.service.OwnerLinksService

import scala.concurrent.Future

/**
  * Runnable specs on [[LinkedSubscriptionService]] when
  * there are no links.
  *
  * @author dimas
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class NoLinksSubscriptionServiceSpec extends LinkedSubscriptionServiceSpecBase {

  lazy val ownerLinksService: OwnerLinksService = new OwnerLinksService {

    def find(alias: Owner): Future[Owner] =
      Future.failed(new NoSuchElementException)
  }
}
