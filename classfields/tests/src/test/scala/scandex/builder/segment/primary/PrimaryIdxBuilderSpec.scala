package scandex.builder.segment.primary

import scandex.db.index.DocumentId
import strict.Utf8
import zio.ZIO
import zio.test.*

object PrimaryIdxBuilderSpec extends ZIOSpecDefault {

  val insertions: Seq[Utf8] = Seq(
    "dinner",
    "acid",
    "banana",
    "god",
    "cat",
    "hill",
    "evening",
    "food",
  )

  override def spec: Spec[TestEnvironment, Any] =
    suite("PrimaryIdxBuilder")(
      test("PrimaryIdxBuilder.insert") {
        val builder = PrimaryIdxBuilder.empty[Utf8]
        val ids     = insertions.map(utf8 => builder.add(utf8))

        val index = builder.build()

        for {
          values  <- ZIO.foreach(ids)(id => index.getPrimaryKey(id))
          lookups <- ZIO.foreach(insertions)(pk => index.getDocumentId(pk))
        } yield assertTrue(
          ids == insertions.indices.map(id => DocumentId(id.toLong)),
        ) && assertTrue(values == insertions) && assertTrue(lookups == ids)
      },
    )

}
