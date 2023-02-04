package scandex.builder.db.bench

import scandex.builder.db.DatabaseBuilder
import scandex.model.gen.DocumentGenSchema
import scandex.model.{Document, PrimaryKey}
import zio.*
import zio.test.assertTrue

import java.time.Instant

object Common {

  def build[PK : PrimaryKey : Ordering : Tag](
    n: Int,
  ): ZIO[Ref[Chunk[Document[PK]]], Throwable, Unit] =
    for {
      db        <- DatabaseBuilder.createImpl[PK]
      ref       <- ZIO.service[Ref[Chunk[Document[PK]]]]
      documents <- ref.get.map(_.take(n))
      t1        <- ZIO.attempt(Instant.now().toEpochMilli)
      _         <- ZIO.foreachDiscard(documents)(db.upsert)
      t2        <- ZIO.attempt(Instant.now().toEpochMilli)
      db        <- db.build
      t3        <- ZIO.attempt(Instant.now().toEpochMilli)
      meta      <- ZIO.attempt(db.getMeta)
    } yield {
      val d1      = t2 - t1
      val d2      = t3 - t2
      val tpe     = implicitly[PrimaryKey[PK]].tpe
      val indices = meta.indexes.map(_.name).filter(_.nonEmpty).mkString(",")
      println(
        s"Inserting a $n documents[$tpe]($indices)  took $d1 ms, building index of ${meta
          .documentsCount} documents took $d2 ms",
      )
    }

  def refLayer[PK : Tag](): ZLayer[Any, Nothing, Ref[Chunk[Document[PK]]]] =
    ZLayer(
      for {
        ref <- Ref.make[Chunk[Document[PK]]](Chunk.empty)
      } yield ref,
    )

  def fillRef[PK : Tag](schema: DocumentGenSchema[PK], n: Int) =
    zio
      .test
      .test(s"creating $n elements") {
        for {
          t0 <- ZIO.attempt(Instant.now().toEpochMilli)
          documents <-
            schema()
              .sample
              .forever
              .take(n.toLong)
              .map(x => x.map(_.value).get)
              .runCollect
          t1  <- ZIO.attempt(Instant.now().toEpochMilli)
          ref <- ZIO.service[Ref[Chunk[Document[PK]]]]
          _   <- ref.set(documents)
        } yield {
          val diff = t1 - t0
          println(s"Creating a $n documents took $diff ms")
          assertTrue(documents.length == n)
        }
      }

}
