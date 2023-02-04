//package vasgen.core.saas.db
//
//import bootstrap.logging.Logging
//import ru.yandex.vertis.ydb.zio.{TxError, YdbZioWrapper}
//import ru.yandex.vertis.ydb.{
//  QueryOptions,
//  YdbQuerySyntaxVersion,
//  YdbWrapperContainer,
//}
//import vasgen.core.VasgenErrorContainer
//import vasgen.core.saas.db
//import zio.clock.Clock
//import zio.test.Assertion._
//import zio.test._
//import zio._
//
//import scala.concurrent.duration.DurationInt
//
//object FieldMappingStorageSpec extends ZIOSpecDefault with Logging {
//  import SaasFieldMetaSamples._
//
//  val suites =
//    suite("FieldConverterStorage.Service")(
//      test("Check state of empty database")(
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          state   <- storage.getTableState
//        } yield assert(state)(equalTo(FieldMappingStorage.TableState.NoTable)),
//      ),
//      test("Check state of table.V1")(
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          _       <- storage.createTableV1
//          state   <- storage.getTableState
//          _       <- storage.dropTable
//        } yield assert(state)(equalTo(FieldMappingStorage.TableState.TableV1)),
//      ),
//      test("migrate from v1")(
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          _       <- storage.createTableV1
//          state1  <- storage.getTableState
//          _       <- storage.storeV1(Seq(a1str, b1i32, c1str, d1i64))
//          _       <- storage.migrateV1
//          fields <- storage.retrieve(
//            1,
//            Seq(a1str.name, b1i32.name, "nonexistent", d1i64.name, c1str.name),
//          )
//          state2 <- storage.getTableState
//          _      <- storage.dropTable
//          state3 <- storage.getTableState
//        } yield assert(state1)(
//          equalTo(FieldMappingStorage.TableState.TableV1),
//        ) && assert(state2)(equalTo(FieldMappingStorage.TableState.TableV2)) &&
//          assert(fields)(hasSameElements(Seq(a1str, b1i32, d1i64, c1str))) &&
//          assert(state3)(equalTo(FieldMappingStorage.TableState.NoTable)),
//      ),
//      test("Create empty table twice ")(
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          _       <- storage.createOrMigrate
//          _       <- storage.createOrMigrate
//          state   <- storage.getTableState
//        } yield assert(state)(equalTo(FieldMappingStorage.TableState.TableV2)),
//      ),
//      test("Store batch of new fields")(
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          _       <- storage.store(Seq(a1str, b1i32, c1str, d1i64))
//        } yield assert(())(isUnit),
//      ),
//      test("Epoch 1: Select stored fields") {
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          fields <- storage.retrieve(
//            1,
//            Seq(a1str.name, b1i32.name, "nonexistent", d1i64.name, c1str.name),
//          )
//        } yield assert(fields)(hasSameElements(Seq(a1str, b1i32, d1i64, c1str)))
//      },
//      test("Store batch of existing fields with same epoch")(
//        assertZIO(
//          (
//            for {
//              storage <- ZIO.service[FieldMappingStorage]
//              _       <- storage.store(Seq(a1str, b1i32))
//            } yield ()
//          ).run,
//        )(
//          fails(
//            isSubtype[VasgenErrorContainer[_]](
//              hasField("cause", _.cause, isSubtype[TxError[_]](anything)),
//            ),
//          ),
//        ),
//      ),
//      test("Store batch of existing fields with another epoch")(
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          _       <- storage.store(Seq(a2i32, b2str, e1str))
//        } yield assert(())(isUnit),
//      ),
//      test("Epoch 2: Select stored fields") {
//        for {
//          storage <- ZIO.service[FieldMappingStorage]
//          fields <- storage.retrieve(
//            2,
//            Seq(
//              "nonexistent",
//              a2i32.name,
//              b2str.name,
//              d1i64.name,
//              c1str.name,
//              e1str.name,
//            ),
//          )
//        } yield assert(fields)(hasSameElements(Seq(a2i32, b2str)))
//      },
//    ) @@ TestAspect.sequential @@ TestAspect.ignore
//
//  val table = "test/index/mapping"
//
//  val container: YdbWrapperContainer = YdbWrapperContainer.stable
//
//  private val ydbLayer =
//    ZIO.succeed {
//        container.start()
//
//        YdbZioWrapper.make(
//          container.tableClient,
//          "/local",
//          sessionAcquireTimeout = 3.seconds,
//          QueryOptions(syntaxVersion = YdbQuerySyntaxVersion.V1),
//        )
//      }
//      .toLayer
//
//  private val storageLayer =
//    (
//      for {
//        ydb <- ZIO.service[YdbZioWrapper]
//      } yield db
//        .FieldMappingStorage(ydb, FieldMappingStorage.Config(table = table))
//    ).toLayer
//
//  override def spec: Spec[Environment, Failure] =
//    suites.provideLayerShared(ydbLayer >>> (storageLayer ++ Clock.live)) @@
//      TestAspect.sequential @@ TestAspect.ignore
//
//}
