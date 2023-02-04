package vasgen.indexer.saas.integration.consumer

object ConsumerContext extends Logging {

  val live =
    (Tracing.noop ++ Blocking.live ++ Clock.live ++ Metrics.live ++ keeper) >+>
      (mockLayer ++ launcherLayer ++ producerLayer ++ logbrokerWorker)
  private val tracing  = ZLayer.requires[Tracing]
  private val metrics  = ZLayer.requires[Metrics]
  private val blocking = Blocking.live
  private val clock    = Clock.live
  private val converterConfig = ZLayer
    .succeed(ConverterConfig("vasgen", "general", "ru", "en", 1L, false))
  private val fieldConverterServiceLayer
    : ZLayer[Any, VasgenStatus, FieldMappingConverterLayer] =
    ZIO
      .succeed(
        FieldMappingConverter(
          new FieldMappingStorageStub,
          FieldMappingConverter.Config("g"),
        ),
      )
      .toLayer
  private val keeper = FactorKeeperMock
    .GetFactors(anything, value(Seq.empty))
    .atLeast(1)
  private val rtyState = RtyStateReaderMock
    .Current(value(RtyServerState(false)))
    .atLeast(1)
  private val protoConverterService =
    (converterConfig ++ fieldConverterServiceLayer) >>>
      TMessageConverter.live.orDie
  private val documentConversionServiceLayer =
    (tracing ++ metrics ++ keeper ++ fieldConverterServiceLayer ++ rtyState ++
      protoConverterService) >>>
      DocumentConversionService.live[TestSetup, TestSetup].orDie
  private val launcherLayer = EmbeddedKafkaLauncher.live
  private val consumerProviderLayer =
    EmbeddedKafkaLauncher.consumerProviderLayer[Setup["INPUT"]].orDie
  private val producerLayer = EmbeddedKafkaLauncher.producerLayer.orDie
  private val logBrokerServiceLayer = LogBrokerServiceMock.live
  private val logbrokerWorker =
    (tracing ++ blocking ++ clock ++ logBrokerServiceLayer ++
      documentConversionServiceLayer ++ consumerProviderLayer) >>>
      LogBrokerWorker.live
  private val mockLayer = LogBrokerServiceMock.mock

}
