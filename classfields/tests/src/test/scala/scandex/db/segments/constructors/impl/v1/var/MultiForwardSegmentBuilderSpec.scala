package scandex.db.segments.constructors.impl.v1.`var`

import scandex.db.index.{DocumentId, ValueIdx}
import scandex.db.segments.forward.onetomany.MultiForwardSegment
import zio.ZIO
import zio.test.*
import zio.test.Assertion.*
import zio.test.Gen.*

object MultiForwardSegmentBuilderSpec extends ZIOSpecDefault {

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

  override def spec: Spec[TestEnvironment, Any] =
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
      test("get all values") {
        val segment = buildSegment(docValues)
        val values = docValues
          .indices
          .map(id =>
            segment.valueIndexesIterator(DocumentId(id.toLong)).toSet: Set[Long],
          )
        assert(values)(equalTo(docValues))
      },
      test("get values for out-of-bound document") {
        val segment = buildSegment(docValues)
        assertZIO(
          ZIO
            .from(
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
            .from(
              segment.foldValueIndexes[Long](
                DocumentId(docValues.length.toLong),
              )(10)((n, idx) => idx.toInt + n),
            )
            .exit,
        )(fails(isSubtype[IndexOutOfBoundsException](anything)))
      },
    )

  private def buildSegment(data: Seq[Set[Long]]): MultiForwardSegment = {

    val builder =
      new MultiValueForwardSegmentBuilder(
        data.flatten.length.toLong,
        data.length.toLong,
      )

    data.foreach(values => builder.add(values.map(idx => ValueIdx(idx))))

    builder.build()
  }

}
