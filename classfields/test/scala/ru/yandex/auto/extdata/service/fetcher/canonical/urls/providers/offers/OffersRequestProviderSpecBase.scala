package ru.yandex.auto.extdata.service.fetcher.canonical.urls.providers.offers

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.DummyPreparedQueryProvider
import ru.yandex.auto.index.consumer.PreparedQuery
import ru.yandex.vertis.mockito.MockitoSupport

class OffersRequestProviderSpecBase[T] extends WordSpecLike with Matchers with MockitoSupport {

  protected def preparedQuery(messages: Seq[T]): PreparedQuery[T] =
    new DummyPreparedQueryProvider[T](messages).get()

}
