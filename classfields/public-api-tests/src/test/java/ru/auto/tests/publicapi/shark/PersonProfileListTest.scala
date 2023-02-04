package ru.auto.tests.publicapi.shark

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.SHARK
import ru.auto.tests.publicapi.model.VertisSharkBlockGenderBlock.GenderTypeEnum
import ru.auto.tests.publicapi.model._
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import java.time.temporal.ChronoUnit
import java.time.{OffsetDateTime, ZoneId}
import scala.annotation.meta.getter
import scala.jdk.CollectionConverters._
import scala.util.Random


@DisplayName("GET /shark/person-profile/list/{user}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PersonProfileListTest {
  @(Rule@getter)
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
  def shouldGetPersonProfileOfUser(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId
    val personalProfile = new VertisSharkPersonProfile()
      .name(new VertisSharkBlockNameBlock().nameEntity(new VertisSharkEntityNameEntity().name("Иван").surname("Иванов").patronymic("Иванович")))
      .gender(new VertisSharkBlockGenderBlock().genderType(GenderTypeEnum.MALE))
      .passportRf(new VertisSharkBlockPassportRfBlock().passportRfEntity(
        new VertisSharkEntityPassportRfEntity().series(Random.between(1000, 9999).toString)
          .number(Random.between(100000, 999999).toString)
          .departCode(s"${Random.between(100, 999)}-${Random.between(100, 999)}")
          .departName("ОУФМС").issueDate(OffsetDateTime.now(ZoneId.of("UTC"))
          .minusYears(2L)
          .truncatedTo(ChronoUnit.DAYS))
      ))
    val creditApplicationId = api.shark.creditApplicationCreate()
      .reqSpec(defaultSpec)
      .body(new VertisSharkCreditApplicationSource().borrowerPersonProfile(personalProfile))
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))
      .getCreditApplication
      .getId

    val response = api.shark.personProfileListShark()
      .reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val respProfile = response.getPersonProfiles.asScala.head
    Assertions.assertThat(respProfile.getName).isEqualTo(personalProfile.getName)
    Assertions.assertThat(respProfile.getGender).isEqualTo(personalProfile.getGender)
    Assertions.assertThat(respProfile.getPassportRf).isEqualTo(personalProfile.getPassportRf)
  }
}
