package ru.yandex.realty.unification.unifier.processor.unifiers;

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.context.CacheHolder
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.CategoryType
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.model.offer.SalesAgentCategory
import ru.yandex.realty.model.offer.Transaction
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.model.raw.RawSalesAgentOld
import ru.yandex.realty.storage.verba.VerbaDictionary
import ru.yandex.realty.storage.verba.VerbaStorage
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.verba2.model.Dictionary

import java.util.Collections

import scala.collection.JavaConverters._

/**
  * @author Anton Irinev (airinev@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class SaleAgentUnifierTest extends AsyncSpecBase {

  val regionGraph = RegionGraphProtoConverter.deserialize(
    IOUtils.gunzip(
      getClass().getClassLoader().getResourceAsStream("region_graph-8-2.data")
    )
  )

  val verbaStorage = new VerbaStorage(
    Collections.singleton(
      new Dictionary(
        1L,
        1L,
        VerbaDictionary.AGENCY_NAME_SIGNS.getCode(),
        VerbaDictionary.AGENCY_NAME_SIGNS.getCode(),
        VerbaDictionary.AGENCY_NAME_SIGNS.getCode(),
        Seq.empty.asJava
      )
    )
  )

  val unifier = new SaleAgentUnifier(
    new CacheHolder(),
    ProviderAdapter.create(verbaStorage),
    ProviderAdapter.create(regionGraph)
  )

  implicit val traced = Traced.empty

  "SaleAgentUnifier" should {
    "test agent phone unification" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.AGENCY, false);

      unifier.unify(offerWrapper, traced)

      val phones = offerWrapper.getOffer().getSaleAgent().getHalfUnifiedPhones();
      phones.size shouldEqual 1
      "+7(906)3233432" shouldEqual phones.get(0)
    }

    "testSetPrivateAgentIfOwnerAndResellerIsTrue" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.OWNER, true);

      unifier.unify(offerWrapper, traced)

      val saleAgent = offerWrapper.getOffer().getSaleAgent();
      SalesAgentCategory.PRIVATE_AGENT shouldEqual saleAgent.getCategory()
    }

    "testSaveOwnerIfResellerIsFalse" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.OWNER, false);

      unifier.unify(offerWrapper, traced);

      val saleAgent = offerWrapper.getOffer().getSaleAgent();
      SalesAgentCategory.OWNER shouldEqual saleAgent.getCategory()
    }

    "testSaveAgencyIfResellerIsFalse" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.AGENCY, false);

      unifier.unify(offerWrapper, traced);

      val saleAgent = offerWrapper.getOffer().getSaleAgent();
      SalesAgentCategory.AGENCY shouldEqual saleAgent.getCategory()
    }

    "testSaveAgencyIfResellerIsTrue" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.AGENCY, true);

      unifier.unify(offerWrapper, traced);

      val saleAgent = offerWrapper.getOffer().getSaleAgent();
      SalesAgentCategory.AGENCY shouldEqual saleAgent.getCategory()
    }

    "testSetPrivateAgentIfPaymentTypeIsNaturalAndSalesCategoryIsAgent" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.AGENT, true);
      offerWrapper.getOffer().setPaymentType(PaymentType.NATURAL_PERSON);

      unifier.unify(offerWrapper, traced);

      val saleAgent = offerWrapper.getOffer().getSaleAgent();
      SalesAgentCategory.PRIVATE_AGENT shouldEqual saleAgent.getCategory()
    }

    "testSaveAgentIfPaymentTypeIsJuridical" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.AGENT, true);
      offerWrapper.getOffer().setPaymentType(PaymentType.JURIDICAL_PERSON);

      unifier.unify(offerWrapper, traced);

      val saleAgent = offerWrapper.getOffer().getSaleAgent();
      SalesAgentCategory.AGENT shouldEqual saleAgent.getCategory()
    }

    "testSetAgencyIfOwnerAndOfferForNotCommercialAndNotFromVos" in {
      val offerWrapper = buildOfferWithSalesCategoryAndPlace(SalesAgentCategory.OWNER, true, false)
      offerWrapper.getOffer().setCategoryType(CategoryType.APARTMENT);

      unifier.unify(offerWrapper, traced);

      val saleAgent = offerWrapper.getOffer().getSaleAgent();
      SalesAgentCategory.AGENCY shouldEqual saleAgent.getCategory()
    }

    "testSaveOnerIfOfferForCommercialAndNotFromVos" in {
      val offerWrapper = buildOfferWithSalesCategoryAndPlace(SalesAgentCategory.OWNER, false, false)
      offerWrapper.getOffer().setCategoryType(CategoryType.COMMERCIAL);

      unifier.unify(offerWrapper, traced);

      val saleAgent = offerWrapper.getOffer().getSaleAgent()
      SalesAgentCategory.OWNER shouldEqual saleAgent.getCategory()
    }

    "testSetPrivateAgentIfExistsAgencyFeeAndCategoryOwner" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.OWNER, false);
      val transaction = new Transaction()
      transaction.setAgentFee(100.0f)
      offerWrapper.getOffer().setTransaction(transaction)

      unifier.unify(offerWrapper, traced)

      val saleAgent = offerWrapper.getOffer().getSaleAgent()
      SalesAgentCategory.PRIVATE_AGENT shouldEqual saleAgent.getCategory()
    }

    "testSaveDeveloperIfResellerIsNull" in {
      val offerWrapper = buildOfferWithSalesCategoryFromVos(SalesAgentCategory.DEVELOPER, null)

      unifier.unify(offerWrapper, traced)

      val saleAgent = offerWrapper.getOffer().getSaleAgent()
      SalesAgentCategory.DEVELOPER shouldEqual saleAgent.getCategory()
    }
  }

  private def buildOfferWithSalesCategoryFromVos(
    category: SalesAgentCategory,
    reseller: java.lang.Boolean
  ): OfferWrapper = {
    buildOfferWithSalesCategoryAndPlace(category, reseller, true)
  }

  private def buildOfferWithSalesCategoryAndPlace(
    category: SalesAgentCategory,
    reseller: java.lang.Boolean,
    isFromVos: Boolean
  ): OfferWrapper = {
    val rawOffer = new RawOfferImpl();
    rawOffer.setFromVos(isFromVos);
    val agent = new RawSalesAgentOld();
    agent.setReseller(reseller);
    agent.setCategory(category);
    agent.setPhone("+7(906)323-34-32");
    rawOffer.setSalesAgent(agent);

    val offerWrapper = new OfferWrapper(rawOffer);
    offerWrapper.getOffer().createAndGetSaleAgent();
    offerWrapper.getOffer().setLocation(new Location());
    offerWrapper.getOffer().getLocation().setGeocoderId(213);
    offerWrapper
  }
}
