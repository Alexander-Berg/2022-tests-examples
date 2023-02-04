package infra.profiler_collector.converter.test

import com.google.devtools.build.runfiles.Runfiles
import common.profiler.Profiler
import zio.Chunk
import scala.jdk.CollectionConverters._

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object TestData {
  val runfiles: Runfiles = Runfiles.create()

  val testProfileResults: Path = Paths.get(
    runfiles.rlocation("verticals_backend/infra/profiler_collector/converter/test/test-profile-results.jfr")
  )

  val profilingMode: Profiler.ProfilingMode =
    Profiler.ProfilingMode(Profiler.CpuProfilingMode.Itimer, alloc = true, locks = true)

  val expectedSamples: Chunk[String] =
    Chunk.fromIterable(
      Files
        .readAllLines(
          Paths.get(
            runfiles.rlocation("verticals_backend/infra/profiler_collector/converter/test/expected_samples.txt")
          ),
          StandardCharsets.UTF_8
        )
        .asScala
    )
}
