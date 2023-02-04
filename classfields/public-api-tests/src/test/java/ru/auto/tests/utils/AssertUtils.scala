package ru.auto.tests.utils

import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.{Assertions, IterableAssert}
import io.restassured.response.ResponseBodyExtractionOptions
import ru.auto.tests.publicapi.model.{AutoApiErrorResponse, AutoApiErrorResponseAssert}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR

import scala.jdk.CollectionConverters._

object AssertUtils {

  def assertApiError[T <: ResponseBodyExtractionOptions](error: ErrorEnum, detailedError: Option[String] = None)(response: T): AutoApiErrorResponseAssert = {
    val assert = assertThat(response.as(classOf[AutoApiErrorResponse])).hasStatus(ERROR).hasError(error)
    detailedError.foreach(err => assert.hasDetailedError(err))
    assert
  }

  def assertInAnyOrder[T](sequenceA: Iterable[T])(sequenceB: Iterable[T]): IterableAssert[T] = {
    Assertions.assertThat(sequenceA.asJava).hasSameElementsAs(sequenceB.asJava)
  }

}
