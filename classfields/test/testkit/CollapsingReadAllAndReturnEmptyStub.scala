package ru.vertistraf.cost_plus.builder.auto.testkit

import ru.vertistraf.cost_plus.builder.auto.service.Collapsing
import ru.vertistraf.cost_plus.builder.auto.service.Collapsing.Collapsing
import ru.vertistraf.cost_plus.builder.model.thumb.CostPlusThumb
import ru.vertistraf.cost_plus.builder.reducer.CostPlusReducer
import ru.vertistraf.cost_plus.model.ServiceOffer.Auto
import ru.vertistraf.cost_plus.model.ServiceSetInfo.AutoSetInfo
import zio._
import zio.stream.ZSink

object CollapsingReadAllAndReturnEmptyStub extends Collapsing.Service {

  override def collapseCars(
      key: CostPlusReducer.UrlAsKey[AutoSetInfo.Collapse],
      cars: stream.Stream[Throwable, Auto.Car]): Task[Seq[CostPlusThumb.Auto]] =
    cars.run(ZSink.drain).as(Seq.empty)

  val layer: ULayer[Collapsing] = ZLayer.succeed(this)
}
