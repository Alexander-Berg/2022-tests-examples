package ru.yandex.vertis.vsquality.techsupport.dao.scenario

import org.scalacheck.{Arbitrary, Gen}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable.{AwaitableSyntax, IoAwaitable}
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson._
import ru.yandex.vertis.vsquality.techsupport.Arbitraries
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances._
import ru.yandex.vertis.vsquality.techsupport.dao.scenario.CheckResult._
import ru.yandex.vertis.vsquality.techsupport.dao.scenario.ScenarioDao.{ScenarioKey, ScenarioMetaInfo, ScenarioRow}
import ru.yandex.vertis.vsquality.techsupport.model.Domain
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario
import ru.yandex.vertis.vsquality.techsupport.util.Clearable.Ops
import ru.yandex.vertis.vsquality.techsupport.util.ydb.YdbSpecBase
import ru.yandex.vertis.vsquality.techsupport.util.{scenarioFromFile, Clearable}

import java.time.Instant
import java.time.temporal.ChronoUnit

class YdbScenarioDaoSpec extends YdbSpecBase {
  private lazy val scenarioDao: ScenarioDao[F] = new YdbScenarioDao(ydb)

  implicit def clearableScenarioDao[C[_]]: Clearable[ScenarioDao[C]] =
    () =>
      ydb
        .runTx(
          ydb.execute(
            s"""
               |DELETE FROM scenarios;
               |""".stripMargin
          )
        )
        .void
        .await

  before {
    scenarioDao.clear()
  }

  private val chooseScenario: ExternalGraphScenario = scenarioFromFile("choose_scenario.json")
  private val greetingScenario: ExternalGraphScenario = scenarioFromFile("greeting_scenario.json")
  implicit val valueArb: Arbitrary[ExternalGraphScenario] = Arbitrary(Gen.oneOf(greetingScenario, chooseScenario))

  "ScenarioDaoImpl" should {

    "store and retrieve multiple scenarios by multiple PKs" in {
      val externalGraphOne = Arbitraries.generate[ExternalGraphScenario]()
      val externalGraphTwo = Arbitraries.generate[ExternalGraphScenario]()

      val first =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 1),
          externalGraphOne,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )

      val second =
        ScenarioRow(
          ScenarioKey("2", Domain.Realty, 1),
          externalGraphTwo,
          now(),
          "SomeUserSavedHighVersion@yandex-team.ru"
        )
      val third =
        ScenarioRow(ScenarioKey("3", Domain.Autoru, 1), externalGraphTwo, now(), "RandomUser@yandex-team.ru")
      val fourth =
        ScenarioRow(ScenarioKey("4", Domain.Realty, 1), externalGraphTwo, now(), "RandomUser@yandex-team.ru")
      saveScenario(first, Domain.Autoru)
      saveScenario(second, Domain.Realty)
      saveScenario(third, Domain.Autoru)
      saveScenario(fourth, Domain.Realty)

      val keyOneToSearch = ScenarioKey("1", Domain.Autoru, 1)
      val keyTwoToSearch = ScenarioKey("2", Domain.Autoru, 1)
      val keyFourToSearch = ScenarioKey("4", Domain.Realty, 1)
      val retrievedScenarios = scenarioDao.getScenarios(Seq(keyOneToSearch, keyTwoToSearch, keyFourToSearch)).await

      retrievedScenarios shouldBe Seq(first, fourth)
    }

    "store and retrieve scenarios" in {
      val externalGraph = Arbitraries.generate[ExternalGraphScenario]()
      val record =
        ScenarioRow(ScenarioKey("1", Domain.Autoru, 1), externalGraph, now(), "SomeUser@yandex-team.ru")
      saveScenario(record, Domain.Autoru)
      val retrievedScenario = scenarioDao.getScenario(record.scenarioKey).await
      retrievedScenario shouldBe Some(record)

    }

    "store and get highest version of scenario" in {
      val externalGraphScenarioLowestVersion = Arbitraries.generate[ExternalGraphScenario]()
      val externalGraphScenarioHighestVersion = Arbitraries.generate[ExternalGraphScenario]()
      val lowestRecord =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 1),
          externalGraphScenarioLowestVersion,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )
      val highestRecord =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 2),
          externalGraphScenarioHighestVersion,
          now(),
          "SomeUserSavedHighVersion@yandex-team.ru"
        )
      val recordWithWrongDomain =
        ScenarioRow(
          ScenarioKey("1", Domain.Realty, 1),
          externalGraphScenarioHighestVersion,
          now(),
          "SomeUserSavedHighVersion@yandex-team.ru"
        )
      saveScenario(lowestRecord, Domain.Autoru)
      saveScenario(highestRecord, Domain.Autoru)
      saveScenario(recordWithWrongDomain, Domain.Autoru)

      val retrievedScenarios = scenarioDao.getTheNewestScenariosByDomain(Domain.Autoru).await
      retrievedScenarios shouldBe Seq(highestRecord)

    }

    "save new not existing version" in {
      val recordToSave = Arbitraries.generate[ExternalGraphScenario]()
      val newScenario =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 1),
          recordToSave,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )

      scenarioDao.updateScenarios(Seq(newScenario), Domain.Autoru).await
      val result = scenarioDao.getTheNewestScenariosByDomain(Domain.Autoru).await
      result shouldBe Seq(newScenario)

    }

    "save new not existing with wrong version" in {
      val recordToSave = Arbitraries.generate[ExternalGraphScenario]()
      val newScenario =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 24),
          recordToSave,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )

      scenarioDao.updateScenarios(Seq(newScenario), Domain.Autoru).await
      val result = scenarioDao.getTheNewestScenariosByDomain(Domain.Autoru).await
      result.isEmpty shouldBe true

    }
    "update existing version" in {
      val externalGraphScenarioLowestVersion = Arbitraries.generate[ExternalGraphScenario]()
      val externalGraphScenarioHighestVersion = Arbitraries.generate[ExternalGraphScenario]()
      val lowestRecord =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 1),
          externalGraphScenarioLowestVersion,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )
      val highestRecord =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 2),
          externalGraphScenarioHighestVersion,
          now(),
          "SomeUserSavedHighVersion@yandex-team.ru"
        )

      saveScenario(lowestRecord, Domain.Autoru)
      scenarioDao.updateScenarios(Seq(highestRecord), Domain.Autoru).await
      val result = scenarioDao.getTheNewestScenariosByDomain(Domain.Autoru).await
      result shouldBe Seq(highestRecord)

    }

    "update existing with too high version" in {
      val externalGraphScenarioLowestVersion = Arbitraries.generate[ExternalGraphScenario]()
      val externalGraphScenarioHighestVersion = Arbitraries.generate[ExternalGraphScenario]()
      val lowestRecord =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 1),
          externalGraphScenarioLowestVersion,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )
      val highestRecord =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 24),
          externalGraphScenarioHighestVersion,
          now(),
          "SomeUserSavedHighVersion@yandex-team.ru"
        )
      saveScenario(lowestRecord, Domain.Autoru)
      saveScenario(highestRecord, Domain.Autoru)
      val result = scenarioDao.getTheNewestScenariosByDomain(Domain.Autoru).await
      result shouldBe Seq(lowestRecord)

    }

    "update existing with too low version" in {
      val externalGraphScenarioLowestVersion = Arbitraries.generate[ExternalGraphScenario]()
      val externalGraphScenarioHighestVersion = Arbitraries.generate[ExternalGraphScenario]()

      val v1 =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 1),
          externalGraphScenarioLowestVersion,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )
      val v2 =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 2),
          externalGraphScenarioLowestVersion,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )
      val v3 =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 3),
          externalGraphScenarioLowestVersion,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )
      val v4 =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 4),
          externalGraphScenarioLowestVersion,
          now(),
          "SomeUserSavedLowVersion@yandex-team.ru"
        )
      val lowVersionScenario =
        ScenarioRow(
          ScenarioKey("1", Domain.Autoru, 1),
          externalGraphScenarioHighestVersion,
          now(),
          "SomeUserSavedHighVersion@yandex-team.ru"
        )

      saveScenario(v1, Domain.Autoru)
      saveScenario(v2, Domain.Autoru)
      saveScenario(v3, Domain.Autoru)
      saveScenario(v4, Domain.Autoru)
      val executionResult = scenarioDao.updateScenarios(Seq(lowVersionScenario), Domain.Autoru).await
      executionResult.isLeft.shouldBe(true)
      val result = scenarioDao.getTheNewestScenariosByDomain(Domain.Autoru).await
      result shouldBe Seq(v4)

    }

    "get all scenario ids in domain" in {
      val author = "SomeUser@yandex-team.ru"
      val domain = Domain.Autoru
      scenarioDao.getScenarioIds(domain).await shouldBe Seq.empty

      // put the first version of the scenario '1', update (increment version),
      // and then put the first version of the scenario '2'
      Seq(
        generateScenarioRow("1", domain, 1, author),
        generateScenarioRow("1", domain, 2, author),
        generateScenarioRow("2", domain, 1, author)
      ).map(saveScenario(_, domain))
      scenarioDao.getScenarioIds(domain).await should contain theSameElementsAs Seq("1", "2")
    }

    "get all scenario metaInfo" in {
      val id = "1"
      val domain = Domain.Autoru
      val scenarios =
        Seq(
          generateScenarioRow(id, domain, 1, "SomeUser1"),
          generateScenarioRow(id, domain, 2, "SomeUser2"),
          generateScenarioRow(id, domain, 3, "SomeUser3")
        )
      val expected =
        scenarios.map(scenario =>
          ScenarioMetaInfo(scenario.updatedBy, scenario.scenarioKey.version, scenario.updateTime)
        )
      scenarios.map(saveScenario(_, domain))
      val actual = scenarioDao.getMetaInfo(domain, id).await
      actual.size shouldBe 3
      actual should contain theSameElementsAs expected
    }
  }

  private def now() = Instant.now().truncatedTo(ChronoUnit.MICROS)

  private def generateScenarioRow(id: String, domain: Domain, version: Int, author: String): ScenarioRow =
    ScenarioRow(ScenarioKey(id, domain, version), Arbitraries.generate[ExternalGraphScenario](), now(), author)

  private def saveScenario(scenarioRow: ScenarioRow, domain: Domain): Either[Seq[Failure], Ok.type] = {
    scenarioDao.updateScenarios(Seq(scenarioRow), domain).await
  }

}
