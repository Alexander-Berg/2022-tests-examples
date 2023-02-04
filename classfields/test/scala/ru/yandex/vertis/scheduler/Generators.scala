package ru.yandex.vertis.scheduler

import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.{DateTime, Duration}
import org.scalacheck.Gen
import ru.yandex.vertis.scheduler.model._

import scala.collection.JavaConverters._
import scala.util.Random

/**
  * Model object generators for testing purposes.
  *
  * @author dimas
  */
object Generators {

  def stringGen(minLength: Int, maxLength: Int): Gen[String] = for {
    length <- Gen.chooseNum(minLength, maxLength)
    result = Random.alphanumeric.take(length).mkString
  } yield result

  val DateTimeGen: Gen[DateTime] = for {
    days <- Gen.chooseNum(-100, +100)
  } yield DateTime.now.plusDays(days)

  val DurationGen: Gen[Duration] = for {
    ms <- Gen.chooseNum(0, +100500)
  } yield Duration.millis(ms)

  val SchedulerInstanceGen: Gen[SchedulerInstance] = for {
    id <- stringGen(1, 10)
    name <- Gen.option(stringGen(1, 10))
  } yield SchedulerInstance(id, name)

  val ConfigGen: Gen[Config] = for {
    items <- Gen.choose(0, 10)
    values <- Gen.listOfN(items, Gen.zip(stringGen(1, 5), stringGen(1, 5)))
  } yield ConfigFactory.parseMap(values.toMap.asJava)

  val OptConfigGen: Gen[Option[Config]] = Gen.option(ConfigGen)

  val JobCompletedGen: Gen[JobCompleted] = for {
    time <- DateTimeGen
    duration <- DurationGen
    message <- Gen.option(stringGen(0, 10))
  } yield JobCompleted(time, duration, message)

  val JobFailedGen: Gen[JobFailed] = for {
    time <- DateTimeGen
    duration <- DurationGen
    message <- stringGen(0, 10)
  } yield JobFailed(time, duration, message)

  val JobResultGen: Gen[JobResult] =
    Gen.oneOf(JobCompletedGen, JobFailedGen)

  val JobGen: Gen[Job] = for {
    start <- DateTimeGen
    instance <- SchedulerInstanceGen
    config <- ConfigGen
    result <- Gen.option(JobResultGen)
  } yield Job(start, instance, config, result)

  val LastJobGen: Gen[LastJob] = Gen.option(JobGen)
}
