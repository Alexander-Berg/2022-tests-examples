package saas.push.sharding

import scala.io.Source

object UrlHashShardingRuleTest extends ZIOSpecDefault {

  val tMessageGen = Gen
    .alphaNumericString
    .withFilter(_.nonEmpty)
    .map(url =>
      TMessage
        .defaultInstance
        .withDocument(TDocument.defaultInstance.withUrl(url)),
    )

  val testData = Gen.fromIterable(
    Source
      .fromResource("saas_url_hashes.tsv")
      .getLines()
      .collect { case s"$url $hash" =>
        (url, hash.toLong)
      }
      .toVector,
  )

  val shards = (0L until 65533L)
    .grouped(100)
    .map(r => Shard(Some(r.head), 80, 81, r.last, ""))
    .toList

  override def spec: Spec[environment.TestEnvironment, Any] =
    suite("URL Hash sharding rule")(
      test("compatible with SaaS code") {
        check(testData) { case (url, hash) =>
          assert(calcHash(url))(equalTo(hash))
        }
      },
    )

}
