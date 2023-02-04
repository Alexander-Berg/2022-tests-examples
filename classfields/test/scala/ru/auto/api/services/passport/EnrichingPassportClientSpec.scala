package ru.auto.api.services.passport

import org.mockito.Mockito
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.{ModelGenerators, RequestParams}
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.services.passport.util.UserProfileStubsProvider
import ru.auto.api.services.passport.util.UserProfileStubsProvider.{UserProfileStub, Userpic}
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._

class EnrichingPassportClientSpec extends BaseSpec with MockitoSupport {
  val underlying = mock[PassportClient]

  val stubbedFullName = "Grumpy Hearse"
  val stubbedUserpic = Userpic("tombstone", "//hell.org/tombstone.jpg/")
  val stubbedUserpicSizes = Map("size" -> s"${stubbedUserpic.url}size")

  val stubsProvider =
    new UserProfileStubsProvider(Seq(UserProfileStub(stubbedFullName, stubbedUserpic)), stubbedUserpicSizes.keys.toSeq)

  override protected def afterAll(): Unit = {
    Mockito.reset(underlying)
  }

  val client = new EnrichingPassportClient(underlying, stubsProvider)

  implicit private val trace: Traced = Traced.empty

  implicit private val request: RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Option("testUid")))
    r.setUser(PrivateUserRefGen.next)
    r.setApplication(Application.iosApp)
    r.setToken(TokenServiceImpl.iosApp)
    r.setTrace(trace)
    r
  }

  "EnrichingPassportClient" when {
    "login" should {
      "use stubs" in {
        when(underlying.login(?)(?)).thenReturnF(ModelGenerators.PassportLoginResultGen.filter { result =>
          val profile = result.getUser.getProfile.getAutoru
          profile.getFullName.isEmpty && profile.getAlias.isEmpty
        }.next)
        val result = client.login(ModelGenerators.PassportLoginParametersGen.next).futureValue
        val profile = result.getUser.getProfile.getAutoru
        profile.getFullName shouldBe stubbedFullName
        profile.getUserpic.getName shouldBe stubbedUserpic.name
        profile.getUserpic.getSizesMap.asScala shouldBe stubbedUserpicSizes
      }
      "not use stubs if fields are non empty" in {
        val fullName = "Foo Bar"
        val userpicName = "123-foobar"
        val userpicSizes = Map("foo" -> "http://foo.com/bar")
        val loginResult = {
          val builder = ModelGenerators.PassportLoginResultGen.next.toBuilder
          builder.getUserBuilder.getProfileBuilder.getAutoruBuilder
            .setFullName(fullName)
            .getUserpicBuilder
            .setName(userpicName)
            .putAllSizes(userpicSizes.asJava)
          builder.build()
        }
        val loginProfile = loginResult.getUser.getProfile.getAutoru
        when(underlying.login(?)(?)).thenReturnF(loginResult)
        val result = client.login(ModelGenerators.PassportLoginParametersGen.next).futureValue
        val profile = result.getUser.getProfile.getAutoru
        profile.getFullName shouldBe loginProfile.getFullName
        profile.getUserpic.getName shouldBe loginProfile.getUserpic.getName
        profile.getUserpic.getSizesMap shouldBe loginProfile.getUserpic.getSizesMap
      }
    }
  }
}
