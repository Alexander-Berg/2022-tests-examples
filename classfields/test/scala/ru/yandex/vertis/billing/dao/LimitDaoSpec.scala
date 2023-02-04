package ru.yandex.vertis.billing.dao

import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.LimitDao.ByCampaign
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.service.CampaignService

/**
  * Specs on any [[LimitDao]].
  *
  * @author dimas
  */
trait LimitDaoSpec extends AnyWordSpec with Matchers with TryValues {

  protected def limitDao: LimitDao

  protected def customerDao: CustomerDao
  protected def orderDao: OrderDao
  protected def campaignDao: CampaignDao

  def withFunds(funds: Funds)(settings: LimitSetting) = settings match {
    case d @ LimitSetting.Daily(_, _) =>
      d.copy(funds = funds)
    case w @ LimitSetting.Weekly(_, _) =>
      w.copy(funds = funds)
  }

  "LimitDao" should {
    "insert, get and update limit setting" in {
      val campaign = createCampaign("foo")

      val setting = LimitSettingGen.next
      limitDao.update(campaign.id, setting).get

      val retrieved = limitDao.get(campaign.id).get
      retrieved should have size 1
      retrieved.head should be(setting)

      val updated = withFunds(funds = setting.funds + 10)(setting)
      limitDao.update(campaign.id, updated).get

      val retrieved2 = limitDao.get(campaign.id).get
      retrieved2 should have size 1
      retrieved2.head should be(updated)

    }

    "insert batch limit settings" in {
      val campaign = createCampaign("bar")

      val setting = LimitSettingGen.next
      val dailySetting = LimitSetting.Daily(setting.funds, setting.effectiveSince)
      val weeklySettings = LimitSetting.Weekly(setting.funds, setting.effectiveSince)
      val settings = Iterable(dailySetting, weeklySettings)

      limitDao.update(campaign.id, settings).get

      val retrieved = limitDao.get(campaign.id).get
      retrieved should have size 2
      retrieved should contain theSameElementsAs settings

      val updated = withFunds(funds = dailySetting.funds + 10)(dailySetting)
      limitDao.update(campaign.id, updated).get

      val updatedSettings = Iterable(updated, weeklySettings)
      val retrieved2 = limitDao.get(campaign.id).get
      retrieved2 should have size 2
      retrieved2 should contain theSameElementsAs updatedSettings
    }

    "insert, get all and skip" in {
      val ids = CampaignIdGen.next(3)
      ids.foreach { id =>
        createCampaign(id)
      }

      // todo(darl) могут сгенерироваться настройки с одинаковым временем
      // тогда одна из настроек пропадет, а тест флапнет
      val settings = LimitSettingGen.next(10).toSet

      for {
        id <- ids
        setting <- settings
      } limitDao.update(id, setting).get

      for {
        id <- ids
        all <- limitDao.getAll(ids)
        retrievedSettings = all.values.flatten.toSet
      } retrievedSettings.toList.sortBy(_.effectiveSince.getMillis) should
        be(settings.toList.sortBy(_.effectiveSince.getMillis))
    }

    "get nothing for empty ID" in {
      limitDao.getAll(Iterable.empty).get should
        be(Map.empty[CampaignId, Iterable[LimitSetting]])
    }

    "delete campaign's limits" in {
      val ids = CampaignIdGen.next(3)
      ids.foreach { id =>
        createCampaign(id)
      }
      val settings = LimitSettingGen.next(10).toSet
      for {
        id <- ids
        setting <- settings
      } limitDao.update(id, setting).get

      for {
        limits <- limitDao.getAll(ids)
        _ = limits.keySet shouldBe ids.toSet
      } ()

      val firstId = ids.head
      val leftIds = ids.tail
      limitDao.delete(ByCampaign(firstId)).success

      for {
        limits <- limitDao.getAll(ids)
        _ = limits.keySet should be(leftIds.toSet)
      } ()
    }
  }

  private def createCampaign(id: CampaignId): CampaignHeader = {
    val customer = CustomerHeaderGen.next
    customerDao.create(customer).get
    val orderProperties = OrderPropertiesGen.next
    val order = orderDao.create(customer.id, orderProperties).get
    val product = ProductGen.next
    val settings = CampaignSettings.Default
    val source = CampaignService.Source(Some("foo"), order.id, product, settings, Some(id), Iterable.empty)
    campaignDao.create(customer.id, source, DuplicationPolicy.AllowDuplicates).get
  }

}
