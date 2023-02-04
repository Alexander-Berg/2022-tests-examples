package vasgen.grpc

object SearchGrpcImplTest extends ZIOSpecDefault {

  val serviceLayer =
    (
      for {
        (saasClient, epochService, mapping, tracing) <- ZIO.services[
          SaasClientService.Service,
          EpochState.Service,
          FieldMappingReader.Service,
          Tracing.Service,
        ]
        (httpClient, clock, metrics, converter) <- ZIO.services[
          HttpClient.Service,
          Clock.Service,
          Metrics.Service,
          RequestConverter.Service,
        ]
        searcherMetric <- metrics.create[SearchMetric]
      } yield SearchGrpcImpl.Service(
        saasClient,
        epochService,
        mapping,
        tracing,
        httpClient,
        clock,
        converter,
        searcherMetric,
      )
    ).provideLayer(
        MockedLayer.epoch(0L) ++ MockedLayer.fieldConverterLayer(Seq.empty) ++
          MockedLayer.saasClient ++ Tracing.noop ++ MockedLayer.httpClient ++
          MockedLayer.requestConverter ++ Clock.live ++ Metrics.live,
      )
      .toLayer

  val serverLayer =
    ServerLayer
      .fromServiceLayer(io.grpc.ServerBuilder.forPort(9000))(serviceLayer)

  val clientLayer: ZLayer[Server, Nothing, SearchClient] = ZLayer
    .fromServiceManaged { ss: Server.Service =>
      ZManaged.fromEffect(ss.port).orDie >>= { port: Int =>
        val channelBuilder = ManagedChannelBuilder.forTarget(s"localhost:$port")
        channelBuilder.usePlaintext()
        ZioSearch.SearchClient.managed(ZManagedChannel(channelBuilder)).orDie
      }
    }

  val tests: Spec[SearchClient, TestFailure[Throwable], TestSuccess] =
    suite("grpc")(
      test("find object")(
        assertZIO(
          ZioSearch
            .SearchClient
            .findObject(
              FindObjectRequest(
                domain = Some(DomainId.of("test")),
                pk = Some(Raw.u64(1234L)),
                version = 0L,
              ),
            )
            .run,
        )(fails(hasStatusCode(Status.UNKNOWN))),
      ),
      test("empty listing") {
        val filter = Filter(and =
          Seq(
            Filter(op =
              more("emulated.not.exist.attr", Raw.bytes("1212".getBytes)),
            ),
          ),
        )
        assertZIO(
          ZioSearch
            .SearchClient
            .execute(
              ExecutionRequest.of(
                domain = Some(DomainId.of("test")),
                query = Some(
                  Query(
                    plan = Some(ExecutionPlan("general_search")),
                    filter = Some(filter),
                    grouping = None,
                  ),
                ),
              ),
            )
            .orElseFail(new IllegalArgumentException("Test failed")),
        )(equalTo(emptyPayload("emulated.not.exist.attr")))
      },
//      test("count object") {
//        val y: ZIO[Has[SearchClient.ZService[Any, Any]] with Any, TestFailure[Status], TestResult] = for {
//          r: ExecutionResult <- ZioSearch
//            .SearchClient
//            .execute(
//              ExecutionRequest.of(
//                domain = Some(DomainId.of("test")),
//                query = Some(Query(plan = Some(ExecutionPlan("general_count")))),
//              ),
//            ).mapError(e => TestFailure.fail(e))
//        } yield assert(r)(
//          equalTo(
//            ExecutionResult(payload =
//              Some(
//                ru.yandex
//                  .vertis
//                  .sraas
//                  .any
//                  .Any(typeUrl = "", value = ByteString.copyFromUtf8("")),
//              ),
//            ),
//          ),
//        )
//        y
//      }
    )

  override def spec: Spec[TestEnvironment, Any] = {
    tests.provideCustomLayerShared(
      (serverLayer >+> clientLayer).mapError(e => TestFailure.fail(e)),
    )
  }

  private def hasStatusCode(c: Status) =
    hasField[Status, Code]("code", _.getCode, equalTo(c.getCode))

  private def emptyPayload(unsupportedAttr: String) =
    ExecutionResult
      .defaultInstance
      .withPayload(
        SearchOffersResponse
          .defaultInstance
          .withPageStatistics(PageStatistics.of(0, 0))
          .addErrors(UnsupportedAttributeName(unsupportedAttr).asError)
          .toAny,
      )

}
