package vasgen.core.saas.db

import scala.concurrent.duration.DurationInt

object FactorStorageSpec extends ZIOSpecDefault with Logging {

  final type MappingConfiguration = Setup[("MAPPING")]
  type ConfiguredFactorStorage    = FactorStorage[MappingConfiguration]

  val suites =
    suite("FactorStorage.Service")(
      test("Create empty table")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              _       <- storage.createTable
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Attempt to create database second time")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              _       <- storage.createTable
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Store batch of factors")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              _ <- storage.store(
                1,
                Seq(inLove, inHate, staticConfused12, dynamicConfused),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Version 1: Select stored factors") {
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              fields  <- storage.retrieve(1)
            } yield fields
          ).exit,
        )(
          succeeds(
            hasSameElements(
              Seq(inLove, inHate, staticConfused12, dynamicConfused),
            ),
          ),
        )
      },
      test("Store batch of existing factors with same version")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              _ <- storage.store(
                1,
                Seq(inLove, inHate, staticConfused34, dynamicConfused),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Version 1: Select stored factors again") {
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              fields  <- storage.retrieve(1)
            } yield fields
          ).exit,
        )(
          succeeds(
            hasSameElements(
              Seq(inLove, inHate, staticConfused34, dynamicConfused),
            ),
          ),
        )
      },
      test("Store batch of existing factors with version epoch")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              _       <- storage.store(2, Seq(inLove, inHate, staticConfused12))
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Version 2: Select stored fields") {
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredFactorStorage]
              fields  <- storage.retrieve(2)
            } yield fields
          ).exit,
        )(succeeds(hasSameElements(Seq(inLove, inHate, staticConfused12))))
      },
    )

  val table                          = "test/index/factors"
  val container: YdbWrapperContainer = YdbWrapperContainer.stable

  private val storageLayer =
    (
      for {
        ydb <- ZIO.service[Service[MappingConfiguration]]
      } yield db.FactorStorage(ydb, FactorStorage.Config(table = table))
    ).toLayer

  override def spec =
    suites.provideLayerShared(
      ydbLayer[MappingConfiguration] >>> (storageLayer ++ Clock.live),
    ) @@ TestAspect.sequential @@ TestAspect.ignore

  private def ydbLayer[S <: Setup[_] : Tag] =
    ZIO
      .succeed {
        container.start()

        Service[S](
          YdbZioWrapper.make(
            container.tableClient,
            "/local",
            sessionAcquireTimeout = 3.seconds,
            QueryOptions(syntaxVersion = YdbQuerySyntaxVersion.V1),
          ),
        )
      }
      .toLayer

}
