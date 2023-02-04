package vasgen.indexer.saas.dm

object RtyServerStateSpec extends ZIOSpecDefault {

  override def spec = {
    suite("RtyServerState")(
      test("annotationsEnabled=true")(
        assert(RtyServerState(Samples.completeRtyState))(
          isRight(equalTo(RtyServerState(annotationsEnabled = true))),
        ),
      ),
      test("Components do not contain ANN")(
        assert(RtyServerState(Samples.rtyState1))(
          isRight(equalTo(RtyServerState(annotationsEnabled = false))),
        ),
      ),
      test("Components are missing")(
        assert(RtyServerState(Samples.rtyState2))(
          isRight(equalTo(RtyServerState(annotationsEnabled = false))),
        ),
      ),
    )
  }

}
