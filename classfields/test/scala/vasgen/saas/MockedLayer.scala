package vasgen.saas

object MockedLayer {

  def fieldConverterLayer(
    fields: Seq[FieldMapping],
  ): ZLayer[Any, Nothing, FieldMappingReaderLayer] =
    ZIO
      .succeed {
        val stub = new FieldMappingStorageStub
        stub.put(fields: _*)
        new FieldMappingReader(stub)
      }
      .toLayer

  def epoch(initCache: Long): ULayer[EpochLayer] = {
    ZLayer.succeed {
      epochService(initCache)
    }
  }

  def epochService(initCache: Long): EpochState.Service = {

    val storage = Ref.make(initCache)

    new EpochState.Service {
      override def clear(): IO[VasgenStatus, Unit] = ???

      override def setCurrentEpoch(epoch: Long): IO[VasgenStatus, Unit] = ???

      override def currentEpoch: IO[UnsupportedDomain, Long] =
        storage.flatMap(_.get)
    }
  }

  def requestConverter: ULayer[RequestConverterServiceLayer] = {
    ZLayer.succeed {
      new RequestConverter.Service {
        override def convert(
          filter: Filter,
          addEpochFilter: Boolean,
        ): ZIO[FieldMappingReaderLayer with Clock with Tracing, List[
          FilterIssue,
        ], FilterQuery] = {
          if (filter.and.nonEmpty)
            ZIO.fail(List(UnsupportedAttributeName("emulated.not.exist.attr")))
          else
            ZIO.succeed(FilterQuery("", "", Nil))
        }
      }
    }
  }

  def httpClient: ULayer[HttpClient] =
    ZLayer.succeed {
      new HttpClient.Service {
        override def doRequest[T](req: Request[T]): Task[Response[T]] = ???

        override def tvmRequest[T](
          req: Request[T],
          destination: Destination,
        ): Task[Response[T]] = ???
      }
    }

  def saasClient: ULayer[SaasClientServiceLayer] =
    ZLayer.succeed {
      new SaasClientService.Service {
        val totalDocCount = 42
        override def getRawDocument(
          pk: String,
        ): RIO[HttpClient with Tracing, Option[TDocument]] = {
          ZIO.none
        }

        override def count(
          requestId: String,
          simpleText: Text,
          filters: FilterQuery,
          softness: Option[Int],
          experiments: Set[Experiment],
        ): RIO[HttpClient with Tracing, CountResponse] =
          ZIO.succeed(CountResponse(totalDocCount))

        override def search(
          requestId: String,
          simpleText: Text,
          filters: FilterQuery,
          paging: Paging,
          sorting: Sorting,
          refineFactors: Seq[ExtendedRefine],
          rankingVectors: Seq[EmbeddedVector],
          userFactors: ExtFactors,
          pruneLimit: Option[Int],
          relevancePruning: RelevancePruning,
          softness: Option[Int],
          groupingParams: Option[GroupingParams],
          debugMode: Debug,
          experiments: Set[Experiment],
        ): RIO[
          HttpClient with Tracing with FieldMappingReaderLayer with Clock with EpochLayer,
          SearchResponse,
        ] = ZIO.fail(new NoSuchFieldError)

        override def statistics(
          requestId: String,
          params: ExtendedStatParams,
          simpleText: Text,
          filters: FilterQuery,
          relevancePruning: RelevancePruning,
          softness: Option[Int],
          experiments: Set[Experiment],
        ): RIO[
          HttpClient with Tracing with FieldMappingReaderLayer with Clock with EpochLayer,
          Statistics,
        ] = ZIO.succeed(Statistics(Seq(Sample("low", 12)), Map()))

      }
    }

}
