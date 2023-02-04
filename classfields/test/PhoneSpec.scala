package ru.yandex.vertis.shark.model

import zio.Task
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object PhoneSpec extends DefaultRunnableSpec {

  private val phoneWithCode8 = Phone("89267917752")
  private val phoneWithPlusCode8 = Phone("+89267917752")

  private val phoneWithCode7 = Phone("79267917752")
  private val phoneWithPlusCode7 = Phone("+79267917752")

  private val phoneWithoutCode = Phone("9267917752")

  private val expectNormalizePhone = "79267917752"
  private val expectNormalizeLocalPhone = "89267917752"

  private def testCasesMap = Map(
    "with code 8" -> Right(phoneWithCode8),
    "with code +8" -> Left(phoneWithPlusCode8),
    "with code 7" -> Right(phoneWithCode7),
    "with code +7" -> Right(phoneWithPlusCode7),
    "without code" -> Right(phoneWithoutCode)
  )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PhoneSpec")(
      suitNormalize,
      suitNormalizeLocal
    )

  private def suitNormalize: Spec[Any, TestFailure[Throwable], TestSuccess] =
    suitBuilder("normalize", normalizeSuccess)

  private def suitNormalizeLocal: Spec[Any, TestFailure[Throwable], TestSuccess] =
    suitBuilder("normalize local", normalizeLocalSuccess)

  private def suitBuilder(
      title: String,
      fTest: (String, Phone) => ZSpec[Any, Throwable]): Spec[Any, TestFailure[Throwable], TestSuccess] = {
    val cases = testCasesMap.map { case (title, either) =>
      either match {
        case Right(phone) => fTest(title, phone)
        case Left(phone) => validationErrors(title, phone)
      }
    }.toSeq
    suite(title)(cases: _*)
  }

  private def normalizeSuccess(title: String, phone: Phone): ZSpec[Any, Throwable] =
    test(title)(assert(phone.normalized.get)(equalTo(expectNormalizePhone)))

  private def normalizeLocalSuccess(title: String, phone: Phone): ZSpec[Any, Throwable] =
    testM(title)(assertM(Task.effect(phone.normalizedLocal.get))(equalTo(expectNormalizeLocalPhone)))

  private def validationErrors(title: String, phone: Phone): ZSpec[Any, Throwable] =
    testM(title)(assertM(Task.effect(phone.normalized.get).run)(fails(anything)))
}
