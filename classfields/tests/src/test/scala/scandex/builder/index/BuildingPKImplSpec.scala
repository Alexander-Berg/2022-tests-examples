package scandex.builder.index

import scandex.bitset.mutable.LongArrayBitSet
import strict.{PrimitiveTypeTag, Utf8}
import tests.strict.types.anyUtf8
import zio.ZIO
import zio.test.Gen.vectorOfN
import zio.test.{Gen, ZIOSpecDefault, assertTrue, check}

object BuildingPKImplSpec extends ZIOSpecDefault {

  val DocCount = 30

  override def spec =
    suite("BuildingPK")(
      test("add docs and delete some of them") {
        check(
          vectorOfN(DocCount)(anyUtf8.filter(_.value.nonEmpty)).map(_.distinct),
          vectorOfN(DocCount)(
            Gen.weighted((Gen.const(true), 8), (Gen.const(false), 2)),
          ),
        ) { (values, activity) =>
          val keys       = values.zip(activity)
          val pkBuilder  = BuildingPK.create[Utf8]
          val activeDocs = new LongArrayBitSet(keys.size.toLong)

          val allDocs = new LongArrayBitSet(keys.size.toLong)
          allDocs.fill()

          for {
            all <-
              ZIO.foreach(keys.zipWithIndex) { case ((key, isActive), docId) =>
                if (isActive)
                  activeDocs.set(docId.toLong)
                pkBuilder.add(key).as((key, isActive))
              }
            dead = all.filter(!_._2).map(_._1)
            _ <- ZIO.foreachDiscard(dead)(key => pkBuilder.remove(key))
            (pk, sieve) = pkBuilder.build
            _ <- sieve.siftActive(allDocs)
          } yield assertTrue(pk.numberOfDocuments == keys.size.toLong) &&
            assertTrue(pk.pkType == PrimitiveTypeTag.Utf8Type) &&
            assertTrue(activeDocs == allDocs)
        }
      },
    )

}
