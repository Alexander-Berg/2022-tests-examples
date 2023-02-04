package ru.yandex.realty.unification.unifier.processor.unifiers;

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.collections.CollectionFactory.list
import ru.yandex.common.util.currency.Currency
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.domain.currency.CurrencyExchangeRate
import ru.yandex.realty.graph.{MutableRegionGraph, RegionGraph}
import ru.yandex.realty.graph.core.{GeoObjectType, Name, Node}
import ru.yandex.realty.model.offer.{DealStatus, OfferType, TransactionCondition}
import ru.yandex.realty.model.raw.{RawAreaOld, RawOffer, RawOfferImpl, RawPriceOld}
import ru.yandex.realty.storage.CurrencyStorage
import ru.yandex.realty.storage.verba.TestVerbaDictionaries.AREA_TYPE
import ru.yandex.realty.storage.verba.TestVerbaDictionaries.CURRENCY
import ru.yandex.realty.storage.verba.TestVerbaDictionaries.DEAL_STATUS
import ru.yandex.realty.storage.verba.TestVerbaDictionaries.PERIOD_TYPE
import ru.yandex.realty.storage.verba.TestVerbaDictionaries.TYPE
import ru.yandex.realty.storage.verba.VerbaStorage
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper

import java.util
import scala.collection.JavaConverters._

/**
  * @author aherman
  */
@RunWith(classOf[JUnitRunner])
class TransactionUnifierTest extends AsyncSpecBase {
  val transactionUnifier = new TransactionUnifier()

  val dictionaryStorage = new VerbaStorage(util.Arrays.asList(TYPE, DEAL_STATUS, PERIOD_TYPE, AREA_TYPE, CURRENCY));

  val exchangeRates = Seq(
    new CurrencyExchangeRate(Currency.USD, Currency.RUR, 31, 1),
    new CurrencyExchangeRate(Currency.EUR, Currency.RUR, 40, 1)
  )
  val regionGraph = MutableRegionGraph.createEmptyRegionGraphWithAllFeatures();

  val regionGraphProvider = new Provider[RegionGraph] {
    override def get(): RegionGraph = regionGraph
  }
  val node = new Node();
  node.setGeoId(10000);
  var name = new Name();
  name.setDisplay("world");
  name.setAddress("world");
  node.setName(name);
  node.setGeoObjectType(GeoObjectType.UNKNOWN);

  val child = new Node();
  child.setGeoId(1);
  name = new Name();
  name.setDisplay("Region");
  name.setAddress("Region");
  child.setName(name);
  child.setGeoObjectType(GeoObjectType.CITY);

  regionGraph.addNode(node);
  regionGraph.addNode(child);
  node.addChildrenId(child.getId());
  child.addParentId(node.getId());
  regionGraph.setRoot(node);

  val currencyStorage = new CurrencyStorage(Seq.empty.asJava, exchangeRates.asJava, regionGraphProvider);

  transactionUnifier.setCurrencyProvider(ProviderAdapter.create(currencyStorage));
  transactionUnifier.setVerbaProvider(ProviderAdapter.create(dictionaryStorage));

  val areaUnifier = new AreaUnifier();
  areaUnifier.setVerbaProvider(ProviderAdapter.create(dictionaryStorage));

  val offerTypeUnifier = new OfferTypeUnifier(ProviderAdapter.create(dictionaryStorage))

  implicit val traced = Traced.empty
  "TransactionUnifier" should {
    "testUnifier" in {
      val rawOffer = createRawOffer();
      val offerWrapper = new OfferWrapper(rawOffer);
      transactionUnifier.unify(offerWrapper, traced);

      val transaction = offerWrapper.getOffer().getTransaction();
      transaction != null shouldBe true
      transaction.getTransactionConditions() != null shouldBe true
      transaction.getTransactionConditions().containsKey(TransactionCondition.MORTGAGE) shouldBe true
    }

    "testPriceInfo" in {
      val rawOffer = createRawOffer();
      val offerWrapper = new OfferWrapper(rawOffer);

      transactionUnifier.unify(offerWrapper, traced);

      val transaction = offerWrapper.getOffer().getTransaction();
      100.0f shouldEqual transaction.getPrice().getValue()
      Currency.RUR shouldEqual transaction.getPrice().getCurrency()
    }

    "testPriceInRubbles" in {
      val rawOffer = createRawOffer();
      val offerWrapper = new OfferWrapper(rawOffer);

      transactionUnifier.unify(offerWrapper, traced);

      val transaction = offerWrapper.getOffer().getTransaction();
      100.0f shouldEqual transaction.getWholeInRubles().getValue()
    }

    "testWholeOfferPrice" in {
      val rawOffer = new RawOfferImpl();
      val rawPrice = new RawPriceOld();
      rawOffer.setPrice(rawPrice);
      rawPrice.setCurrency("RUR");
      rawPrice.setUnit("сотка");
      rawPrice.setValue(10f);

      val rawArea = new RawAreaOld();
      rawOffer.setArea(rawArea);
      rawArea.setUnit("сотка");
      rawArea.setValue(2f);

      val offerWrapper = new OfferWrapper(rawOffer);
      areaUnifier.unify(offerWrapper, traced);
      transactionUnifier.unify(offerWrapper, traced);

      val transactionInfo = offerWrapper.getOffer().getTransaction();
      20f shouldEqual transactionInfo.getWholeInRubles().getValue()
    }

    "testNewRentFields" in {
      val rawOffer = new RawOfferImpl();
      val rawPrice = new RawPriceOld();
      rawOffer.setPrice(rawPrice);
      rawPrice.setValue(10f);
      rawOffer.setRentDeposit(5L);
      rawOffer.setRentPledge(true);
      rawOffer.setUtilitiesFee("INCLUDED");
      rawOffer.setUtilitiesIncluded(true);

      val offerWrapper = new OfferWrapper(rawOffer);
      transactionUnifier.unify(offerWrapper, traced);

      val offer = offerWrapper.getOffer();
      offer.getTransaction().getRentDeposit() shouldEqual rawOffer.getRentDeposit()
      offer.getTransaction().getUtilitiesFee() shouldEqual rawOffer.getUtilitiesFee()
      offer.getTransaction().getUtilitiesIncluded() shouldEqual rawOffer.getUtilitiesIncluded()
      offer.getTransaction().getTransactionConditions().containsKey(TransactionCondition.RENT_PLEDGE) shouldBe true
    }

    "testRentOfferWithSaleDeal" in {
      val rawOffer = new RawOfferImpl();
      rawOffer.setMortgage(true);
      val rawPrice = new RawPriceOld();
      rawPrice.setCurrency("RUB");
      rawPrice.setValue(100.0f);
      rawOffer.setPrice(rawPrice);
      rawOffer.setDealStatus("sale");
      rawOffer.setType("аренда");

      val offerWrapper = new OfferWrapper(rawOffer);
      offerTypeUnifier.unify(offerWrapper).futureValue
      OfferType.RENT shouldEqual offerWrapper.getOffer().getOfferType()

      transactionUnifier.unify(offerWrapper, traced);
      val transaction = offerWrapper.getOffer().getTransaction();
      transaction != null shouldBe true
      transaction.getDealStatus() != null shouldBe true
      DealStatus.DIRECT_RENT shouldEqual transaction.getDealStatus()

    }
  }

  private def createRawOffer(): RawOffer = {
    val rawOffer = new RawOfferImpl();
    rawOffer.setMortgage(true);

    val rawPrice = new RawPriceOld();
    rawPrice.setCurrency("RUB");
    rawPrice.setValue(100.0f);
    rawOffer.setPrice(rawPrice);
    rawOffer;
  }

}
