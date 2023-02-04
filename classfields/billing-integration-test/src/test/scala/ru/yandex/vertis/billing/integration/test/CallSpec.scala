package ru.yandex.vertis.billing.integration.test

import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.BillingEvent.CampaignActiveStatusChangeEvent
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.integration.test.environment.{Data, IntegrationSpec}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.CampaignService.EnabledCampaignsFilter
import ru.yandex.vertis.billing.service.OrderService
import ru.yandex.vertis.billing.util._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._

class CallSpec
  extends AnyWordSpec
  with AsyncSpecBase
  with Matchers
  with MockitoSupport
  with BeforeAndAfterEach
  with IntegrationSpec {

  override protected def beforeEach(): Unit = {
    clearDatabase()
    campaignActiveStatusProducer.clear()
    messageDeliveryService.clear()
    s3CampaignStorage.clear()
    super.beforeEach()
  }

  "Звонки для AutoRu матчим по callsSettings.objectId у РК с objectId у звонка" in {
    val callObjectId = nextSeqId.toString
    val otherCallObjectId = nextSeqId.toString
    val customer = createCustomer()
    val order = createOrder(customer, balance = 1000)
    val campaign = createCallCampaign(order, callObjectId = callObjectId)

    // Матчинг будем оценивать по тому, что за звонок списались деньги
    tms.priceService.upsertPrices(campaign.id -> 100)

    tms.teleponyCallProcessor.process(Seq(Data.getAutoruCall(objectId = otherCallObjectId))).futureValue
    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm)
    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(order.id)))
      .get
      .head
      .balance2
      .totalSpent shouldBe 0

    tms.teleponyCallProcessor.process(Seq(Data.getAutoruCall(objectId = callObjectId))).futureValue
    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm)
    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(order.id)))
      .get
      .head
      .balance2
      .totalSpent shouldBe 100

  }

  "Должны использоваться отдельные РК для звонков call:cars:used" in {
    val dealer = s"dealer-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer, balance = 1000)
    val carNewCampaign = createCallCampaign(order, dealer, "call")
    val carNewCall = Data.getAutoruCall(dealer, "section=NEW")
    val carUsedCampaign = createCallCampaign(order, dealer, "call:cars:used")
    val carUsedCall = Data.getAutoruCall(dealer, "section=USED")

    tms.priceService.upsertPrices(carNewCampaign.id -> 100, carUsedCampaign.id -> 300)

    tms.teleponyCallProcessor.process(Seq(carNewCall, carUsedCall)).futureValue

    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm)

    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(order.id)))
      .get
      .head
      .balance2
      .totalSpent shouldBe 400
  }

  "По достижении лимитов, РК должна становиться неактивной" in {
    val dealer = s"dealer-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer, balance = 1000)
    val campaign = createCallCampaign(order, dealer)
    updateCampaign(campaign, dailyLimit = Some(TypedLimitSource(Some(200), Some(200))))
    tms.priceService.upsertPrices(campaign.id -> 100)

    tms.teleponyCallProcessor.process(Seq(Data.getAutoruCall(dealer))).futureValue

    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, indexer.campaignIndexerTask)

    indexer.asyncCampaignProvider
      .getValuable(EnabledCampaignsFilter.All)
      .futureValue
      .exists(_.getId == campaign.id) shouldBe true

    tms.teleponyCallProcessor.process(Seq(Data.getAutoruCall(dealer))).futureValue

    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, indexer.campaignIndexerTask)

    indexer.asyncCampaignProvider
      .getValuable(EnabledCampaignsFilter.All)
      .futureValue
      .exists(_.getId == campaign.id) shouldBe false
  }

  "Не отправляем события CampaignActiveStatusChange в кафку для старых РК" in {
    val dealer = s"dealer-$nextSeqId"
    val customer = createCustomer()
    val order1 = createOrder(customer, balance = 1000)
    val order2 = createOrder(customer, balance = 1000)
    val campaign1 = createCallCampaign(order1, dealer)
    val campaign2 = createCallCampaign(order2, dealer)

    runTasks(indexer.campaignIndexerTask)

    campaignActiveStatusProducer.getMessages.map(_.getCampaignId) shouldBe List(campaign2.id)
    messageDeliveryService
      .getMessages[CampaignActiveStatusChangeEvent]
      .map(_.getCampaignId)
      .toSet shouldBe Set(campaign1.id, campaign2.id)
  }

  "Обновляем цену звонка при переобиливании" in {
    val dealer = s"dealer-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer, balance = 1000)
    val campaign = createCallCampaign(order, dealer)
    tms.priceService.upsertPrices(campaign.id -> 100)

    tms.teleponyCallProcessor.process(Seq(Data.getAutoruCall(dealer))).futureValue

    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, indexer.campaignIndexerTask)

    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(order.id)))
      .get
      .head
      .balance2
      .totalSpent shouldBe 100

    tms.priceService.upsertPrices(campaign.id -> 200)

    // Для переобиливания запускаем таск calls-revenue-statistics-update с заданным окном
    val from = DateTimeUtils.IsoDateTimeFormatter.print(System.currentTimeMillis - 1000000)
    val to = DateTimeUtils.IsoDateTimeFormatter.print(System.currentTimeMillis)
    val revenueStatisticsTask = tms.CallsRevenue.RevenueStatisticsTask
      .withOverrides(ConfigFactory.parseString(s"""{from = "$from", to = "$to"}"""))

    runTasks(revenueStatisticsTask, tms.withdrawForm, indexer.campaignIndexerTask)

    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(order.id)))
      .get
      .head
      .balance2
      .totalSpent shouldBe 200
  }

  "Переобиливаем звонок если был любой другой звонок в этот период" in {
    val dealer = s"dealer-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer, balance = 1000)
    val campaign = createCallCampaign(order, dealer)

    val dealer2 = s"dealer-$nextSeqId"
    val customer2 = createCustomer()
    val order2 = createOrder(customer2, balance = 1000)
    val campaign2 = createCallCampaign(order2, dealer2)

    tms.priceService.upsertPrices(campaign.id -> 100, campaign2.id -> 100)

    tms.teleponyCallProcessor.process(Seq(Data.getAutoruCall(dealer))).futureValue

    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, indexer.campaignIndexerTask)

    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(order.id)))
      .get
      .head
      .balance2
      .totalSpent shouldBe 100

    tms.priceService.upsertPrices(campaign.id -> 200)

    tms.teleponyCallProcessor.process(Seq(Data.getAutoruCall(dealer2))).futureValue

    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, indexer.campaignIndexerTask)

    tms.orderDao
      .get(OrderService.GetFilter.ForOrderIds(List(order.id)))
      .get
      .head
      .balance2
      .totalSpent shouldBe 200
  }

  "[Autoru] Не списываем деньги за звонки от модераторов" in {
    val callObjectId = s"call-object-id-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer)
    val campaign = createCallCampaign(order, callObjectId)
    val call = Data.getAutoruCall(objectId = callObjectId, isModeration = true)
    tms.priceService.upsertPrices(campaign.id -> 100)

    tms.teleponyCallProcessor.process(Seq(call)).futureValue
    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm)

    getOrder(order.id).balance2.totalSpent shouldBe 0
  }

  "[Autoru] Не отправляем на арбитраж звонки от модераторов" in {
    val callObjectId = s"call-object-id-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer)
    createCallCampaign(order, callObjectId)
    val call = Data.getAutoruCall(objectId = callObjectId, isModeration = true)

    tms.teleponyCallProcessor.process(Seq(call)).futureValue
    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, tms.hoboPushTask)

    tms.hoboClient.getCallIds.contains(call.getCallId) shouldBe false
  }

  "[Autoru] Не списываем деньги за звонки короче 60 секунд" in {
    val callObjectId = s"call-object-id-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer)
    val campaign = createCallCampaign(order, callObjectId)
    val call = Data.getAutoruCall(objectId = callObjectId, duration = 59.seconds, talkDuration = 58.seconds)
    tms.priceService.upsertPrices(campaign.id -> 100)

    tms.teleponyCallProcessor.process(Seq(call)).futureValue
    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm)

    getOrder(order.id).balance2.totalSpent shouldBe 0
  }

  "[Autoru] Отправляем в арбитраж звонки длительностью от 20 до 60 секунд" in {
    val callObjectId = s"call-object-id-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer)
    createCallCampaign(order, callObjectId)
    val call = Data.getAutoruCall(objectId = callObjectId, duration = 21.seconds, talkDuration = 17.seconds)

    tms.teleponyCallProcessor.process(Seq(call)).futureValue
    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, tms.hoboPushTask)

    tms.hoboClient.getCallIds.contains(call.getCallId) shouldBe true
  }

  "[Autoru] Не отправляем в арбитраж звонки короче 20 секунд" in {
    val callObjectId = s"call-object-id-$nextSeqId"
    val customer = createCustomer()
    val order = createOrder(customer)
    createCallCampaign(order, callObjectId)
    val call = Data.getAutoruCall(objectId = callObjectId, duration = 18.seconds, talkDuration = 17.seconds)

    tms.teleponyCallProcessor.process(Seq(call)).futureValue
    runTasks(tms.CallsRevenue.RevenueStatisticsTask, tms.withdrawForm, tms.hoboPushTask)

    tms.hoboClient.getCallIds.contains(call.getCallId) shouldBe false
  }

}
