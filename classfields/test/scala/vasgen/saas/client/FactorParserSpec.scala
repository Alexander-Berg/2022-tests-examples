package vasgen.saas.client

object FactorParserSpec extends ZIOSpecDefault {

  implicit val oldDecoder: Decoder[Map[String, Double]] = {
    Decoder
      .decodeJson
      .map { json =>
        json
          .asArray
          .map { array =>
            val y = array.flatMap(
              _.asObject
                .map(fact =>
                  fact
                    .toMap
                    .flatMap { case (n, vs) =>
                      vs.asNumber
                        .map { v =>
                          (n, v.toDouble)
                        }
                    },
                ),
            )
            y.reduce(_ ++ _)
          }
          .getOrElse(Map[String, Double]())
      }
  }

  private val sample =
    "[{\"LongQuery\":0.0757841},{\"InvWordCount\":1},{\"TLen\":0.272727}]"

  override def spec
    : Spec[TestEnvironment, TestFailure[Nothing], TestSuccess] = {
    suite("parseFactors")(
      test("check against old decoder") {
        assert(sample.parseFactors(skipZeros = true))(
          equalTo(decode[Map[String, Double]](sample).toOption.get),
        )
      },
      test("empty str") {
        assert("".parseFactors(skipZeros = true))(isEmpty)
      },
      test("skip zeros") {
        assert(
          "[{\"LongQuery\":0.0757841},{\"InvWordCount\":0},{\"TLen\":0.0}]"
            .parseFactors(skipZeros = true),
        )(equalTo("[{\"LongQuery\":0.0757841}]".parseFactors(skipZeros = true)))
      },
    )
  }

}
