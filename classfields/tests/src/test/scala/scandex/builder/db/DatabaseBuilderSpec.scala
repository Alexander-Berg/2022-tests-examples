package scandex.builder.db

import scandex.bitset.mutable.LongArrayBitSet
import scandex.builder.util.BuilderError
import scandex.builder.util.Reason.{DocumentIsDeleted, IndexTypeMismatched}
import scandex.db.DefaultDatabase
import scandex.db.index.DocumentId
import scandex.model.meta.FieldDataTypeMeta.IndexType
import scandex.model.{Document, Field}
import strict.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import scala.collection.SortedSet

object DatabaseBuilderSpec extends ZIOSpecDefault {
  val range: IndexType   = IndexType.RANGE
  val storage: IndexType = IndexType.STORAGE

  val docAv1 = Document[Utf8](
    "a",
    Map("version" -> Field(SortedSet(Int32(1)), storage)),
  )

  val docAv2 = Document[Utf8](
    "a",
    Map(
      "version" -> Field(SortedSet(Int32(2)), storage),
      "cities"  -> Field[Utf8](SortedSet("New York"), range),
    ),
  )

  val docAv3 = Document[Utf8](
    "a",
    Map("version" -> Field(SortedSet(Int32(3)), storage)),
  )

  val docAv4 = Document[Utf8](
    "a",
    Map(
      "version" -> Field(SortedSet(Int32(4)), storage),
      "cities"  -> Field[Utf8](SortedSet.empty, range),
    ),
  )

  val docBwrongVersion = Document[Utf8](
    "b",
    Map("version" -> Field(SortedSet(Int64(2L)), storage)),
  )

  val docBv1 = Document[Utf8](
    "b",
    Map(
      "version" -> Field(SortedSet(Int32(1)), storage),
      "cities"  -> Field[Utf8](SortedSet("Moscow", "Dublin"), range),
    ),
  )

  val docBv2 = Document[Utf8](
    "b",
    Map("version" -> Field(SortedSet(Int32(2)), storage)),
  )

  val docBv3 = Document[Utf8](
    "b",
    Map(
      "version" -> Field(SortedSet(Int32(3)), storage),
      "cities"  -> Field[Utf8](SortedSet("Sochi", "London"), range),
    ),
  )

  val docCv1 = Document[Utf8](
    "c",
    Map(
      "version" -> Field(SortedSet(Int32(1)), storage),
      "cities"  -> Field[Utf8](SortedSet("Tomsk"), range),
    ),
  )

  val docDv1 = Document[Utf8](
    "d",
    Map(
      "version" -> Field(SortedSet(Int32(1)), storage),
      "cities"  -> Field[Utf8](SortedSet("Omsk"), range),
    ),
  )

  val dbLayer = ZLayer(DatabaseBuilder.createImpl[Utf8]).orDie

  override def spec =
    suite("DatabaseBuilder")(
      test("Upsert first document")(
        for {
          db      <- ZIO.service[DatabaseBuilderImpl[Utf8]]
          _       <- db.upsert(docAv1)
          version <- db.getField[Int32](DocumentId(0L), "version")
          schema  <- db.getSchema
        } yield assertTrue(version == List(Int32(1))) &&
          assertTrue(
            schema ==
              PrimitiveTypeTag
                .Utf8Type -> Map("version" -> PrimitiveTypeTag.Int32Type),
          ),
      ),
      test("Don't upsert document with field of wrong type")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _  <- db.upsert(docBwrongVersion)
            } yield ()
          ).exit,
        )(
          fails(
            isSubtype[BuilderError](
              hasField(
                "reason",
                _.reason,
                isSubtype[IndexTypeMismatched](anything),
              ),
            ),
          ),
        ),
      ),
      test("Few upserts")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _  <- db.upsert(docBv1)
              _  <- db.upsert(docAv2)
              _  <- db.upsert(docBv2)
              _  <- db.upsert(docBv3)
              _  <- db.upsert(docAv3)

            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Check that document id=2 does not exist")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _  <- db.getPK(DocumentId(2L))
            } yield ()
          ).exit,
        )(
          fails(
            isSubtype[BuilderError](
              hasField(
                "reason",
                _.reason,
                isSubtype[DocumentIsDeleted](anything),
              ),
            ),
          ),
        ),
      ),
      test("Upserts and remove")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _  <- db.upsert(docDv1)
              _  <- db.upsert(docCv1)
              _  <- db.remove(docDv1.primaryKey)
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Check that document id=2 does not exist")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _  <- db.getPK(DocumentId(2L))
            } yield ()
          ).exit,
        )(
          fails(
            isSubtype[BuilderError](
              hasField(
                "reason",
                _.reason,
                isSubtype[DocumentIsDeleted](anything),
              ),
            ),
          ),
        ),
      ),
      test("Check schema")(
        for {
          db       <- ZIO.service[DatabaseBuilderImpl[Utf8]]
          schema   <- db.getSchema
          a        <- db.getPK(DocumentId(0L))
          b        <- db.getPK(DocumentId(1L))
          aVersion <- db.getField[Int32](DocumentId(0L), "version")
          bVersion <- db.getField[Int32](DocumentId(1L), "version")
          aCities  <- db.getField[Utf8](DocumentId(0L), "cities")
          bCities  <- db.getField[Utf8](DocumentId(1L), "cities")
        } yield assertTrue(
          schema ==
            (
              PrimitiveTypeTag.Utf8Type,
              Map(
                "version" -> PrimitiveTypeTag.Int32Type,
                "cities"  -> PrimitiveTypeTag.Utf8Type,
              ),
            ),
        ) && assertTrue(a == "a".toUtf8) && assertTrue(b == "b".toUtf8) &&
          assertTrue(aVersion == List(Int32(3))) &&
          assertTrue(bVersion == List(Int32(3))) &&
          assertTrue(aCities.isEmpty) &&
          assert(bCities)(hasSameElements(List(Utf8("Sochi"), Utf8("London")))),
      ),
      test("remove document")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _  <- db.remove("a")
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Check document removed")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              a  <- db.getPK(DocumentId(0L))
            } yield a
          ).exit,
        )(
          fails(
            isSubtype[BuilderError](
              hasField(
                "reason",
                _.reason,
                isSubtype[DocumentIsDeleted](anything),
              ),
            ),
          ),
        ),
      ),
      test("Upsert it again")(
        assertZIO(
          (
            for {
              db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _  <- db.upsert(docAv4)
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Check values")(
        for {
          db       <- ZIO.service[DatabaseBuilderImpl[Utf8]]
          a        <- db.getPK(DocumentId(0L))
          aVersion <- db.getField[Int32](DocumentId(0L), "version")
          aCities  <- db.getField[Utf8](DocumentId(0L), "cities")
        } yield assertTrue(a == "a".toUtf8) &&
          assertTrue(aVersion == List(Int32(4))) && assertTrue(aCities.isEmpty),
      ),
      test("build and check meta")(
        for {
          dbBuilder <- ZIO.service[DatabaseBuilderImpl[Utf8]]
          db <- dbBuilder.build.map(_.asInstanceOf[DefaultDatabase[Utf8]])
          target <- ZIO.attempt {
            val target = new LongArrayBitSet(db.documentCount)
            target.fill()
            target
          }
          _ <- db.sieveIndex.siftActive(target)
        } yield {
          val bits = db
            .getMeta
            .indexes
            .find(_.name == "Sieve")
            .flatMap(_.segments.headOption.map(_.getBitset.bits))
            .getOrElse(0L)

          assertTrue(db.getMeta.documentsCount == 4L) &&
          assertTrue(bits == 4L) &&
          assertTrue(target.iterator.toSeq == Seq(0L, 1L, 3L))
        },
      ),
    ).provideLayerShared(dbLayer) @@ TestAspect.sequential

}
