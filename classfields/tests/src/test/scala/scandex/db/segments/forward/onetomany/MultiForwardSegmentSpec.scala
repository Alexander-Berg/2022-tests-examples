package scandex.db.segments.forward.onetomany

import scandex.db.index.{DocumentId, ValueIdx}
import scandex.db.segments.SegmentTool
import scandex.db.segments.constructors.impl.v1.`var`.MultiValueForwardSegmentBuilder
import scandex.db.segments.forward.ForwardSegment
import scandex.db.serde.SegmentDeserializer
import scandex.model.meta.FieldDataTypeMeta.Cardinality
import zio.ZIO
import zio.test.*
import zio.test.Assertion.{anything, fails, hasSameElements, isSubtype}
import zio.test.Gen.*

object MultiForwardSegmentSpec extends ZIOSpecDefault {

  // ordinary test data
  val docValues: Seq[Set[Long]] = Seq(
    Set[Long](0, 1, 2),
    Set.empty[Long],
    Set[Long](0),
    Set[Long](0),
    Set.empty[Long],
    Set[Long](2, 3),
    Set[Long](1, 3),
    Set[Long](0, 2, 3),
  )

  override def spec =
    suite("forward segment 1-many")(
      test("document values") {
        check(
          listOf(
            oneOf(
              setOfBounded(min = 1, max = 3)(
                fromIterable(Seq[Long](0, 1, 2, 3)),
              ),
              const(Set.empty[Long]),
            ),
          ),
        ) { docValues =>
          val segment = buildSegment(docValues)
          val result: Seq[Set[ValueIdx]] = docValues
            .indices
            .map(doc =>
              segment.valueIndexesIterator(DocumentId(doc.toLong)).toSeq.toSet,
            )
          val eR = docValues
            .map(_.map(i => ValueIdx(i)).filterNot(_ == ValueIdx.NOT_FOUND))
          assert(result)(hasSameElements(eR))
        }
      },
      test("SerDe") {
        check(
          listOf(
            oneOf(
              setOfBounded(min = 1, max = 3)(
                fromIterable(Seq[Long](0, 1, 2, 3)),
              ),
              const(Set.empty[Long]),
            ),
          ),
        ) { docValues =>
          val eR = docValues
            .map(_.map(i => ValueIdx(i)).filterNot(_ == ValueIdx.NOT_FOUND))

          val segment = buildSegment(docValues)
          for {
            deser <- SegmentDeserializer.createEmptySegment(
              SegmentDeserializer.SegmentMeta(
                segment.getHeader,
                docValues.length.toLong,
                Cardinality.MULTI,
                None,
              ),
            )
            _ <- SegmentTool.serializeDeserialize(segment, deser)
            result <- ZIO.attempt(
              docValues
                .indices
                .map(doc =>
                  deser
                    .asInstanceOf[ForwardSegment]
                    .valueIndexesIterator(DocumentId(doc.toLong))
                    .toSeq
                    .toSet,
                ),
            )
          } yield assert(result)(hasSameElements(eR))
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
      test("update all value") {
        check(
          listOf(
            oneOf(
              setOfBounded(min = 1, max = 3)(
                fromIterable(Seq[Long](0, 1, 2, 3)),
              ),
              const(Set.empty[Long]),
            ),
          ),
        ) { docValues =>
          val segment = buildSegment(docValues)
          val updated = docValues
            .indices
            .map(doc =>
              segment
                .foldValueIndexes[Long](DocumentId(doc.toLong))(10)((n, idx) =>
                  idx.toInt + n,
                ),
            )
          val eR = docValues.map { v =>
            v.sum + 10
          }
          assert(updated)(hasSameElements(eR))
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
    )

  private def buildSegment(data: Seq[Set[Long]]): ForwardSegment = {
    val assembler = MultiValueForwardSegmentBuilder.assembler(
      numberOfDocuments = data.length.toLong,
      numberOfValues = data.flatten.length.toLong,
    )
    assembler.assemble(data.map(_.map(l => ValueIdx(l))))
  }

}
