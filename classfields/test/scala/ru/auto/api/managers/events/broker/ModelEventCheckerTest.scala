package ru.auto.api.managers.events.broker

import auto.events.Model
import auto.events.common.Common
import com.google.protobuf.util.Timestamps
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.testkit.TestData
import ru.auto.api.{ApiOfferModel, AsyncTasksSupport, BaseSpec, DummyOperationalSupport}
import ru.yandex.vertis.mockito.MockitoSupport

import java.lang.{Integer => JInt, Long => JLong}
import scala.jdk.CollectionConverters._

class ModelEventCheckerTest
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with AsyncTasksSupport
  with DummyOperationalSupport {

  private val modelEventChecker = new ModelEventChecker(TestData.tree)

  private val simpleListingEvent = {
    val builder = Model.Event.newBuilder()
    builder.setTimestamp(Timestamps.fromMillis(1647205200007L))
    builder.setEventTime(Timestamps.fromMillis(1647205100007L))
    builder.setEventType(Model.EventType.SNIPPET_SHOW)
    builder.setOriginalRequestId("dd5b2408231f925d7f6c842a0ef74801")
    builder.setEventId("dec9f1de1d67ece")

    val userInfoBuilder = builder.getUserInfoBuilder
    userInfoBuilder.getAbtInfoBuilder.setExpboxes("4,5,6")
    userInfoBuilder.setUserId("")
    userInfoBuilder.setUserUuid("g622e586a2jf09fv5j531ua3kovq79nm.e4210f16e3af30d001796839c225a68c")
    userInfoBuilder.addAllRid(List(JInt.valueOf(213)).asJava)
    userInfoBuilder.setPortalRid(213)

    val contextBuilder = builder.getContextBuilder
    contextBuilder.setService(Common.ContextService.SERVICE_AUTORU)
    contextBuilder.setPage(Common.ContextPage.PAGE_LISTING)
    contextBuilder.setBlock(Common.ContextBlock.BLOCK_LISTING)

    builder.build()
  }

  "ModelEventChecker" should {
    "should not find any errors for SNIPPET_SHOW for single offer on DESKTOP" in {
      val builder = simpleListingEvent.toBuilder
      builder.setApp(Model.ClientType.DESKTOP)
      builder.setAppVersion("9f45f78c432")

      val userInfoBuilder = builder.getUserInfoBuilder
      userInfoBuilder.getWebUserInfoBuilder.setUserYandexuid("947961866160537")
      userInfoBuilder.getWebUserInfoBuilder.setUserAgent(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0.2 Safari/605.1.15"
      )

      val contextBuilder = builder.getContextBuilder
      contextBuilder.setReferer("https://auto.ru/moskva/cars/jeep/all/?year_from=2012&sort=price-asc")
      contextBuilder.setRereferer("https://auto.ru/cars/used/sale/kia/sportage/1106023116-e6b3ac57/")

      val showBuilder = builder.getShowBuilder
      showBuilder.setIndex(7)
      showBuilder.setSelfType(Model.SelfType.TYPE_SINGLE)

      val offerBuilder = builder.getOfferBuilder
      offerBuilder.getSellerBuilder.addAllPhones(
        Seq(ApiOfferModel.Phone.newBuilder().setOriginal("+7 555 059-56-57").setPhone("+7 555 059-56-57").build()).asJava
      )
      offerBuilder.getSellerBuilder.setTeleponyInfo(ApiOfferModel.TeleponyInfo.newBuilder().build())
      offerBuilder.setOfferId("1114991796-c0786272")

      val checkedEvent = modelEventChecker.checkEvent(builder.build())

      checkedEvent.getEventVerificationPassed shouldBe true
      checkedEvent.getEventVerificationErrorsList.asScala shouldBe empty
    }

    "should not find any errors for CARD_SHOW for offer group on IOS" in {
      val builder = simpleListingEvent.toBuilder
      builder.setEventType(Model.EventType.CARD_VIEW)
      builder.setApp(Model.ClientType.IOS)
      builder.setAppVersion("12.4.0")

      val userInfoBuilder = builder.getUserInfoBuilder
      userInfoBuilder.getAppUserInfoBuilder.setIdfa("qweqeqwe")
      userInfoBuilder.getAppUserInfoBuilder.setIfv("dsfsdfds")
      userInfoBuilder.getAppUserInfoBuilder.setDeviceBrand("Apple")
      userInfoBuilder.getAppUserInfoBuilder.setDeviceModel("IPhone SE")
      userInfoBuilder.getAppUserInfoBuilder.setAdid("zxczxcz")

      val contextBuilder = builder.getContextBuilder
      contextBuilder.setReferer("PAGE_LISTING")
      contextBuilder.setRereferer("PAGE_MAIN")

      val showBuilder = builder.getShowBuilder
      showBuilder.setIndex(4)
      showBuilder.setSelfType(Model.SelfType.TYPE_GROUP)

      val offerGroupBuilder = builder.getOfferGroupBuilder
      offerGroupBuilder.setSize(3)
      offerGroupBuilder.setGroupingId("grouping")
      offerGroupBuilder.setPriceFrom(800000)
      offerGroupBuilder.setPriceTo(900000)
      offerGroupBuilder.addAllConfigurations(Seq(JLong.valueOf(340232L)).asJava)

      val checkedEvent = modelEventChecker.checkEvent(builder.build())

      checkedEvent.getEventVerificationPassed shouldBe true
      checkedEvent.getEventVerificationErrorsList.asScala shouldBe empty
    }

    "should not find any errors for SEARCH event on ANDROID" in {
      val builder = simpleListingEvent.toBuilder
      builder.setEventType(Model.EventType.SEARCH)
      builder.setApp(Model.ClientType.ANDROID)
      builder.setAppVersion("12.4.0")
      builder.setQueryId("dasdafsfsdfsd313")

      val userInfoBuilder = builder.getUserInfoBuilder
      userInfoBuilder.getAppUserInfoBuilder.setGaid("qweqeqwe")
      userInfoBuilder.getAppUserInfoBuilder.setDeviceBrand("Google")
      userInfoBuilder.getAppUserInfoBuilder.setDeviceModel("Pixel")
      userInfoBuilder.getAppUserInfoBuilder.setAdid("zxczxcz")

      val contextBuilder = builder.getContextBuilder
      contextBuilder.setReferer("PAGE_LISTING")
      contextBuilder.setRereferer("PAGE_MAIN")

      val searchBuilder = builder.getSearchBuilder
      searchBuilder.setOffersCount(1)
      // FIXME query changed after merging proto updates from AUTORUBACK-3188 - see also ModelEventChecker:156,
      searchBuilder.setSearchQuery(
        "rid=213%Москва&geo_radius=500&seller=3&state=2&custom=1&photo=1&sort=RELEVANCE_SECOND_DESC"
      )
      searchBuilder.setSort(Model.SortingType.RELEVANCE_SECOND_DESC)

      val checkedEvent = modelEventChecker.checkEvent(builder.build())

      checkedEvent.getEventVerificationPassed shouldBe true
      checkedEvent.getEventVerificationErrorsList.asScala shouldBe empty
    }
  }
}
