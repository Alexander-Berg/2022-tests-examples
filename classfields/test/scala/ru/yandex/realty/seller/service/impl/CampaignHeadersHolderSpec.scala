package ru.yandex.realty.seller.service.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.billing.gen.BillingGenerators
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.seller.model.product.ProductTypes
import ru.yandex.vertis.protobuf.ProtoInstanceProvider

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class CampaignHeadersHolderSpec
  extends SpecBase
  with BillingGenerators
  with ProtobufMessageGenerators
  with ProtoInstanceProvider {

  "CampaignHeadersHolder" should {
    "not fail on empty list" in {
      val holder = new CampaignHeadersHolder(Seq.empty)
      holder.getByUid(1, ProductTypes.Raising) shouldBe empty
      holder.getByPartnerId(1, ProductTypes.Raising) shouldBe empty
    }

    "find header by uid and all partner ids" in {
      val uid = 123L
      val partnerIds = Set(456L, 789L)
      val containingProduct = ProductTypes.Raising
      val header = campaignHeaderGen(Some(uid), partnerIds, ProductTypes.getCustomId(containingProduct)).next
      val otherProduct = ProductTypes.Promotion

      val holder = new CampaignHeadersHolder(Seq(header))
      holder.getByUid(uid, containingProduct) should (have size 1 and contain(header))
      holder.getByUid(9999, containingProduct) shouldBe empty
      holder.getByUid(uid, otherProduct) shouldBe empty

      partnerIds.foreach { partnerId =>
        holder.getByPartnerId(partnerId, containingProduct) should (have size 1 and contain(header))
        holder.getByPartnerId(partnerId, otherProduct) shouldBe empty
      }
      holder.getByPartnerId(9999, containingProduct) shouldBe empty
    }

    "find header all partner ids" in {
      val partnerIds = Set(456L, 789L)
      val containingProduct = ProductTypes.Raising
      val header = campaignHeaderGen(None, partnerIds, ProductTypes.getCustomId(containingProduct)).next
      val otherProduct = ProductTypes.Promotion

      val holder = new CampaignHeadersHolder(Seq(header))

      partnerIds.foreach { partnerId =>
        holder.getByPartnerId(partnerId, containingProduct) should (have size 1 and contain(header))
        holder.getByPartnerId(partnerId, otherProduct) shouldBe empty
      }
      holder.getByPartnerId(9999, containingProduct) shouldBe empty
    }

  }

}
