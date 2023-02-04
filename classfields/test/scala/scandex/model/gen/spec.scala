//package scandex.model.gen
//
//import zio.test.Gen._
//import zio.test._
//import zio.console._
//
//case object spec extends ZIOSpecDefault {
//
//  override def spec: Spec[environment.TestEnvironment, Any] =
//    suite("model")(
//      test("Document") {
//        checkNM(10)(heteroDocsGen(anyString)) { docsGen =>
//          checkNM(10)(chunkOf(docsGen)) { docs =>
//            putStrLn(docs.mkString("[\n\t", ",\n\t", "\n]"))
//              .as(assert(true)(Assertion.anything))
//          }
//        }
//      },
//    )
//
//}
