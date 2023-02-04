package scandex.builder.db.bench

import com.github.luben.zstd.Zstd
import scandex.bitset.mutable.LongArrayBitSet
import scandex.builder.serde.DatabaseBuilderDeserializer
import scandex.db.SimpleDatabase
import scandex.db.index.{DocumentId, StorageIndexImpl}
import strict.{Bytes, PrimitiveTypeTag}
import zio.ZIO
import zio.stream.{ZSink, ZStream}
import zio.test.*

import java.io.*
import java.nio.file.Path
import java.time.Instant

case class Stats(
    var documents: Int = 0,
    var uncompressed: Long = 0L,
    var compressed: Long = 0L,
)

object LoadFromFileSpec extends ZIOSpecDefault {

  val thresholds: Array[Int] = Array(
    10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 20000,
  )

  val file    = "/Users/lesser-daemon/work/tmp/latest.scandex"
  val outfile = "/Users/lesser-daemon/work/tmp/out.scandex"

  override def spec =
    suite("Load from filesystem")(
      test("DatabaseBuilder") {
        for {
          t0 <- ZIO.succeed(Instant.now().toEpochMilli)
          builder <- DatabaseBuilderDeserializer
            .deserialize(ZStream.fromFile(Path.of(file).toFile))
          t1 <- ZIO.succeed(Instant.now().toEpochMilli)
          // Warming up
          _    <- builder.build
          t2   <- ZIO.succeed(Instant.now().toEpochMilli)
          db   <- builder.build
          t3   <- ZIO.succeed(Instant.now().toEpochMilli)
          meta <- ZIO.attempt(db.getMeta)
          t31  <- ZIO.succeed(Instant.now().toEpochMilli)
          size <- ZIO.attempt(db.getSerializedSize)
          t32  <- ZIO.succeed(Instant.now().toEpochMilli)
          fos  <- ZIO.attempt(new FileOutputStream(outfile))
          t4   <- ZIO.succeed(Instant.now().toEpochMilli)
          _    <- db.serializeToStream.run(ZSink.fromOutputStream(fos))
          _    <- ZIO.attempt(fos.close())
          t5   <- ZIO.attempt(Instant.now().toEpochMilli)

          _ <- ZIO.succeed {
            val d1 = t1 - t0
            val d2 = t3 - t2
            val d3 = t5 - t4

            val dd1 = t31 - t3
            val dd2 = t32 - t31
            val dd3 = t4 - t32
            println(
              s"File DB[${builder.tpe}](fields=${meta.indexes.length - 2}, documents=${meta
                .documentsCount}, length=${size}       $dd1 $dd2 $dd3",
            )
            println(
              s"Time to load and deserialize: $d1, time to build: $d2, time to serialize to memory: $d3",
            )
          }
        } yield assertTrue(true)
      } @@ TestAspect.ignore,
      test("SimpleDatabase") {
        for {
          t0 <- ZIO.attempt(Instant.now().toEpochMilli)
          db <- SimpleDatabase
            .deserializer
            .deserialize(ZStream.fromFile(Path.of(file).toFile))
          t1 <- ZIO.attempt(Instant.now().toEpochMilli)
          // Warming up
          t3   <- ZIO.attempt(Instant.now().toEpochMilli)
          meta <- ZIO.attempt(db.getMeta)
          t31  <- ZIO.attempt(Instant.now().toEpochMilli)
          size <- ZIO.attempt(db.getSerializedSize)
          t32  <- ZIO.attempt(Instant.now().toEpochMilli)
          //          fos    <- ZIO.attempt(new FileOutputStream(outfile))
          t4 <- ZIO.attempt(Instant.now().toEpochMilli)
          //          _      <- db.serializeToStream.run(ZSink.fromOutputStream(fos))
          //          _      <- ZIO.attempt(fos.close())
          t5     <- ZIO.attempt(Instant.now().toEpochMilli)
          target <- ZIO.attempt(new LongArrayBitSet(db.documentCount))
          _      <- ZIO.attempt(target.fill())
          _      <- db.sieveIndex.siftActive(target)
          active <- ZIO.attempt(target.iterator.size)
//          _ <-
//            ZIO.foreachDiscard(target.iterator.map(DocumentId(_)).toSeq) { id =>
//              db.getPKIndex[Utf8]
//                .getPrimaryKey(id)
//                .map(pk => println(s"Document($id) PK=$pk"))
//            }
          storage = db
            .getStorageIndex[Bytes]("Offer", PrimitiveTypeTag.BytesType)
            .get
            .asInstanceOf[StorageIndexImpl[Bytes]]
          _ <- ZIO.attempt(
            println(
              s"documents: ${storage.forward.numberOfDocuments} active: $active values: ${storage.values.numOfValues}",
            ),
          )
          _ <-
            ZIO.foreachDiscard(db.indices)(index =>
              ZIO.attempt {
                println(index.getIndexMeta.name)
              },
            )
          total = Stats()
          histograms = {
            val histograms = Array.ofDim[Stats](thresholds.length + 1)
            histograms.indices.foreach(i => histograms(i) = Stats())
            histograms
          }
          index =
            db.getStorageIndex[Bytes]("Offer", PrimitiveTypeTag.BytesType).get
          _ <-
            ZIO.foreachDiscard(
              (0L until meta.documentsCount).map(DocumentId(_)),
            ) { id =>
              for {
                values <- index.values(id)
                _ <-
                  ZIO.foreachDiscard(values) { value =>
                    ZIO.attempt {
                      val compressed = Zstd.compress(value.toByteArray)
                      total.documents += 1
                      val uncompressed = value.length
                      total.uncompressed += uncompressed
                      total.compressed += compressed.length

                      val h = thresholds
                        .zipWithIndex
                        .find(uncompressed < _._1)
                        .map(_._2)
                        .getOrElse(thresholds.length)
                      histograms(h).uncompressed += uncompressed
                      histograms(h).compressed += compressed.length
                      histograms(h).documents += 1
                    }
                  }
              } yield ()

            }
          _ <- ZIO.attempt {
            println(s"Total: $total")
            thresholds
              .indices
              .foreach(i =>
                println(s"Less than ${thresholds(i)}: ${histograms(i)}"),
              )
            println(s"More than 20K: ${histograms(thresholds.length)}\n\n\n")
          }
          _ <-
            ZIO.foreachDiscard(db.getMeta.indexes) { indexMeta =>
              println(s"Index: ${indexMeta.name}")
              ZIO.foreachDiscard(indexMeta.segments)(segmentMeta =>
                ZIO.attempt {
                  println(
                    s"Segment: type ${segmentMeta.meta}, size ${segmentMeta.allocatedSize}",
                  )
                },
              )
            }
          _ <- ZIO.attempt {
            val d1 = t1 - t0

            val d3 = t5 - t4

            val dd1 = t31 - t3
            val dd2 = t32 - t31
            val dd3 = t4 - t32
            println(
              s"File DB[${db.pkIndex.pkType}](fields=${meta.indexes.length - 2}, documents=${meta
                .documentsCount}, active=$active, length=${size}       $dd1 $dd2 $dd3",
            )
            println(
              s"Time to load and deserialize: $d1, time to serialize to memory: $d3",
            )
          }
        } yield assertTrue(true)
      },
    ) @@ TestAspect.sequential @@ TestAspect.ignore

}
