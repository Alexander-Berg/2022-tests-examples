package ru.yandex.auto.vin.decoder.report.processors.resolution

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.api.exceptions.IncorrectVinException
import ru.yandex.auto.vin.decoder.model.{BodyTypesSelector, ColorsSelector}
import ru.yandex.auto.vin.decoder.proto.VinHistory._
import ru.yandex.auto.vin.decoder.report.processors.resolution.RichBlockBuilders._
import ru.yandex.auto.vin.decoder.report.processors.resolution.RichBlocks._
import ru.yandex.auto.vin.decoder.verba.proto.BodyTypesSchema.VerbaBodyType
import ru.yandex.auto.vin.decoder.verba.proto.ColorsSchema
import ru.yandex.vertis.mockito.MockitoSupport._

import scala.jdk.CollectionConverters.IterableHasAsJava

class ResolutionTests extends AnyWordSpecLike with MockitoSugar with Matchers {

  val vin = "X4XXG55470DS40452"

  def buildPledge(status: VinInfoHistory.Status): VinInfoHistory.Builder = {
    VinInfoHistory
      .newBuilder()
      .setEventType(EventType.AUTOCODE_PLEDGE)
      .setVin(vin)
      .setStatus(status)
  }

  def buildWanted(status: VinInfoHistory.Status): VinInfoHistory.Builder = {
    VinInfoHistory
      .newBuilder()
      .setEventType(EventType.AUTOCODE_WANTED)
      .setVin(vin)
      .setStatus(status)
  }

  def buildConstraint(status: VinInfoHistory.Status): VinInfoHistory.Builder = {
    VinInfoHistory
      .newBuilder()
      .setEventType(EventType.AUTOCODE_CONSTRAINTS)
      .setVin(vin)
      .setStatus(status)
  }

  def buildDtp(status: VinInfoHistory.Status): VinInfoHistory.Builder = {
    VinInfoHistory
      .newBuilder()
      .setEventType(EventType.AUTOCODE_ACCIDENT)
      .setVin(vin)
      .setStatus(status)
  }

  private def buildColorSelector: ColorsSelector = {
    def buildColor(
        code: String,
        name: String,
        aliases: List[String] = List.empty,
        similarColors: List[String] = List.empty) = {
      ColorsSchema.Color
        .newBuilder()
        .setHexCode(code)
        .setName(name)
        .addAliases(name)
        .addAllAliases(aliases.asJava)
        .addAllSimilarColorCodes(similarColors.asJava)
        .build()
    }

    ColorsSelector(
      List(
        buildColor("c49648", "??????????????"),
        buildColor("fafbfb", "??????????"),
        buildColor("22a0f8", "??????????????", similarColors = List("0000cc")),
        buildColor("ffd600", "????????????"),
        buildColor("007f00", "??????????????"),
        buildColor("dea522", "??????????????"),
        buildColor("200204", "????????????????????"),
        buildColor("ee1d19", "??????????????", aliases = List("????????????????????, ????????????????, ????????????????")),
        buildColor("ff8649", "??????????????????"),
        buildColor("660099", "??????????????????", aliases = List("??????????????, ????????????????, ????????????????")),
        buildColor("cacecb", "??????????????????????", aliases = List("??????????")),
        buildColor("97948f", "??????????", aliases = List("??????????????????????")),
        buildColor("0000cc", "??????????", similarColors = List("22a0f8")),
        buildColor("4a2197", "????????????????????"),
        buildColor("040001", "????????????"),
        buildColor("ffc0cb", "??????????????")
      )
    )
  }

  private def buildBodyTypeSelector: BodyTypesSelector = {
    def buildBodyType(code: String, name: String, reportBodyTypes: Set[String]): VerbaBodyType = {
      VerbaBodyType
        .newBuilder()
        .setCode(code)
        .setName(name)
        .addAllReportBodyTypes(reportBodyTypes.asJava)
        .build()
    }

    BodyTypesSelector(
      Seq(
        buildBodyType(
          "coupe",
          "????????",
          Set(
            "??????????????????",
            "????????",
            "???????????????? ???????????????????? ??????????????????",
            "???????????????? ???????????????????? ????????"
          )
        ),
        buildBodyType(
          "sedan_2_doors",
          "?????????? 2 ????.",
          Set(
            "????????",
            "??????????",
            "???????????????? ???????????????????? ????????",
            "???????????????? ???????????????????? ??????????"
          )
        )
      )
    )
  }

  "Legal purity builders" must {
    "build unknown status pledge" in {
      val history = buildPledge(VinInfoHistory.Status.ERROR).build()
      val res = LegalPurityBuilders.buildPledges(history)
      res.getStatus shouldBe Status.UNKNOWN
    }

    "build ok status pledge when no pledges " in {
      val history = buildPledge(VinInfoHistory.Status.OK).build()
      val res = LegalPurityBuilders.buildPledges(history)
      res.getStatus shouldBe Status.OK
    }

    "build error status pledge when two open pledges with same number" in {
      val historyBuilder = buildPledge(VinInfoHistory.Status.OK)

      // two pledges with same number and defined getInPledge
      val pledge1 = Pledge.newBuilder().setNumber("2017-001-790661-534/1").setInPledge(true)
      val pledge2 = Pledge.newBuilder().setNumber("2017-001-790661-534").setInPledge(true)

      historyBuilder
        .addPledges(pledge1)
        .addPledges(pledge2)
      val history = historyBuilder.build()
      val res = LegalPurityBuilders.buildPledges(history)
      res.getStatus shouldBe Status.ERROR
    }

    "build error status pledge when two different pledges" in {
      val historyBuilder = buildPledge(VinInfoHistory.Status.OK)

      // two pledges with same number and defined getInPledge
      val pledge1 = Pledge.newBuilder().setNumber("2017-001-791121-534/1").setInPledge(true)
      val pledge2 = Pledge.newBuilder().setNumber("2017-001-790111-534").setInPledge(true)

      historyBuilder
        .addPledges(pledge1)
        .addPledges(pledge2)
      val history = historyBuilder.build()
      val res = LegalPurityBuilders.buildPledges(history)
      res.getStatus shouldBe Status.ERROR
    }

    "build unknown status wanted when autocode err" in {
      val wanted = buildWanted(VinInfoHistory.Status.ERROR).build()
      val res = LegalPurityBuilders.buildWanted(wanted)
      res.getStatus shouldBe Status.UNKNOWN
    }
    "build ok status wanted when autocode provided zero wanted" in {
      val wanted = buildWanted(VinInfoHistory.Status.OK).build()
      val res = LegalPurityBuilders.buildWanted(wanted)
      res.getStatus shouldBe Status.OK
    }
    "build error status wanted when autocode provided some wanted" in {
      val wantedB = buildWanted(VinInfoHistory.Status.OK)
      wantedB.addWanted(Wanted.newBuilder().setDate(1).build())

      val wanted = wantedB.build()
      val res = LegalPurityBuilders.buildWanted(wanted)
      res.getStatus shouldBe Status.ERROR
    }

    "build unknown status constraint when autocode err" in {
      val constraint = buildConstraint(VinInfoHistory.Status.ERROR).build()
      val res = LegalPurityBuilders.buildConstraint(constraint)
      res.getStatus shouldBe Status.UNKNOWN
    }
    "build ok status constraint when autocode provided zero constraint" in {
      val constraint = buildConstraint(VinInfoHistory.Status.OK).build()
      val res = LegalPurityBuilders.buildConstraint(constraint)
      res.getStatus shouldBe Status.OK
    }
    "build error status constraint when autocode provided some constraint" in {
      val constraintB = buildConstraint(VinInfoHistory.Status.OK)
      constraintB.addConstraints(Constraint.newBuilder().setDate(1).build())

      val constraint = constraintB.build()
      val res = LegalPurityBuilders.buildConstraint(constraint)
      res.getStatus shouldBe Status.ERROR
    }
  }

  "Ownership history builders" must {
    "build valid owners res" in {
      // undefined in offer
      OwnershipHistoryBuilders.buildOwnersRes(None, 3).getStatus shouldBe Status.UNKNOWN

      // undefined in both
      OwnershipHistoryBuilders.buildOwnersRes(None, 0).getStatus shouldBe Status.UNKNOWN

      // undefined in autocode
      OwnershipHistoryBuilders.buildOwnersRes(Some(3), 0).getStatus shouldBe Status.UNKNOWN

      // offer 3 and more,  autocode exact
      OwnershipHistoryBuilders.buildOwnersRes(Some(3), 100).getStatus shouldBe Status.OK

      // offer equal autocode
      OwnershipHistoryBuilders.buildOwnersRes(Some(2), 2).getStatus shouldBe Status.OK

      // offer 3 and more, autocode 1 or 2
      OwnershipHistoryBuilders.buildOwnersRes(Some(3), 1).getStatus shouldBe Status.OK
      OwnershipHistoryBuilders.buildOwnersRes(Some(3), 2).getStatus shouldBe Status.OK

      // offer less than 3 and less than  autocode
      OwnershipHistoryBuilders.buildOwnersRes(Some(2), 1).getStatus shouldBe Status.OK

      // offer less than 3, not equal to autocode
      OwnershipHistoryBuilders.buildOwnersRes(Some(1), 2).getStatus shouldBe Status.UNKNOWN
    }

    "build empty owners res if we have right wheel and no periods" in {
      val ownersAndAccidents = OwnershipHistoryBuilders.buildHistoryInfo(
        request = ResolutionRequest(
          offerId = "aa-aaa",
          mark = None,
          model = None,
          year = None,
          powerHp = None,
          displacement = None,
          bodyType = None,
          ownersCount = Some(2),
          sellerType = None,
          color = None,
          format = None,
          kmAge = None
        ),
        autocodeRegistration = Registration.newBuilder().build(),
        autocodeDtp = VinInfoHistory
          .newBuilder()
          .setVin("1")
          .setEventType(EventType.AUTOCODE_ACCIDENT)
          .setStatus(VinInfoHistory.Status.OK)
          .build(),
        rightWheel = true,
        offer = None,
        offers = List.empty,
        isFree = false
      )
      ownersAndAccidents.ownersGroup shouldBe None
      ownersAndAccidents.buildSummaryBlock.getStatus shouldBe Status.OK
    }

    "build accidents unknown if no info from autocode" in {
      val dtp = buildDtp(VinInfoHistory.Status.ERROR).build()

      OwnershipHistoryBuilders.buildDtpGroup(dtp).accidentsStatus shouldBe Status.UNKNOWN
    }
    "build accidents ok if accidents less <= 10" in {
      val dtp = buildDtp(VinInfoHistory.Status.OK)

      OwnershipHistoryBuilders.buildDtpGroup(dtp.build()).accidentsStatus shouldBe Status.OK
      dtp.addAccidents(Accident.newBuilder().setRegion("X"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Y"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Z"))

      dtp.addAccidents(Accident.newBuilder().setRegion("X"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Y"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Z"))

      dtp.addAccidents(Accident.newBuilder().setRegion("X"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Y"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Z"))

      dtp.addAccidents(Accident.newBuilder().setRegion("X"))

      OwnershipHistoryBuilders.buildDtpGroup(dtp.build()).accidentsStatus shouldBe Status.ERROR
    }

    "build accidents err if accidents more than 10" in {
      val dtp = buildDtp(VinInfoHistory.Status.OK)

      OwnershipHistoryBuilders.buildDtpGroup(dtp.build()).accidentsStatus shouldBe Status.OK
      dtp.addAccidents(Accident.newBuilder().setRegion("X"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Y"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Z"))

      dtp.addAccidents(Accident.newBuilder().setRegion("X"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Y"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Z"))

      dtp.addAccidents(Accident.newBuilder().setRegion("X"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Y"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Z"))

      dtp.addAccidents(Accident.newBuilder().setRegion("X"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Y"))
      dtp.addAccidents(Accident.newBuilder().setRegion("Z"))

      for (considerAll <- Seq(true, false))
        OwnershipHistoryBuilders.buildDtpGroup(dtp.build()).accidentsStatus shouldBe Status.ERROR
    }
  }

  "Tech params bulders" must {

    "build correct year status" in {
      YearBlockBuilder(2000, 2000).getStatus shouldBe Status.OK
      YearBlockBuilder(1910, 2010).getStatus shouldBe Status.OK
      YearBlockBuilder(0, 2000).getStatus shouldBe Status.UNKNOWN
      YearBlockBuilder(2005, 2000).getStatus shouldBe Status.ERROR
    }

    "build correct displacement status" in {
      DisplacementBlockBuilder(2000, 2000).getStatus shouldBe Status.OK
      DisplacementBlockBuilder(2000, 2100).getStatus shouldBe Status.OK
      DisplacementBlockBuilder(0, 2000).getStatus shouldBe Status.UNKNOWN
      DisplacementBlockBuilder(2005, 0).getStatus shouldBe Status.OK
      DisplacementBlockBuilder(2500, 2000).getStatus shouldBe Status.UNKNOWN
      DisplacementBlockBuilder(2560, 2000).getStatus shouldBe Status.ERROR
    }

    "build correct power block" in {
      PowerBlockBuilder(100, 101).getStatus shouldBe Status.OK
      PowerBlockBuilder(100, 110).getStatus shouldBe Status.OK
      PowerBlockBuilder(1100, 110).getStatus shouldBe Status.OK
      PowerBlockBuilder(125, 100).getStatus shouldBe Status.UNKNOWN
      PowerBlockBuilder(1250, 100).getStatus shouldBe Status.UNKNOWN
      PowerBlockBuilder(0, 2000).getStatus shouldBe Status.UNKNOWN
      PowerBlockBuilder(126, 100).getStatus shouldBe Status.ERROR
      PowerBlockBuilder(1260, 100).getStatus shouldBe Status.ERROR
    }

    "build correct color block" in {
      ColorBlockBuilder("????????????", Some("040001"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("??????????", Some("0000cc"), buildColorSelector).getStatus shouldBe Status.UNKNOWN
      ColorBlockBuilder("????????????-??????????", Some("0000cc"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("????????????-??????????????", Some("0000cc"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("????????????-??????????", Some("22a0f8"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("????????????-??????????", Some("cacecb"), buildColorSelector).getStatus shouldBe Status.OK
      // for both verba and non verba codes
      ColorBlockBuilder("????????????-????????????????????????", Some("97948f"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("????????????-????????????????????????", Some("9c9999"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("????????????-????????????????????????", Some("9c9999"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("???????????? ?????? ??????????????????", Some("007f00"), buildColorSelector).getStatus shouldBe Status.OK
      ColorBlockBuilder("???????????? ?????? ??????????????????", Some("22a0f8"), buildColorSelector).getStatus shouldBe Status.UNKNOWN
      // ColorBlockBuilder("???????????? ?????? ??????????????????", Some("22a0a8"), buildColorSelector).getStatus shouldBe Status.UNKNOWN
    }

    "build correct body type block" in {
      BodyTypeBlockBuilder("??????????????????", "", "COUPE", buildBodyTypeSelector).getStatus shouldBe Status.OK
      BodyTypeBlockBuilder("????????", "", "SEDAN_2_DOORS", buildBodyTypeSelector).getStatus shouldBe Status.OK
      BodyTypeBlockBuilder("??????????", "", "HATCHBACK", buildBodyTypeSelector).getStatus shouldBe Status.UNKNOWN
      // ?????? ?????????????????????? ???????????? ????????????????, ?????????? ???????????????? ?? ?????????????????? ?????????????????????? ?? ?????????????????? ?????????????????????????? ??????
      BodyTypeBlockBuilder("??????????", "", "HATCHBACK", buildBodyTypeSelector).getError shouldBe
        "???????????????????????? ?????? ????????????: ?????????? (????????????????????????????? hatchback)"
      BodyTypeBlockBuilder("??????????", "", "COUPE", buildBodyTypeSelector).getError shouldBe
        "???????????????????????? ?????? ????????????: ?????????? (????????????????????????????? ????????)"
    }

    "build body type block with confirmed" in {
      BodyTypeBlockBuilder("??????????????????", "coupe", "COUPE", buildBodyTypeSelector).getStatus shouldBe Status.OK
      BodyTypeBlockBuilder("", "coupe", "COUPE", buildBodyTypeSelector).getStatus shouldBe Status.OK
      BodyTypeBlockBuilder("", "SEDAN", "COUPE", buildBodyTypeSelector).getStatus shouldBe Status.UNKNOWN
      BodyTypeBlockBuilder("??????????????????", "sedan_2_doors", "COUPE", buildBodyTypeSelector).getStatus shouldBe
        Status.UNKNOWN
      BodyTypeBlockBuilder("??????????????????", "sedan_2_doors", "COUPE", buildBodyTypeSelector).getError shouldBe
        "???????????????????????? ?????? ????????????: ?????????? 2 ????. (????????????????????????????? ????????)"
    }

    //    TODO refactor builder for set Catalog provider
    def buildRequest(mark: Option[String], model: Option[String]): ResolutionRequest = {
      ResolutionRequest(
        "4-a",
        mark = mark,
        model = model,
        year = None,
        powerHp = None,
        displacement = None,
        bodyType = None,
        ownersCount = Some(2),
        sellerType = None,
        color = None,
        format = None,
        kmAge = None
      )
    }

    def buildAutocodeReg(mark: String, model: String, raw: String): Registration = {
      Registration
        .newBuilder()
        .setMark(mark)
        .setModel(model)
        .setRawMarkModel(raw)
        .build()
    }

    import ru.yandex.auto.vin.decoder.model.catalog._

    val HONDA = "HONDA"
    val ACCORD = "ACCORD"

    "build OK mark model resolutions" in {
      val r = buildRequest(Some(HONDA), Some(ACCORD))
      val a = buildAutocodeReg(HONDA, ACCORD, "H A")
      val catalog = mock[Catalog]
      when(catalog.getCard(?, ?)).thenReturn(
        Some(
          CatalogCard(Mark(HONDA, "X", "url", Some("dark_url")), Model(ACCORD, "A", None, None), null, null)
        )
      )
      RichBlockBuilders.MarkModelBlockBuilder(r.mark.get, r.model.get, a, false, catalog).getStatus shouldBe Status.OK

      val a1 = buildAutocodeReg(HONDA, "CIVIC", "H A")
      RichBlockBuilders.MarkModelBlockBuilder(HONDA, ACCORD, a1, true, catalog).getStatus shouldBe Status.OK
    }

    "build UNKNOWN mark model resolutions" in {
      val catalog = mock[Catalog]
      when(catalog.getCard(?, ?)).thenReturn(
        Some(
          CatalogCard(Mark(HONDA, "X", "url", Some("dark_url")), Model(ACCORD, "A", None, None), null, null)
        )
      )

      val a = buildAutocodeReg("", "", "H A")
      RichBlockBuilders.MarkModelBlockBuilder(HONDA, ACCORD, a, false, catalog).getStatus shouldBe Status.UNKNOWN

      val a1 = buildAutocodeReg(HONDA, "", "H A")
      RichBlockBuilders.MarkModelBlockBuilder(HONDA, ACCORD, a1, false, catalog).getStatus shouldBe Status.UNKNOWN

      val a2 = buildAutocodeReg(HONDA, "CIVIC", "H A")
      RichBlockBuilders.MarkModelBlockBuilder(HONDA, ACCORD, a2, false, catalog).getStatus shouldBe Status.UNKNOWN

      val a3 = buildAutocodeReg("FORD", "A", "FORD A")
      RichBlockBuilders.MarkModelBlockBuilder(HONDA, ACCORD, a3, true, catalog).getStatus shouldBe Status.UNKNOWN
    }

    "build ERROR mark model resolutions" in {

      val catalog = mock[Catalog]
      when(catalog.getCard(?, ?)).thenReturn(
        Some(
          CatalogCard(Mark(HONDA, "X", "url", Some("dark_url")), Model(ACCORD, "A", None, None), null, null)
        )
      )
      val a = buildAutocodeReg("FORD", "A", "FORD A")
      intercept[IncorrectVinException] {
        RichBlockBuilders.MarkModelBlockBuilder(HONDA, ACCORD, a, false, catalog).getStatus
      }
    }

  }

}
