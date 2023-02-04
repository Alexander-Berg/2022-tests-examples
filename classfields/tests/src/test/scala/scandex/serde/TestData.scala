package scandex.serde

import strict.{Bool, Bytes, Int32, Utf8}

object TestData {
  val emptyBytes     = Bytes(Array[Byte]())
  val docCount       = 10L
  val pk: Seq[Int32] = Seq[Int32](1, 2, 3, 6, 9, 12, 14, 56, 78, 89)

  val utfPK: Seq[Utf8] = Seq[Utf8](
    "Lorem",
    "ipsum",
    "dolor",
    "sit",
    "amet",
    "consectetur",
    "adipiscing",
    "elit",
    "Integer",
    "eget",
  )

  val sieve: Seq[Boolean] = Seq(
    // 0   1     2      3      4     5      6      7       8    9
    true, true, false, false, true, true, false, false, true, true,
  )

  val intStorage: Seq[Int32] = Seq[Int32](12, 14, 6, 9, -1, 2, 3, 56, 78, 89)

  val strStorage: Seq[Utf8] = Seq[Utf8](
    "cat",
    "dog",
    "rabbit",
    "",
    "cow",
    "lion",
    "cat",
    "",
    "giraffe",
    "dog",
  )

  val bytesStorage: Seq[Bytes] = Seq(
    Bytes("lio7878n".getBytes),
    Bytes("c888at".getBytes),
    emptyBytes,
    Bytes("co0w".getBytes),
    emptyBytes,
    Bytes("cat77".getBytes),
    Bytes("do78g".getBytes),
    Bytes("rab555bit".getBytes),
    emptyBytes,
    emptyBytes,
  )

  val isSomething: Seq[Bool] = Seq(
    // 0   1     2      3      4     5      6      7       8    9
    true, true, false, false, true, true, false, false, true, true,
  ).map(Bool(_))

}
