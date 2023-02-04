package auto.c2b.reception.model.test

import auto.c2b.reception.application_model.InternalApplication
import auto.c2b.reception.model.Application
import auto.c2b.reception.model.ApplicationStatus.WaitingDocuments
import auto.c2b.reception.model.mapping.ApplicationApiMapping._
import auto.c2b.reception.model.testkit.Generators.Application.applicationAny
import zio.test.Assertion._
import zio.test.TestAspect.shrinks
import zio.test._
import zio.test.environment.TestEnvironment

object ApplicationApiMappingTest extends DefaultRunnableSpec {

  private def setSameTimeZone(original: Application, decoded: Application) = {
    decoded.copy(
      createdAt = original.createdAt.withOffsetSameInstant(original.createdAt.getOffset),
      completedAt = original.completedAt.map(_.withOffsetSameInstant(original.completedAt.get.getOffset))
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("ApplicationApiMapping")(
      testM("Application⇽⇾InternalApplication") {
        checkN(10)(applicationAny) { app =>
          val internal: InternalApplication = applicationAsInternal(app)
          val decoded: Either[IllegalStateException, Application] = applicationFromInternal(internal)
            .map(setSameTimeZone(original = app, _))
          assert(Right(app))(equalTo(decoded))
        }
      }
    ) @@ shrinks(0)
  }
}
