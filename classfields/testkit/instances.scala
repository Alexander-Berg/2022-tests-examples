package ru.yandex.vertis.test_utils

import zio.test.diff.Diff

package object instances {

  // Will be present in ZIO 1.14
  implicit def diffSeq[A: Diff]: Diff[Seq[A]] =
    Diff.mkSeqDiff("Seq")(identity)
}
