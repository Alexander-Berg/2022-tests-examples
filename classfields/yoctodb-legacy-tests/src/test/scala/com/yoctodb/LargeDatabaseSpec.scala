package com.yoctodb

import com.google.common.io.Resources
import com.yandex.yoctodb.DatabaseFormat
import com.yandex.yoctodb.immutable.Database
import com.yandex.yoctodb.mutable.{DatabaseBuilder, DocumentBuilder}
import com.yandex.yoctodb.mutable.DocumentBuilder.IndexOption
import com.yandex.yoctodb.query.{DocumentProcessor, QueryBuilder}
import com.yandex.yoctodb.util.UnsignedByteArrays
import com.yandex.yoctodb.util.buf.{Buffer, FileChannelBuffer}
import com.yandex.yoctodb.v1.V1DatabaseFormat.Feature
import com.yandex.yoctodb.v1.immutable.V1CompositeDatabase
import ru.yandex.common.io.ByteBufferOutputStream
import zio._
import zio.random.Random
import zio.stream.ZStream
import zio.test._

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util
import java.util.Collections
import com.yandex.yoctodb.query.QueryBuilder._

case object LargeDatabaseSpec extends DefaultRunnableSpec {

  val documentGen: Gen[Random, DocumentBuilder] = for {
    src <- Gen.int(0, 70000)
    score <- Gen.short(700, 1000)
    data <- Gen.chunkOfN(6)(Gen.anyByte)
  } yield {
    val doc = DatabaseFormat.getCurrent.newDocumentBuilder()
    doc.withField("src", src, IndexOption.RANGE_FILTERABLE)
    doc.withField("score", score, IndexOption.RANGE_FILTERABLE)
    doc.withField("data", UnsignedByteArrays.from(data.toArray), IndexOption.STORED)
    doc
  }

  val dbGen: Gen[Random, DatabaseBuilder] = {
    Gen.fromEffect {
      for {
        builder <- UIO(DatabaseFormat.getCurrent.newDatabaseBuilder(Feature.LEGACY))
        db <- ZStream
          .range(0, 100000)
          .flatMap(_ => documentGen.sample)
          .fold(builder)((db, sample) => db.merge(sample.value))
      } yield db
    }
  }

  override def spec: ZSpec[environment.TestEnvironment, Any] =
    suite("Test large dataset serde")(
      testM("Generate and serde large database") {
        check(dbGen) { db =>
          val writable = db.buildWritable()
          val sizeInBytes = writable.getSizeInBytes
          println(sizeInBytes)
          val buffer = ByteBuffer.allocate(sizeInBytes.toInt)
          val os = new ByteBufferOutputStream(buffer)
          writable.writeTo(os)
          buffer.rewind()
          val de = DatabaseFormat.getCurrent.getDatabaseReader.from(Buffer.from(buffer))
          assertTrue(de.getDocumentCount > 0)
        }
      }
    )
}
