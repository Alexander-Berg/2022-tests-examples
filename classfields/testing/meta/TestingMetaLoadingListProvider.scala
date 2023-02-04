package ru.yandex.vos2.testing.meta

import ru.yandex.extdata.core.Controller
import ru.yandex.extdata.core.event.Event
import ru.yandex.extdata.core.lego.{Provider, SimpleProvider}
import ru.yandex.realty.context.v2.bunker.{BunkerProviderUpdated, BunkerStorage}
import ru.yandex.realty.loaders.AbstractProvider

import scala.util.Try

class TestingMetaLoadingListProvider(override val controller: Controller, bunkerProvider: Provider[BunkerStorage])
  extends SimpleProvider[TestingMetaLoadingList]
  with AbstractProvider {
  override def build(): Try[TestingMetaLoadingList] = Try {
    new TestingMetaLoadingListStorage(
      bunkerProvider
        .get()
        .get[Seq[String]]("/realty/testing-uids-with-metadata-loading")
        .getOrElse(Seq.empty)
        .toSet
    )
  }

  override def updatedEvent: Event = TestingMetaLoadingListUpdated

  override def eventDependencies: List[Event] = List(BunkerProviderUpdated)
}

object TestingMetaLoadingListUpdated extends Event
