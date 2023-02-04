package scandex.db.segments.forward.onetoone

import scandex.db.index.{DocumentId, ValueIdx}
import scandex.db.segments.SegmentTool
import scandex.db.segments.constructors.impl.v1.fixed.SingleValueForwardSegmentBuilder
import scandex.db.segments.forward.ForwardSegment
import scandex.db.serde.SegmentDeserializer
import scandex.model.meta.FieldDataTypeMeta.Cardinality
import zio.ZIO
import zio.test.*
import zio.test.Assertion.{anything, fails, hasSameElements, isSubtype}
import zio.test.Gen.{int, listOfN, long}

object SingleForwardSegmentSpec extends ZIOSpecDefault {
  val docValues = List[Long](2, -1, 2, 3, 2, 1, 2, 3, -1)

  override def spec =
    suite("forward segment 1-1")(
      test("document values") {
        check(int(7, 10)) { docCount =>
          check(listOfN(docCount)(long(min = -1, max = 3))) {
            forwardSegmentData =>
              val segment = buildSegment(forwardSegmentData)
              val result = forwardSegmentData
                .indices
                .flatMap(doc =>
                  segment.valueIndexesIterator(DocumentId(doc.toLong)).toSeq,
                )
              val eR = forwardSegmentData
                .map(i => ValueIdx(i))
                .filterNot(_ == ValueIdx.NOT_FOUND)
              assert(result)(hasSameElements(eR))
          }
        }
      },
      test("get values for out-of-bound document") {
        val segment = buildSegment(docValues)
        assertZIO(
          ZIO
            .attempt(
              segment.valueIndexesIterator(DocumentId(docValues.length.toLong)),
            )
            .exit,
        )(fails(isSubtype[IndexOutOfBoundsException](anything)))
      },
      test("update all documents") {
        check(int(7, 10)) { docCount =>
          check(listOfN(docCount)(long(min = -1, max = 3))) {
            forwardSegmentData =>
              val segment = buildSegment(forwardSegmentData)
              val updated: Seq[Int] = forwardSegmentData
                .indices
                .map(doc =>
                  segment.foldValueIndexes[Int](DocumentId(doc.toLong))(2)(
                    (n, idx) => idx.toInt + n,
                  ),
                )
              val eR = forwardSegmentData
                .map(i => ValueIdx(i))
                .map { v =>
                  v.toInt + 2
                }

              assert(updated)(hasSameElements(eR))
          }
        }
      },
      test("update out-of-bound document") {
        val segment = buildSegment(docValues)
        assertZIO(
          ZIO
            .attempt(
              segment.foldValueIndexes[Long](
                DocumentId(docValues.length.toLong),
              )(10)((n, idx) => idx.toInt + n),
            )
            .exit,
        )(fails(isSubtype[IndexOutOfBoundsException](anything)))
      },
      test("SerDe") {
        check(int(21, 30)) { docCount =>
          check(listOfN(docCount)(long(min = -1, max = 10))) {
            forwardSegmentData =>
              val eR = forwardSegmentData
                .map(i => ValueIdx(i))
                .filterNot(_ == ValueIdx.NOT_FOUND)

              val segment = buildSegment(forwardSegmentData)
              for {

                deser <- SegmentDeserializer.createEmptySegment(
                  SegmentDeserializer.SegmentMeta(
                    segment.getHeader,
                    forwardSegmentData.length.toLong,
                    Cardinality.SINGLE,
                    None,
                  ),
                )
                _ <- SegmentTool.serializeDeserialize(segment, deser)
                result <- ZIO.attempt(
                  forwardSegmentData
                    .indices
                    .flatMap(doc =>
                      deser
                        .asInstanceOf[ForwardSegment]
                        .valueIndexesIterator(DocumentId(doc.toLong))
                        .toSeq,
                    ),
                )
              } yield assert(result)(hasSameElements(eR))
          }
        }
      },
    )

  private def buildSegment(data: Seq[Long]): SingleForwardSegment = {
    val builder = SingleValueForwardSegmentBuilder(data.length.toLong)

    data
      .zipWithIndex
      .foreach { case (v, k) =>
        builder.set(k.toLong, v)
      }

    builder.build()
  }

}
