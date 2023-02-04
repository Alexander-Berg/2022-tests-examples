package ru.vertistraf.common.model.s3

import ru.vertistraf.common.model.s3.S3Path.{S3DirPath, S3FilePath}
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import eu.timepit.refined.auto._

object S3PathKeySpec extends DefaultRunnableSpec {

  private val cases: Seq[(S3Path, String)] = Seq(
    S3DirPath("b", Seq.empty) -> "",
    S3DirPath("b", Seq("dir")) -> "dir/",
    S3FilePath(S3DirPath("b", Seq.empty), "file.txt") -> "file.txt",
    S3FilePath(S3DirPath("b", Seq("dir")), "file.txt") -> "dir/file.txt"
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val tests =
      cases.map { case (path, key) =>
        test(s"path $path should have key $key") {
          assertTrue(path.key == key)
        }
      }

    suite("S3Path.key")(tests: _*)
  }
}
