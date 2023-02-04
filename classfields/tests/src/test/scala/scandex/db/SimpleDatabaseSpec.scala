package scandex.db

import scandex.db.Reason.{
  NoCommands,
  NoDocuments,
  TooManyConditions,
  UnsupportedValueType,
}
import strict.PrimitiveTypeTag.*
import strict.{Bool, Int32, OptionalValue, Utf8}
import zio.ZIO
import zio.stream.ZStream
import zio.test.Assertion.*
import zio.test.{TestAspect, assert, assertTrue, assertZIO}

import java.io.{File, FileInputStream, InputStream}
import scala.reflect.ClassTag

object SimpleDatabaseSpec extends zio.test.ZIOSpecDefault {

  override def spec =
    suite("DB execution: int32")(
      test("normal execution") {
        for {
          db     <- readDB[Int32]("simpleDB")
          result <- db.electricalTapeExecute(Seq(FilterByPK(Int32(78))))
          noDocsAssert <-
            assertZIO(
              db.electricalTapeExecute(Seq(FilterByPK(Int32(14)))).exit,
            )(fails(equalTo(ExecutionError(NoDocuments))))
        } yield assert(result)(
          hasKeys(
            hasSameElements(
              Seq(
                ("some numbers", Int32Type),
                ("animals", Utf8Type),
                ("content", BytesType),
                ("isSomething", BoolType),
                ("PK", Int32Type),
              ),
            ),
          ),
        ) &&
          assertTrue(
            OptionalValue
              .parseFrom(result(("some numbers", Int32Type)).toByteArray)
              .contains(Int32(78)),
          ) &&
          assertTrue(
            OptionalValue
              .parseFrom(result(("animals", Utf8Type)).toByteArray)
              .contains(Utf8("giraffe")),
          ) &&
          assertTrue(
            OptionalValue
              .parseFrom(result(("content", BytesType)).toByteArray)
              .isEmpty,
          ) &&
          assertTrue(
            OptionalValue
              .parseFrom(result(("isSomething", BoolType)).toByteArray)
              .contains(Bool(true)),
          ) && noDocsAssert
      },
      test("error execution: utf8") {
        for {
          db <- readDB[Utf8]("utf8DB")
          invalidPKFilter <-
            assertZIO(
              db.electricalTapeExecute(Seq(FilterByPK(Int32(14))))
                .mapError(_.reason)
                .exit,
            )(fails(isSubtype[UnsupportedValueType](anything)))

          commandOverFlow <-
            assertZIO(
              db.electricalTapeExecute(
                  Seq(
                    FilterByPK(Int32(14)),
                    FilterByPK(Utf8("id3")),
                    FilterByPK(Int32(15)),
                  ),
                )
                .mapError(_.reason)
                .exit,
            )(
              fails(isSubtype[TooManyConditions](hasField("", _.n, equalTo(3)))),
            )
          noCommand <-
            assertZIO(
              db.electricalTapeExecute(Seq.empty).mapError(_.reason).exit,
            )(fails(equalTo(NoCommands)))
          result <- db.electricalTapeExecute(Seq(FilterByPK(Utf8("id2"))))
        } yield invalidPKFilter && commandOverFlow && noCommand &&
          assert(result)(
            hasKeys(
              hasSameElements(
                Seq(
                  ("int32", Int32Type),
                  ("int8", Int8Type),
                  ("singleString", Utf8Type),
                  ("PK", Utf8Type),
                ),
              ),
            ),
          ) &&
          assertTrue(
            OptionalValue
              .parseFrom(result(("singleString", Utf8Type)).toByteArray)
              .contains(Utf8("tU6JkDTp")),
          )
      },
    ) @@ TestAspect.ignore

  private def readDB[T : ClassTag](
    fileName: String,
  ): ZIO[Any, Throwable, SimpleDatabase[T]] = {
    val fileInputStream: FileInputStream =
      new FileInputStream(new File(getClass.getResource(fileName).getPath))

    ZIO.acquireReleaseWith[Any, Throwable, InputStream](
      ZIO.succeed(fileInputStream),
    )(f => ZIO.succeed(f.close())) { is =>
      SimpleDatabase
        .deserializer
        .deserialize(ZStream.fromInputStream(is))
        .map(_.asInstanceOf[SimpleDatabase[T]])
    }

  }

}
