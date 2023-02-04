package common.clients.bigb.testkit

import cats.data.NonEmptyList
import common.clients.bigb.BigBrotherClient
import common.clients.bigb.BigBrotherClient.BigBrotherClient
import common.clients.bigb.model.BigBrotherUserId
import ru.yandex.proto.crypta.user_profile.Profile
import zio.macros.accessible
import zio.{Has, Ref, Task, UIO, ULayer}

@accessible
object TestBigBrotherClient {
  type TestBigBrotherClient = Has[Service]

  trait Service {
    def setMapping(mapping: Map[NonEmptyList[BigBrotherUserId], Profile]): UIO[Unit]

    def addMapping(mapping: Map[NonEmptyList[BigBrotherUserId], Profile]): UIO[Unit]

    def deleteMapping(mapping: Set[NonEmptyList[BigBrotherUserId]]): UIO[Unit]
  }

  private class Test(mappingRef: Ref[Map[NonEmptyList[BigBrotherUserId], Profile]])
    extends Service
    with BigBrotherClient.Service {

    override def setMapping(mapping: Map[NonEmptyList[BigBrotherUserId], Profile]): UIO[Unit] =
      mappingRef.set(mapping)

    override def addMapping(mapping: Map[NonEmptyList[BigBrotherUserId], Profile]): UIO[Unit] =
      mappingRef.update(_ ++ mapping)

    override def deleteMapping(mapping: Set[NonEmptyList[BigBrotherUserId]]): UIO[Unit] =
      mappingRef.update(_ -- mapping)

    override def getProfile(bigBrotherUserIds: NonEmptyList[BigBrotherUserId]): Task[Profile] =
      mappingRef.get.map(_(bigBrotherUserIds))
  }

  val layer: ULayer[TestBigBrotherClient with BigBrotherClient] = Ref
    .make(Map.empty[NonEmptyList[BigBrotherUserId], Profile])
    .map { ref =>
      val test = new Test(ref)
      Has.allOf[Service, BigBrotherClient.Service](test, test)
    }
    .toLayerMany
}
