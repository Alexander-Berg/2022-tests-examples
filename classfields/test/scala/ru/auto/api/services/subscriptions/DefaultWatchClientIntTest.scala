package ru.auto.api.services.subscriptions

import org.scalacheck.Gen
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.favorite.WatchManager
import ru.auto.api.model.{ModelGenerators, OfferID, UserRef}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.FutureMatchers._
import ru.yandex.vertis.subscriptions.api.ApiModel.Watch

import scala.jdk.CollectionConverters._

/**
  * Created by mcsim-gr on 18.10.17.
  */
class DefaultWatchClientIntTest extends HttpClientSuite {
  val uid = 20871601L
  val deviceId = "SomeTestDeviceId"

  override protected def config: HttpClientConfig =
    HttpClientConfig.apply("http", "subscriptions-api-test-int.slb.vertis.yandex.net", 80)

  val watchClient = new DefaultWatchClient(http)

  test("watch full case") {
    val privateUser = UserRef.user(uid)
    val anonUser = UserRef.anon(deviceId)

    val offerIDs = Gen.listOfN(3, ModelGenerators.OfferIDGen).next

    val addPath = WatchManager.watchPatch(add = Seq(offerIDs.head))
    watchClient.patchWatch(anonUser, addPath, WatchManager.AnonDefaultDeliveries) should beSuccessful

    var watch = watchClient.getWatch(anonUser).futureValue
    watch.getObjectsList.asScala should contain(offerIDs.head.toString)

    val multiPatch = WatchManager.watchPatch(add = offerIDs.tail, remove = Seq(offerIDs.head))
    watchClient.patchWatch(anonUser, multiPatch, WatchManager.AnonDefaultDeliveries) should beSuccessful

    watch = watchClient.getWatch(anonUser).futureValue
    watch.getObjectsList.asScala should contain(offerIDs.last.toString)
    watch.getObjectsList.asScala shouldNot contain(offerIDs.head.toString)

    watchClient.moveWatch(anonUser, privateUser, WatchManager.PrivateUserDefaultDeliveries) should beSuccessful

    watch = watchClient.getWatch(privateUser).futureValue
    watch.getObjectsList.asScala should contain(offerIDs.last.toString)

    val removePatch = WatchManager.watchPatch(remove = watch.getObjectsList.asScala.toSeq.map(OfferID.parse))
    watchClient.patchWatch(privateUser, removePatch, WatchManager.AnonDefaultDeliveries) should beSuccessful

    watch = watchClient.getWatch(privateUser).futureValue
    watch.getObjectsList.asScala shouldBe empty
  }

  test("get watch objects 404 handle") {
    val privateUser = UserRef.user(1111111111111111L)
    watchClient.getWatch(privateUser) should completeWith(Watch.getDefaultInstance)
  }

  test("move watch from unexciting user") {
    val privateUser = UserRef.user(uid)
    val anonRef = UserRef.anon("SomeUnexcitingAnonDeviceUid")
    watchClient.moveWatch(anonRef, privateUser, WatchManager.PrivateUserDefaultDeliveries) should beSuccessful
  }

  test("remove some watch objects from unexciting watch") {
    val privateUser = ModelGenerators.PrivateUserRefGen.next
    val offerIDs = Gen.listOfN(3, ModelGenerators.OfferIDGen).next
    val removePatch = WatchManager.watchPatch(remove = offerIDs)
    watchClient.patchWatch(privateUser, removePatch, WatchManager.PrivateUserDefaultDeliveries) should beSuccessful
  }
}
