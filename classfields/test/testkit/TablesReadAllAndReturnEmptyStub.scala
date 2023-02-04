package ru.vertistraf.cost_plus.builder.auto.testkit

import ru.vertistraf.cost_plus.builder.auto.service.Tables
import ru.vertistraf.cost_plus.builder.auto.service.Tables.{Result, Tables}
import ru.vertistraf.cost_plus.builder.reducer.CostPlusReducer
import ru.vertistraf.cost_plus.model.ServiceOffer.Auto
import ru.vertistraf.cost_plus.model.ServiceSetInfo.AutoSetInfo
import zio.stream.ZSink
import zio.{stream, Task, ULayer, ZLayer}

object TablesReadAllAndReturnEmptyStub extends Tables.Service {

  def tableCars(
      key: CostPlusReducer.UrlAsKey[AutoSetInfo.Table],
      offers: stream.Stream[Throwable, Auto.Car]): Task[Result] =
    offers.run(ZSink.drain).as(None)

  val layer: ULayer[Tables] = ZLayer.succeed(this)
}
