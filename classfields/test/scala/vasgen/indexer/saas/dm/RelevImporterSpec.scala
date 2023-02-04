package vasgen.indexer.saas.dm

object RelevImporterSpec extends ZIOSpecDefault {
  type TestImporter = RelevImporter[TestSetup, TestSetup]

  private val emptyVersionExpectation =
    DmClientMock.ClusterService(value(ServiceName("test_service"))) ++
      DmClientMock.GetVersion(
        equalTo(
          (
            RtyServer,
            ServiceName("test_service"),
            RelevImporterContext.relevConf,
          ),
        ),
        value(None),
      )

  private val sameVersionExpectation =
    (DmClientMock.ClusterService(value(ServiceName("test_service"))) ++
      DmClientMock.GetVersion(
        equalTo(
          (
            RtyServer,
            ServiceName("test_service"),
            RelevImporterContext.relevConf,
          ),
        ),
        value(Some(11)),
      ) ++ StateKeeperMock.Current(value(11)))

  private val configVersionIsChangedExpectation =
    (DmClientMock.ClusterService(value(ServiceName("test_service"))) ++
      DmClientMock.GetConfig(
        equalTo(
          (
            RtyServer,
            ServiceName("test_service"),
            RelevImporterContext.relevConf,
          ),
        ),
        value(ConfigData(Samples.completeRelevConf, Some(12))),
      ) ++
      FactorStorageMock.Store(
        hasField[(Int, Seq[Factor]), Int]("version", _._1, equalTo(12)) &&
          hasField[(Int, Seq[Factor]), Seq[Factor]](
            "factors",
            _._2,
            hasSameElements(
              Seq(
                Factor(Factor.Dynamic, SaasName("LongQuery"), 0, None),
                Factor(Factor.Dynamic, SaasName("InvWordCount"), 1, None),
                Factor(
                  Factor.Static,
                  SaasName("f_general_no_photo"),
                  50,
                  Some(1.0),
                ),
                Factor(Factor.User, SaasName("sort_hash"), 21, None),
                Factor(Factor.User, SaasName("dssm_dot_product"), 24, Some(0)),
                Factor(
                  Factor.RtyDynamic,
                  SaasName("_Time__f_f_offer_publish_date_g"),
                  31,
                  None,
                ),
                Factor(
                  Factor.Zone,
                  SaasName("_BM25F_Sy_z_offer_title_g"),
                  61,
                  None,
                ),
              ),
            ),
          ),
        unit,
      ) ++ StateKeeperMock.Set(equalTo(12), unit))

  private val differentVersionExpectation =
    (DmClientMock.ClusterService(value(ServiceName("test_service"))) ++
      DmClientMock.GetVersion(
        equalTo(
          (
            RtyServer,
            ServiceName("test_service"),
            RelevImporterContext.relevConf,
          ),
        ),
        value(Some(12)),
      ) ++ StateKeeperMock.Current(value(11)) ++
      configVersionIsChangedExpectation)

  override def spec = {
    suite("RelevImporterSpec")(
      test("configVersionIsChanged")(
        assertZIO(
          (
            for {
              service <- ZIO.service[TestImporter]
              _       <- service.configVersionIsChanged
            } yield ()
          ),
        )(isUnit),
      ).provideLayer(
        (Clock.live ++ configVersionIsChangedExpectation) >+>
          RelevImporterContext.live,
      ),
      test("compareAndApplyVersion: fails on empty version")(
        assertZIO(
          (
            for {
              service <- ZIO.service[TestImporter]
              _       <- service.compareAndApplyVersion
            } yield ()
          ).run,
        )(fails(equalTo(VasgenEmptyValue))),
      ).provideLayer(
        (Clock.live ++ emptyVersionExpectation ++ FactorStorageMock.empty ++
          StateKeeperMock.empty) >+> RelevImporterContext.live,
      ),
      test("compareAndApplyVersion: do nothing on same versions")(
        assertZIO(
          for {
            service <- ZIO.service[TestImporter]
            _       <- service.compareAndApplyVersion
          } yield (),
        )(isUnit),
      ).provideLayer(
        (Clock.live ++ sameVersionExpectation ++ FactorStorageMock.empty) >+>
          RelevImporterContext.live,
      ),
      test("compareAndApplyVersion: fetch config if versions are not same")(
        assertZIO(
          for {
            service <- ZIO.service[TestImporter]
            _       <- service.compareAndApplyVersion
          } yield (),
        )(isUnit),
      ).provideLayer(
        (Clock.live ++ differentVersionExpectation) >+>
          RelevImporterContext.live,
      ),
    )
  }

}
