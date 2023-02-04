package ru.auto.cm_sink.storage.testkit

import zio.{stream, Has, Task, URLayer, ZLayer}
import zio.test.mock.{mockable, Mock}
import zio.sqs.producer.{ErrorOrEvent, Producer, ProducerEvent}
import zio.stream.{ZSink, ZStream}
import zio.test.mock

object StringProducerMock extends Mock[Has[Producer[String]]] {

  object Produce extends Effect[ProducerEvent[String], Throwable, ProducerEvent[String]]

  val compose: URLayer[Has[mock.Proxy], Has[Producer[String]]] =
    ZLayer.fromService { proxy =>
      new Producer[String] {
        override def produce(e: ProducerEvent[String]): Task[ProducerEvent[String]] = proxy(Produce, e)

        override def produceBatch(es: Iterable[ProducerEvent[String]]): Task[Iterable[ProducerEvent[String]]] = ???

        override def sendStream: stream.Stream[Throwable, ProducerEvent[String]] => ZStream[Any, Throwable, ProducerEvent[String]] =
          ???

        override def sendSink: ZSink[Any, Throwable, Iterable[ProducerEvent[String]], Nothing, Unit] = ???

        override def produceBatchE(es: Iterable[ProducerEvent[String]]): Task[Iterable[ErrorOrEvent[String]]] = ???

        override def sendStreamE: stream.Stream[Throwable, ProducerEvent[String]] => ZStream[Any, Throwable, ErrorOrEvent[String]] =
          ???
      }
    }
}
