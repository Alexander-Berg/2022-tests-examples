package scandex.builder.db

import strict.Utf8
import zio.test.Gen

object Schema {

  val uuIdGen: Gen[Any, Utf8] = Gen.uuid.map(u => Utf8(u.toString))

  val idGenerator = Gen
    .weighted(
      (Gen.const("id1"), 0.05),
      (Gen.const("id2"), 0.05),
      (Gen.const("id3"), 0.05),
      (Gen.const("id4"), 0.05),
      (Gen.const("id5"), 0.5),
      (Gen.const("id6"), 0.1),
      (Gen.const("id7"), 0.05),
      (Gen.const("id8"), 0.05),
      (Gen.const("id9"), 0.05),
      (Gen.const("id10"), 0.05),
    )
    .map(Utf8(_))

  val wordGenerator = Gen.elements(
    "zero",
    "one",
    "two",
    "three",
    "four",
    "five",
    "six",
    "seven",
    "eight",
    "nine",
  )

}
