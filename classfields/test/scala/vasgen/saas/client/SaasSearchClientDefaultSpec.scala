package vasgen.saas.client

object SaasSearchClientDefaultSpec extends ZIOSpecDefault {

  val zonesMap = Map(
    "title" -> "1 Мистер Барнстейпл решает отдохнуть".toLowerCase,
    "description" ->
      "2 Мистер Барнстейпл почувствовал, что самым настоятельным образом нуждается в отдыхе, но поехать ему было не с кем и некуда. 3 А он был переутомлен. 4 И он устал от своей семьи. 5 По натуре он был человеком очень привязчивым; он нежно любил жену и детей и поэтому знал их наизусть, так что в подобные периоды душевной подавленности они его невыносимо раздражали. 6 Трое его сыновей, дружно взрослевшие, казалось, с каждым днем становились все более широкоплечими и долговязыми; они усаживались именно в то кресло, которое он только что облюбовал для себя; они доводили его до исступления с помощью им же купленной пианолы; они сотрясали дом оглушительным хохотом, а спросить, над чем они смеются, было неудобно; они перебивали ему дорогу в безобидном отеческом флирте, до тех пор составлявшем одно из главных его утешений в этой юдоли скорби; они обыгрывали его в теннис; они в шутку затевали драки на лестничных площадках и с невообразимым грохотом по двое и по трое катились вниз. 7 Их шляпы валялись повсюду. 8 Они опаздывали к завтраку. 9 Каждый вечер они укладывались спать под громовые раскаты: «Ха-ха-ха! 10 Бац! 11 » – а их матери это как будто было приятно. 12 Они обходились недешево, но вовсе не желали считаться с тем, что цены растут в отличие от жалованья мистера Барнстейпла. 13 А когда за завтраком или обедом он позволял себе без обиняков высказаться о мистере Ллойд Джордже или пытался придать хоть некоторую серьезность пустой застольной болтовне, они слушали его с демонстративной рассеянностью, во всяком случае, так ему казалось."
        .toLowerCase,
    "attributes" -> "17 отдых".toLowerCase,
    "other" ->
      "14 А кроме того, ему хотелось на некоторое время уехать подальше от мистера Пиви. 15 Городские улицы стали для него источником мучений – он больше не мог выносить даже вида газет или газетных афишек. 16 Его томило гнетущее предчувствие гигантского финансового и экономического краха, по сравнению с которым недавняя мировая война покажется сущим пустяком."
        .toLowerCase,
  )

  val saasHits = Seq(
    HitEntry(1, 2),
    HitEntry(1, 3),
    HitEntry(1, 4),
    HitEntry(2, 5),
    HitEntry(3, 3),
    HitEntry(5, 10),
    HitEntry(6, 4),
    HitEntry(7, 1),
    HitEntry(7, 2),
    HitEntry(7, 4),
    HitEntry(16, 2),
  )

  val tDoc = ru
    .yandex
    .saas
    .model
    .TDocument
    .defaultInstance
    .withFirstStageAttribute(
      Seq(packAttribute(Highlights, saasHits.asJson.noSpaces)),
    )
    .withArchiveInfo(
      TArchiveInfo
        .defaultInstance
        .addGtaRelatedAttribute(
          packAttribute("s_zones", "title;description;attributes;other"),
          packAttribute(
            "s_all_text",
            zonesMap
              .values
              .map(_.replace("\\", "\\\\").replace(".", "\\."))
              .mkString("."),
          ),
        ),
    )

  override def spec: Spec[TestEnvironment, Any] =
    suite("extract")(
      test("highlight") {
        val result = SaasSearchClientDefault.extractSearchedFragments(tDoc)
        result.foreach { case (z, positions) =>
          val highlighted =
            positions.foldLeft(zonesMap(z))((s, i) =>
              s.updated(i.start, s(i.start).toUpper)
                .updated(i.stop, s(i.stop).toUpper),
            )
          println(highlighted)
        }
        println(result)
        assert(result)(hasSize(equalTo(3))) &&
        assert(result)(
          hasKeys(hasSameElements(Seq("title", "description", "other"))),
        ) && assert(result.values.flatten)(hasSize(equalTo(11)))
      },
    )

  private def packAttribute(key: String, value: String): TPairBytesBytes = {
    TPairBytesBytes.of(
      Some(ByteString.copyFromUtf8(key)),
      Some(ByteString.copyFromUtf8(value)),
    )
  }

}
