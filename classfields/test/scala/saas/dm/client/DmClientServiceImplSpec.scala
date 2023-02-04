package saas.dm.client

object DmClientServiceImplSpec extends ZIOSpecDefault {

  private val confFileListSample =
    """
      |{
      |  "files":
      |    [
      |      {
      |        "version":-1,
      |        "name":"not used now",
      |        "rename":"description",
      |        "url":"%7B%22generator%22%3A%7B%22type%22%3A%22description%22%7D%2C%22context%22%3A%7B%22slot_info%22%3A%7B%22slot%22%3A%22%22%2C%22disableindexing%22%3Afalse%2C%22disablefetch%22%3Afalse%2C%22servicetype%22%3A%22indexerproxy%22%2C%22dc%22%3A%22%22%2C%22disablesearch%22%3Afalse%2C%22configtype%22%3A%22default%22%2C%22isintsearch%22%3Afalse%2C%22disablesearchfiltration%22%3Afalse%2C%22shardmin%22%3A41299264%2C%22shardmax%22%3A1%2C%22service%22%3A%22indexerproxy%22%2C%22ctype%22%3A%22stable%22%7D%2C%22no_patch%22%3Afalse%7D%7D"
      |      },
      |      {
      |        "version":-1,
      |        "name":"not used now",
      |        "rename":"searchmap.json",
      |        "url":"%7B%22generator%22%3A%7B%22version%22%3A20710%2C%22type%22%3A%22searchmap%22%2C%22service_type%22%3A%22indexerproxy%22%7D%2C%22context%22%3A%7B%22slot_info%22%3A%7B%22slot%22%3A%22%22%2C%22disableindexing%22%3Afalse%2C%22disablefetch%22%3Afalse%2C%22servicetype%22%3A%22indexerproxy%22%2C%22dc%22%3A%22%22%2C%22disablesearch%22%3Afalse%2C%22configtype%22%3A%22default%22%2C%22isintsearch%22%3Afalse%2C%22disablesearchfiltration%22%3Afalse%2C%22shardmin%22%3A41299264%2C%22shardmax%22%3A1%2C%22service%22%3A%22indexerproxy%22%2C%22ctype%22%3A%22stable%22%7D%2C%22no_patch%22%3Afalse%7D%7D"
      |      }
      |    ],
      |  "slot_info":
      |    {
      |      "slot":"",
      |      "shardmin":41299264,
      |      "shardmax":1,
      |      "service":"indexerproxy",
      |      "ctype":"stable"
      |    }
      |}
      |""".stripMargin

  private val confFileUrl =
    "%7B%22generator%22%3A%7B%22version%22%3A20710%2C%22type%22%3A%22searchmap%22%2C%22service_type%22%3A%22indexerproxy%22%7D%2C%22context%22%3A%7B%22slot_info%22%3A%7B%22slot%22%3A%22%22%2C%22disableindexing%22%3Afalse%2C%22disablefetch%22%3Afalse%2C%22servicetype%22%3A%22indexerproxy%22%2C%22dc%22%3A%22%22%2C%22disablesearch%22%3Afalse%2C%22configtype%22%3A%22default%22%2C%22isintsearch%22%3Afalse%2C%22disablesearchfiltration%22%3Afalse%2C%22shardmin%22%3A41299264%2C%22shardmax%22%3A1%2C%22service%22%3A%22indexerproxy%22%2C%22ctype%22%3A%22stable%22%7D%2C%22no_patch%22%3Afalse%7D%7D"

  override def spec: Spec[TestEnvironment, Any] = {
    suite("decodeJson")(
      test("version")(
        assert(ConfFile.version(confFileUrl))(isSome(equalTo(20710))),
      ),
      test("decodeConfFileList") {
        for {
          confFileList <- DmClientServiceImpl
            .decodeConfFileList(confFileListSample)
          url = confFileList.find(_.rename == "searchmap.json").map(_.url)
          version = confFileList
            .find(_.rename == "searchmap.json")
            .flatMap(_.version)
        } yield assert(confFileList.length)(equalTo(2)) &&
          assert(url)(isSome) && assert(url.forall(_ == confFileUrl))(isTrue) &&
          assert(version)(isSome(equalTo(20710)))
      },
    )
  }

}
