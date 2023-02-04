package vs.splice

import bootstrap.test.BootstrapSpec
import scandex.runtime.DatabaseService
import scandex.runtime.result.ResultSet
import strict.*
import strict.PrimitiveTypeTag.{Float32Type, Int8Type}
import zio.*
import zio.stream.ZStream
import zio.test.*

object InterpreterSpec
    extends BootstrapSpec[DatabaseService & Interpreter[DatabaseService]] {

  override val RLive: ZLayer[
    BaseEnv,
    Nothing,
    DatabaseService & Interpreter[DatabaseService],
  ] = {
    ZLayer.succeed {
      new DatabaseService { // dummy
        override def universalSet: URIO[Scope, ResultSet] = ???
      }
    } ++
      ZLayer.fromZIO(
        Interpreter.default,
      ) // ++ Runtime.enableFiberRoots ++ Runtime.logRuntime
  }

  override def spec: Spec[Environment, Any] =
    suite("Interpreter")(
      test("Done") {
        for {
          compiler <- ZIO.service[Interpreter[DatabaseService]]
          out <-
            compiler
              .run(
                ZStream(
                  Init(
                    Seq(
                      Cast("A", Float32Type.asUniversalTag, "B"),
                      Await("C", Float32Type.asUniversalTag),
                      Return("B"),
                      Return("C"),
                    ),
                    Map("A" -> 1.toInt32),
                  ),
                  Set(Map("C" -> 2.toFloat32)),
                ),
              )
              .runCollect
        } yield assertTrue(
          out.toSet ==
            Chunk(Data("B", 1.toFloat32), Data("C", 2.toFloat32), Done())
              .toSet[Reply.NonEmpty],
        )
      } @@ TestAspect.timeout(1.seconds),
      test("Failed") {
        for {
          compiler <- ZIO.service[Interpreter[DatabaseService]]
          out      <- compiler.run(ZStream(Init(Seq(Return("C"))))).runCollect
        } yield assertTrue(out == Chunk(Failed(NoSuchPromise("C").toString)))
      },
      test("Cancel") {
        for {
          compiler <- ZIO.service[Interpreter[DatabaseService]]
          out <-
            compiler
              .run(
                ZStream(
                  Init(Seq(Await("A", Int8Type.asUniversalTag), Return("A"))),
                  Cancel(),
                ),
              )
              .runCollect
        } yield assertTrue(out == Chunk(Failed(Interrupted().toString)))
      },
    )

}
