package auto.dealers.booking.api.uuid

import java.util.{UUID => jUUID}

import zio.{Has, Ref, UIO, ULayer, URIO, ZIO}

object TestUUID {

  type TestUUID = Has[Service]

  trait Service {
    def feedUUIDs(uuids: jUUID*): UIO[Unit]
    def clearUUIDs: UIO[Unit]
  }

  class Test(buffer: Ref[List[jUUID]]) extends Service with UUID.Service {
    override def feedUUIDs(uuids: jUUID*): UIO[Unit] = buffer.update(_ ::: uuids.toList)

    override def clearUUIDs: UIO[Unit] = buffer.set(Nil)

    override def randomUUID: UIO[jUUID] =
      buffer
        .modify {
          case u :: tail => Some(u) -> tail
          case Nil => None -> Nil
        }
        .map(_.getOrElse(jUUID.randomUUID()))
  }

  val test: ULayer[UUID with TestUUID] =
    Ref
      .make(List.empty[jUUID])
      .map(new Test(_))
      .map(test => Has.allOf[UUID.Service, TestUUID.Service](test, test))
      .toLayerMany

  def feedUUIDs(uuids: jUUID*): URIO[TestUUID, Unit] = ZIO.accessM(_.get.feedUUIDs(uuids: _*))
  def clearUUIDs: URIO[TestUUID, Unit] = ZIO.accessM(_.get.clearUUIDs)
  def randomUUID: URIO[UUID, jUUID] = ZIO.accessM(_.get.randomUUID)
}
