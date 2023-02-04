package ru.yandex.vertis.general.search.testkit

import ru.yandex.vertis.general.search.logic.VasgenSender
import vertis.vasgen.common.DomainId
import vertis.vasgen.document.RawDocument
import zio.macros.accessible
import zio.{Chunk, Has, Ref, Task, UIO, ULayer, ZLayer}

class TestVasgenSender(ref: Ref[Chunk[RawDocument]]) extends VasgenSender.Service with TestVasgenSender.Service {

  override def send(domain: DomainId, documents: Chunk[RawDocument]): Task[Unit] = {
    ref.update(_.prependedAll(documents))
  }

  def get: UIO[Chunk[RawDocument]] = ref.get
}

@accessible
object TestVasgenSender {

  trait Service {
    def get: UIO[Chunk[RawDocument]]
  }

  val test: ULayer[Has[VasgenSender.Service] with Has[Service]] =
    Ref
      .make[Chunk[RawDocument]](Chunk.empty)
      .map { ref =>
        val sender = new TestVasgenSender(ref)
        Has.allOf(sender: VasgenSender.Service, sender: Service)
      }
      .toLayerMany
}
