package ru.yandex.vertis.general.gateway.app.test

import caliban.parsing.adt.Definition.TypeSystemDefinition
import ru.yandex.vertis.general.gateway.app.ApiProvider
import zio.Has
import zio.test.Assertion._
import zio.test._

object ApiDefinitionsTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ApiDefinitions")(
      testM("Api should create interpreter successfully") {
        for {
          api <- ApiProvider.api
          _ <- api.interpreter
        } yield assertCompletes
      },
      testM("Execute introspect query") {
        for {
          api <- ApiProvider.api
          i <- api.interpreter
          res <- i.execute(IntrospectQuery).provide(Has(new TestWorld))
        } yield assert(res.errors)(isEmpty)
      },
      testM("No Option* wrappers derived by magnolia") {
        for {
          api <- ApiProvider.api
          doc = api.toDocument
          types = doc.definitions.collect { case definition: TypeSystemDefinition.TypeDefinition =>
            definition
          }
        } yield {
          val badTypeName: Assertion[String] = equalTo("None") ||
            startsWithString("Some") ||
            startsWithString("Option") ||
            startsWithString("PositiveInt") ||
            containsString("::")
          assert(types)(forall(not(hasField("name", _.name, badTypeName))))
        }
      },
      testM("Test schema validation of SearchAreaInput") {
        val query =
          """
           |query GetCategories {
           |  categories {
           |    id
           |    shortName
           |    state
           |    offerCount(regionId: "11076")
           |    addFormLink {
           |      url
           |      route
           |    }
           |    searchLinks {
           |      withRequestLink(request: { area: { toponyms: { region: "11076"}}}) {
           |        url
           |        route
           |      }
           |    }
           |  }
           |
           |}""".stripMargin

        for {
          api <- ApiProvider.api
          i <- api.interpreter
          _ <- i.check(query)
        } yield assertCompletes
      }
    )

  private val IntrospectQuery =
    """
      |query IntrospectionQuery {
      |  __schema {
      |    queryType { name }
      |    mutationType { name }
      |    subscriptionType { name }
      |    types {
      |      ...FullType
      |    }
      |    directives {
      |      name
      |      description
      |      locations
      |      args {
      |        ...InputValue
      |      }
      |    }
      |  }
      |}
      |
      |fragment FullType on __Type {
      |  kind
      |  name
      |  description
      |  fields(includeDeprecated: true) {
      |    name
      |    description
      |    args {
      |      ...InputValue
      |    }
      |    type {
      |      ...TypeRef
      |    }
      |    isDeprecated
      |    deprecationReason
      |  }
      |  inputFields {
      |    ...InputValue
      |  }
      |  interfaces {
      |    ...TypeRef
      |  }
      |  enumValues(includeDeprecated: true) {
      |    name
      |    description
      |    isDeprecated
      |    deprecationReason
      |  }
      |  possibleTypes {
      |    ...TypeRef
      |  }
      |}
      |
      |fragment InputValue on __InputValue {
      |  name
      |  description
      |  type { ...TypeRef }
      |  defaultValue
      |}
      |
      |fragment TypeRef on __Type {
      |  kind
      |  name
      |  ofType {
      |    kind
      |    name
      |    ofType {
      |      kind
      |      name
      |      ofType {
      |        kind
      |        name
      |        ofType {
      |          kind
      |          name
      |          ofType {
      |            kind
      |            name
      |            ofType {
      |              kind
      |              name
      |              ofType {
      |                kind
      |                name
      |              }
      |            }
      |          }
      |        }
      |      }
      |    }
      |  }
      |}
      |""".stripMargin
}
