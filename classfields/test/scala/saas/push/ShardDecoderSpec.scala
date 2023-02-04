package saas.push

object ShardDecoderSpec extends ZIOSpecDefault {

  private val shardListSample =
    """
      |{
      |  "services":
      |    {
      |      "vasgen_search_lb":
      |        {
      |          "per_dc_search":true,
      |          "shard_by":"url_hash",
      |          "replicas":
      |            {
      |              "default":
      |                [
      |                  {
      |                    "search_port":80,
      |                    "indexer_port":82,
      |                    "shard_max":6552,
      |                    "host":"saas-yp-vasgen-search-lb-1.man.yp-c.yandex.net"
      |                  },
      |                  {
      |                    "shard_min":6553,
      |                    "search_port":80,
      |                    "indexer_port":82,
      |                    "shard_max":13105,
      |                    "host":"saas-yp-vasgen-search-lb-14.man.yp-c.yandex.net"
      |                  }
      |                ]
      |            }
      |        }
      |    },
      |  "properties":
      |    {
      |      "version":2
      |    },
      |  "meta":
      |    {
      |      "ydo_search_wizard":
      |        {
      |          "components":
      |            [
      |              {
      |                "priority":0,
      |                "service":"ydo_search"
      |              }
      |            ],
      |          "shard_by":"url_hash"
      |        },
      |      "pdb":
      |        {
      |          "components":
      |            [
      |              {
      |                "priority":0,
      |                "service":"pdb-service-search"
      |              },
      |              {
      |                "priority":0,
      |                "service":"pdb-service-search-rt"
      |              }
      |            ],
      |          "shard_by":"url_hash"
      |        }
      |    }
      |}
      |""".stripMargin

  override def spec: Spec[TestEnvironment, Any] = {
    suite("decodeJson")(
      test("decodeShardList") {
        for {
          shardList <- Shard.decodeList("vasgen_search_lb")(shardListSample)
        } yield assert(shardList)(
          hasSameElements(
            Seq(
              Shard(
                shard_min = None,
                search_port = 80,
                indexer_port = 82,
                shard_max = 6552,
                host = "saas-yp-vasgen-search-lb-1.man.yp-c.yandex.net",
              ),
              Shard(
                shard_min = Some(6553),
                search_port = 80,
                indexer_port = 82,
                shard_max = 13105,
                host = "saas-yp-vasgen-search-lb-14.man.yp-c.yandex.net",
              ),
            ),
          ),
        )
      },
    )
  }

}
