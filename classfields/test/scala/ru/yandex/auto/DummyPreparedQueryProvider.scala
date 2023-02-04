package ru.yandex.auto

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import ru.yandex.auto.extdata.service.canonical.router.model.CanonicalUrlRequest
import ru.yandex.auto.index.consumer.PreparedQuery
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.vertis.mockito.MockitoSupport

class DummyPreparedQueryProvider[T](entities: Seq[T]) extends Provider[PreparedQuery[T]] with MockitoSupport {

  override def get(): PreparedQuery[T] = {
    val preparedQuery = mock[PreparedQuery[T]]

    when(preparedQuery.foldLeft(?)(?))
      .thenAnswer {
        new Answer[Set[CanonicalUrlRequest]] {
          override def answer(invocation: InvocationOnMock): Set[CanonicalUrlRequest] = {
            entities.foldLeft(invocation.getArgument(0))(invocation.getArgument(1))
          }
        }
      }

    when(preparedQuery.filter(?)).thenAnswer {
      new Answer[PreparedQuery[T]] {
        override def answer(invocation: InvocationOnMock): PreparedQuery[T] = {
          val newEntities = entities.filter(invocation.getArgument(0))

          new DummyPreparedQueryProvider(newEntities).get()
        }
      }
    }

    when(preparedQuery.map(?)).thenAnswer {
      new Answer[PreparedQuery[T]] {
        override def answer(invocation: InvocationOnMock): PreparedQuery[T] = {
          val newEntities = entities.map(invocation.getArgument(0))

          new DummyPreparedQueryProvider(newEntities).get()
        }
      }
    }

    when(preparedQuery.foreach(?)).thenAnswer {
      new Answer[Int] {
        override def answer(invocation: InvocationOnMock): Int = {
          entities.foreach(invocation.getArgument(0))
          entities.size
        }
      }
    }

    when(preparedQuery.toIterable).thenReturn(entities)
    when(preparedQuery.toBuffer).thenReturn(entities.toBuffer)

    preparedQuery
  }
}
