package common.graphql.directives.test

import caliban.CalibanError.ExecutionError
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test._
import caliban.GraphQL.graphQL
import caliban.{CalibanError, GraphQLInterpreter, GraphQLResponse}
import caliban.Macros.gqldoc
import caliban.ResponseValue.ObjectValue
import caliban.Value.StringValue
import common.graphql.directives.OptionalFields
import common.graphql.directives.testkit.TestApi
import zio.{ZIO, ZLayer}
import zio.clock.Clock

object OptionalFieldsSpec extends DefaultRunnableSpec {

  private def createInterpreter = (for {
    clock <- ZIO.service[Clock.Service]
    resolver <- TestApi.resolver
    interpreter <- (graphQL(resolver) |+| OptionalFields(clock)).interpreter
  } yield interpreter).toLayer.orDie

  private def extractErrorExtensions(resp: GraphQLResponse[CalibanError]) = resp.errors
    .collectFirst { case e: ExecutionError => e }
    .flatMap(_.extensions)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("OptionalFields")(
      testM("fail on timeout") {
        for {
          interpreter <- ZIO.service[GraphQLInterpreter[Any, CalibanError]]
          query = gqldoc("""
              query {
                slowField @optional(timeout: 100)
              }
              """)
          result <- interpreter.execute(query)
        } yield assert(extractErrorExtensions(result))(
          isSome(equalTo(ObjectValue(List(("code", StringValue("OptionalFailure"))))))
        )
      },
      testM("add optional code on annotated field failure") {
        for {
          interpreter <- ZIO.service[GraphQLInterpreter[Any, CalibanError]]
          query = gqldoc("""
              query {
                failingField @optional
              }
              """)
          queryWithTimeout = gqldoc("""
              query {
                failingField @optional(timeout: 1000)
              }
              """)
          result <- interpreter.execute(query)
          resultWithTimeout <- interpreter.execute(queryWithTimeout)
        } yield assert(extractErrorExtensions(result).map(_.fields))(
          isSome(contains(("code", StringValue("OptionalFailure"))))
        ) && assert(extractErrorExtensions(resultWithTimeout).map(_.fields))(
          isSome(contains(("code", StringValue("OptionalFailure"))))
        )
      }
    )
  }.provideCustomLayerShared {
    Clock.live >>> createInterpreter
  }
}
