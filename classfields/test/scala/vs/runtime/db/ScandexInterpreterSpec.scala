package vs.runtime.db

import bootstrap.test.BootstrapSpec
import bootstrap.tracing.$
import scandex.runtime.DatabaseService
import strict.*
import vs.runtime.poc.PoCIndex
import vs.runtime.s3.IndexWatcher.State
import vs.runtime.s3.{IndexWatcher, Watcher}
import vs.runtime.scandex.ScandexDatabaseService
import vs.splice.*
import zio.*
import zio.stm.{STM, TMap}
import zio.stream.ZStream
import zio.test.*

import java.time.Instant
import scala.collection.SortedMap

object ScandexInterpreterSpec
    extends BootstrapSpec[DatabaseService & Interpreter[DatabaseService]] {

  override val RLive: ZLayer[
    BaseEnv,
    Nothing,
    DatabaseService & Interpreter[DatabaseService],
  ] = {

    ZLayer.fromZIO(
      for {
        d <- STM.atomically(TMap.empty[Instant, Ref[PoCIndex]])
        i <- STM.atomically(TMap.empty[Instant, Int])
      } yield ScandexDatabaseService(
        pool = d,
        refs = i,
        watcher =
          new Watcher[PoCIndex] {
            val index = PoCIndex(SortedMap(1 -> "ololo"))
            override def watch(): URIO[$, Unit] = ???

            override def getDatabase: UIO[PoCIndex] = ZIO.succeed(index)

            override def getState: UIO[IndexWatcher.State[PoCIndex]] =
              ZIO.succeed(State(index, Instant.now()))
          },
      ),
    )
    ++
    ZLayer.fromZIO(Interpreter.default)
  }

  override def spec: Spec[Environment, Any] =
    suite("Interpreter")(
      test("Universal Count") {
        for {
          compiler <- ZIO.service[Interpreter[DatabaseService]]
          out <-
            compiler
              .run(
                ZStream(
                  Init(instructions =
                    Seq(UniversalSet("B"), Count("B", "C"), Return("C")),
                  ),
                ),
              )
              .runCollect
        } yield assertTrue(out == Chunk(Data("C", 1.toUint64), Done()))
      },
      test("return all rows ") {
        for {
          compiler <- ZIO.service[Interpreter[DatabaseService]]
          out <-
            compiler
              .run(
                ZStream(
                  Init(instructions = Seq(UniversalSet("B"), Return("B"))),
                ),
              )
              .runCollect
        } yield assertTrue(
          out ==
            Chunk(
              Data(
                "B",
                ListValue(values =
                  List(ListValue(values = List(Utf8(value = "ololo")))),
                ),
              ),
              Done(),
            ),
        )
      },
    )

}
