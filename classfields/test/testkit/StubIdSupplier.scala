package ru.vertistraf.cost_plus.builder.auto.testkit

import ru.vertistraf.cost_plus.builder.service.IdSupplier
import ru.vertistraf.cost_plus.builder.service.IdSupplier.IdSupplier
import ru.vertistraf.cost_plus.model.{Id, IdTag}
import zio._
import common.tagged._

object StubIdSupplier extends IdSupplier.Service {

  lazy val Returning: Id = tag[IdTag][String](ReturningString)
  lazy val ReturningString: String = "1"

  val layer: ULayer[IdSupplier] = ZLayer.succeed(this)

  override def get(string: String): UIO[Id] = ZIO.succeed(Returning)
}
