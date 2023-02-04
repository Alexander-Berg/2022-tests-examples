package ru.yandex.vertis.general.gateway.clients.router.testkit

import ru.yandex.vertis.general.clients.router.{Routable, Route, RouterClient}
import ru.yandex.vertis.general.clients.router.RouterClient.RouterClient
import zio.macros.accessible
import zio.{Has, Ref, Task, UIO, ULayer, ZIO, ZRef}

@accessible
object RouterClientMock {

  type RouterClientMock = Has[Service]

  trait Service {
    def setRouteResponse(response: Any => Task[String]): UIO[Unit]
  }

  val layer: ULayer[RouterClient with RouterClientMock] = {
    val creationEffect = for {
      responseRef <- ZRef.make[Any => Task[String]](_ => ZIO.succeed(""))

      responseSetter: Service = new ServiceImpl(responseRef)
      routerClientTest: RouterClient.Service = new RouterClientTestImpl(responseRef)

    } yield Has.allOf(responseSetter, routerClientTest)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(responseRef: Ref[Any => Task[String]]) extends Service {
    override def setRouteResponse(response: Function[Any, Task[String]]): UIO[Unit] = responseRef.set(response)

  }

  private class RouterClientTestImpl(
      responseRes: Ref[Any => Task[String]])
    extends RouterClient.Service {

    override def getLink[R: Routable](routable: R, isCanonical: Boolean): Task[String] =
      responseRes.get.flatMap(f => f(routable))

    override def getLinks(routes: Seq[Route]): Task[Seq[String]] =
      ZIO.foreach(routes)(r => responseRes.get.flatMap(f => f(r)))

    override def parseRoute(link: String): UIO[Route] = ???
  }
}
