package infra.profiler_collector.storage.test

import infra.profiler_collector.model.{AnnotatedSample, StackTrace}
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.{random, ZIO}

import java.time.Instant
import java.time.temporal.ChronoUnit

object SampleMother {

  val frameGen: Gen[Random with Sized, StackTrace.Frame] = for {
    methodName <- Gen.alphaNumericString
    line <- Gen.int(0, 10000)
    frameType <- Gen.elements[StackTrace.FrameType](StackTrace.FrameType.values: _*)
  } yield StackTrace.Frame(methodName, line, frameType)

  val stackTraceGen: Gen[Random with Sized, StackTrace] =
    Gen.listOfBounded(1, 8)(frameGen).map(StackTrace(_))

  val sampleGen: Gen[random.Random with Sized, AnnotatedSample] = for {
    serviceName <- Gen.elements("test-service", "service2", "service3")
    mode <- Gen.elements("cpu", "itimer", "alloc", "lock")
    timestamp <- Gen.instant(Instant.EPOCH, Instant.EPOCH.plus(10, ChronoUnit.DAYS))
    labels <- Gen.mapOf(Gen.alphaNumericString, Gen.alphaNumericString)
    trace <- stackTraceGen
    samples <- Gen.long(1, Int.MaxValue)
  } yield AnnotatedSample(serviceName, timestamp, mode, labels, trace, samples)

  def samples(n: Int): ZIO[Random with Sized, Nothing, List[AnnotatedSample]] = sampleGen
    .runCollectN(n)
    .provideSomeLayer[Sized](zio.random.Random.live)
}
