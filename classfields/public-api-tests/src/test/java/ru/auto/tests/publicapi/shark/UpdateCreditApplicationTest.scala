package ru.auto.tests.publicapi.shark

import java.time.OffsetDateTime

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.AUTH_ERROR
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("POST /shark/credit-application/update/{credit_application_id}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdateCreditApplicationTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(SHARK)
  def shouldSee403WhenNoAuth(): Unit = {
    api.shark.creditApplicationUpdate()
      .creditApplicationIdPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(SHARK)
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.shark.creditApplicationUpdate().reqSpec(defaultSpec)
      .creditApplicationIdPath(getRandomString)
      .body(new VertisSharkCreditApplicationSource)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(AUTH_ERROR).hasStatus(ERROR)
      .hasDetailedError(AUTH_ERROR.getValue)
  }

  @Test
  @Owner(SHARK)
  def shouldChangeRequirementsBlockInCreditApplication(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val creditApplicationId = adaptor.createCreditApplication(sessionId).getCreditApplication.getId
    val requirements = new VertisSharkCreditApplicationRequirements()
      .maxAmount(Random.nextLong(5000000)).initialFee(Random.nextLong(10000))
      .termMonths(Random.nextInt(48))

    api.shark.creditApplicationUpdate()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .body(new VertisSharkCreditApplicationSource().requirements(requirements))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val creditApplicationAfterChange = adaptor.getCreditApplication(sessionId, creditApplicationId)
    assertThat(creditApplicationAfterChange.getCreditApplication.getRequirements)
      .hasMaxAmount(requirements.getMaxAmount)
      .hasInitialFee(requirements.getInitialFee)
      .hasTermMonths(requirements.getTermMonths)
  }

  @Test
  @Owner(SHARK)
  def shouldChangePersonalProfileBlockInCreditApplication(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val creditApplicationId = adaptor.createCreditApplication(sessionId).getCreditApplication.getId
    val personalProfile = new VertisSharkPersonProfile()
      .name(new VertisSharkBlockNameBlock().nameEntity(new VertisSharkEntityNameEntity().name("Тимон").surname("Тимонов").patronymic("Тимоныч")))
      .gender(new VertisSharkBlockGenderBlock().genderType(VertisSharkBlockGenderBlock.GenderTypeEnum.FEMALE))
      .passportRf(new VertisSharkBlockPassportRfBlock().passportRfEntity(
        new VertisSharkEntityPassportRfEntity().series(Random.between(1000, 9999).toString)
          .number(Random.between(100000, 999999).toString)
          .departCode(s"${Random.between(100, 999)}-${Random.between(100, 999)}")
          .departName("МВД").issueDate(OffsetDateTime.now.minusYears(1L))))

    api.shark.creditApplicationUpdate()
      .reqSpec(defaultSpec)
      .creditApplicationIdPath(creditApplicationId)
      .body(new VertisSharkCreditApplicationSource().borrowerPersonProfile(personalProfile))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val creditApplicationAfterChange = adaptor.getCreditApplication(sessionId, creditApplicationId)
    val actualPersonalProfile = creditApplicationAfterChange.getCreditApplication.getBorrowerPersonProfile
    assertThat(actualPersonalProfile.getGender).hasGenderType(personalProfile.getGender.getGenderType)
    assertThat(actualPersonalProfile.getName.getNameEntity)
      .hasName(personalProfile.getName.getNameEntity.getName)
      .hasSurname(personalProfile.getName.getNameEntity.getSurname)
      .hasPatronymic(personalProfile.getName.getNameEntity.getPatronymic)
    assertThat(actualPersonalProfile.getPassportRf.getPassportRfEntity)
      .hasSeries(personalProfile.getPassportRf.getPassportRfEntity.getSeries)
      .hasNumber(personalProfile.getPassportRf.getPassportRfEntity.getNumber)
      .hasDepartCode(personalProfile.getPassportRf.getPassportRfEntity.getDepartCode)
      .hasDepartName(personalProfile.getPassportRf.getPassportRfEntity.getDepartName)
  }
}
