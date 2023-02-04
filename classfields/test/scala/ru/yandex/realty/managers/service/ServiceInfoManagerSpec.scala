package ru.yandex.realty.managers.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.bunker.BunkerResources
import ru.yandex.realty.clients.rent.RentClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.controllers.lk.stats.ShowsStatController
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.managers.useroffers.UserOffersManager
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.proto.api.v2.service.Notification.NotificationCase._
import ru.yandex.realty.proto.api.v2.service.Notification.RentOwnerPaymentInfoTodo.TodoItem
import ru.yandex.realty.proto.api.v2.service.ServiceInfo.Feature.{SERVICES_AND_NOTIFICATIONS, UNFILTERED_MODE}

import scala.collection.JavaConverters._
import ru.yandex.realty.proto.api.v2.service.ServiceInfo.Feature
import ru.yandex.realty.rent.proto.api.flats.FlatStatusNamespace.FlatStatus
import ru.yandex.realty.rent.proto.api.user.InnFilledTypeNamespace.InnFilledType
import ru.yandex.realty.rent.proto.api.user.OwnerCardsStatusNamespace.OwnerCardsStatus
import ru.yandex.realty.rent.proto.api.user.RentUser
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ServiceInfoManagerSpec extends AsyncSpecBase with RequestAware {

  implicit private val features: Set[Feature] = Set(SERVICES_AND_NOTIFICATIONS, UNFILTERED_MODE)
  implicit val traced: Traced = Traced.empty

  private def buildServiceInfoManager(rentUser: RentUser.Builder) = {
    val rentClient: RentClient = mock[RentClient]
    val userOffersManager: UserOffersManager = mock[UserOffersManager]
    val vosClientNG: VosClientNG = mock[VosClientNG]
    val showsStatController: ShowsStatController = mock[ShowsStatController]
    val bunkerResourcesProvider: Provider[BunkerResources] = mock[Provider[BunkerResources]]
    val regionGraphProvider: Provider[RegionGraph] = mock[Provider[RegionGraph]]

    (rentClient
      .getUser(_: PassportUser, _: Boolean)(_: Traced))
      .expects(*, *, *)
      .returns(Future.successful(rentUser.build()))

    new ServiceInfoManager(
      vosClientNG = vosClientNG,
      rentClient = rentClient,
      userOffersManager = userOffersManager,
      showsStatController = showsStatController,
      bunkerResourcesProvider = bunkerResourcesProvider,
      regionGraphProvider = regionGraphProvider
    )
  }

  "ServiceInfoManagerSpec" should {

    "don't display ownerPaymentInfoTodo notification when all done" in {
      val inn = InnFilledType.MANUAL_FILLED
      val card = OwnerCardsStatus.SINGLE_CARD_BOUND
      val rentUser: RentUser.Builder = RentUser.newBuilder().setInnFilledType(inn)
      rentUser.getCalculatedInfoBuilder
        .setHasActiveOwnedFlats(true)
        .addFlatsStatuses(FlatStatus.LOOKING_FOR_TENANT)
        .build()

      rentUser.setOwnerCardsStatus(card)

      val serviceInfoManager = buildServiceInfoManager(rentUser)

      val serviceInfo = serviceInfoManager.getServiceInfo(user = PassportUser(1)).futureValue
      val notifications = serviceInfo.getUnfilteredModeSettings.getNotificationGroupsList.asScala

      if (notifications.size > 0) fail("notification is not hidden")
    }

    "display ownerPaymentInfoTodo notification when not done" in {
      val inn = InnFilledType.SHOULD_MANUAL_FILLED
      val card = OwnerCardsStatus.NO_CARDS_BOUND
      val rentUser: RentUser.Builder = RentUser.newBuilder().setInnFilledType(inn)
      rentUser.getCalculatedInfoBuilder
        .setHasActiveOwnedFlats(true)
        .addFlatsStatuses(FlatStatus.LOOKING_FOR_TENANT)
        .build()

      rentUser.setOwnerCardsStatus(card)

      val serviceInfoManager = buildServiceInfoManager(rentUser)

      val serviceInfo = serviceInfoManager.getServiceInfo(user = PassportUser(1)).futureValue
      val notifications = serviceInfo.getUnfilteredModeSettings.getNotificationGroupsList.asScala

      if (notifications.size > 1) {
        fail("more than 1 notifications")
      }

      val todo = notifications
        .flatMap(_.getNotificationsList.asScala)
        .find(_.getNotificationCase == OWNER_PAYMENT_INFO_TODO)
        .getOrElse(fail("missing notification"))

      val todoItems = todo.getOwnerPaymentInfoTodo.getItemsList

      if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_INN)) {
        fail("missing todo item: AddInn")
      }

      if (todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_INN).exists(_.getDone)) {
        fail("todo item AddInn should not be done")
      }

      if (!todoItems.asScala.exists(_.getItemCase == TodoItem.ItemCase.ADD_PAYMENT_CARD)) {
        fail("missing todo item: AddPaymentCard")
      }

      if (todoItems.asScala.find(_.getItemCase == TodoItem.ItemCase.ADD_PAYMENT_CARD).exists(_.getDone)) {
        fail("todo item AddPaymentCard should not be done")
      }
    }
  }

  "display rentOwnerWithoutInn notification" in {

    val rentUser: RentUser.Builder = RentUser.newBuilder().setInnFilledType(InnFilledType.SHOULD_MANUAL_FILLED)
    rentUser.getCalculatedInfoBuilder
      .setHasActiveOwnedFlats(true)
      .addFlatsStatuses(FlatStatus.CONFIRMED)
      .build()

    rentUser.setOwnerCardsStatus(OwnerCardsStatus.SINGLE_CARD_BOUND)

    val serviceInfoManager = buildServiceInfoManager(rentUser)

    val serviceInfo = serviceInfoManager.getServiceInfo(user = PassportUser(1)).futureValue
    val notifications = serviceInfo.getUnfilteredModeSettings.getNotificationGroupsList.asScala
      .flatMap(_.getNotificationsList.asScala)

    if (notifications.size > 1) {
      fail("more than 1 notifications")
    }

    notifications.find(_.getNotificationCase == RENT_OWNER_WITHOUT_INN).getOrElse(fail("missing INN notification"))

  }

  "display rentOwnerWithoutCard notification" in {

    val rentUser: RentUser.Builder = RentUser.newBuilder().setInnFilledType(InnFilledType.SHOULD_MANUAL_FILLED)
    rentUser.getCalculatedInfoBuilder
      .setHasActiveOwnedFlats(true)
      .addFlatsStatuses(FlatStatus.RENTED)
      .build()

    rentUser.setOwnerCardsStatus(OwnerCardsStatus.NO_CARDS_BOUND)

    val serviceInfoManager = buildServiceInfoManager(rentUser)

    val serviceInfo = serviceInfoManager.getServiceInfo(user = PassportUser(1)).futureValue
    val notifications = serviceInfo.getUnfilteredModeSettings.getNotificationGroupsList.asScala
      .flatMap(_.getNotificationsList.asScala)

    if (notifications.size > 1) {
      fail("more than 1 notifications")
    }

    notifications.find(_.getNotificationCase == RENT_OWNER_WITHOUT_CARD).getOrElse(fail("missing card notification"))
  }
}
