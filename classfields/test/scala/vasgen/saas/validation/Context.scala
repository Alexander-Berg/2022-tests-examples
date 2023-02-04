package vasgen.saas.validation

import bootstrap.config.Setup
import bootstrap.logging.Logging
import bootstrap.metrics.Metrics
import bootstrap.tracing.Tracing
import vasgen.core.saas.FactorKeeper
import vasgen.core.test.util.FieldMappingStorageStub
import zio._
import zio.blocking.Blocking
import zio.clock.Clock

object Context extends Logging {
  val tracing  = Tracing.noop
  val metrics  = Metrics.live
  val blocking = Blocking.live
  val clock    = Clock.live

  def fieldValidatorLayer[S <: Setup[_] : Tag]
    : ZLayer[Has[FactorKeeper.Service[S]], Nothing, Has[DocumentValidator[S]]] =
    ZLayer.fromService[FactorKeeper.Service[S], DocumentValidator[S]](keeper =>
      DocumentValidator[S](
        new FieldMappingStorageStub,
        keeper,
        DocumentValidator.Config("g"),
      ),
    )

}
