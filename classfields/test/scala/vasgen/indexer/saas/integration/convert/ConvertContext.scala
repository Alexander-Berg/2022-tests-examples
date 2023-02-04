package vasgen.indexer.saas.integration.convert

object ConvertContext extends Logging {
  val tracing  = Tracing.noop
  val metrics  = Metrics.live
  val blocking = Blocking.live
  val clock    = Clock.live
  val protoConverterService
    : ZLayer[Any, Nothing, Has[SaasConverter.Service[TMessage]]] =
    ZIO
      .succeed(
        new TMessageConverter(
          ConverterConfig(
            service = "vasgen",
            domain = "g",
            language = "ru",
            language2 = "en",
            namespace = 1,
            enableHighlighting = false,
          ),
          fieldConverter,
        ),
      )
      .toLayer
  val fieldConverterLayer: ZLayer[Any, Nothing, FieldMappingConverterLayer] =
    ZIO.succeed(fieldConverter).toLayer
  val documentConversionServiceLayer =
    (clock ++ blocking ++ tracing ++ metrics ++ fieldConverterLayer ++
      ZLayer.requires[Has[FactorKeeper.Service[TestSetup]]] ++ rtyState ++
      protoConverterService) >>>
      DocumentConversionService.live[TestSetup, TestSetup].orDie
  private val fieldConverter = FieldMappingConverter(
    new FieldMappingStorageStub,
    FieldMappingConverter.Config("g"),
  )
  private val rtyState = RtyStateReaderMock
    .Current(value(RtyServerState(false)))
    .atLeast(1)

}
