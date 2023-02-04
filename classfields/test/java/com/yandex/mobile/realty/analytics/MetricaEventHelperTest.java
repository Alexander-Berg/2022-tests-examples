package com.yandex.mobile.realty.analytics;

import com.yandex.mobile.realty.domain.model.common.*;
import com.yandex.mobile.realty.domain.model.offer.*;
import com.yandex.mobile.realty.domain.model.site.SitePreview;
import com.yandex.mobile.realty.domain.model.site.SitePreviewImpl;
import com.yandex.mobile.realty.domain.model.village.VillagePreview;
import com.yandex.mobile.realty.domain.model.village.VillagePreviewImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author rogovalex on 02.10.17.
 */
public class MetricaEventHelperTest {

    MetricaEventHelper metricaEventHelper;

    @Before
    public void init() {
        metricaEventHelper = new MetricaEventHelper();
    }

    @Test
    public void testOfferSecondaryFlatSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryFlat_Sell"));

        OfferPreview previewNotNewFlat = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.SECONDARY,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        categories = metricaEventHelper.getCategories(previewNotNewFlat);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryFlat_Sell"));

        OfferPreview previewWithSiteId = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                new SiteInfo("1", "siteName", null),
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        categories = metricaEventHelper.getCategories(previewWithSiteId);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryFlat_Sell"));
    }

    @Test
    public void testPhoneCallSite() {
        SitePreview preview = createSitePreview(false);
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("ZhkNewbuilding_Sell"));
    }

    @Test
    public void testPaidPhoneCallSite() {
        SitePreview preview = createSitePreview(true);
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("ZhkNewbuilding_Sell"));
        assertTrue(categories.contains("ZhkNewbuilding_Sell_Paid"));
    }

    @Test
    public void testPhoneCallVillageWithoutSalesDepartment() {
        VillagePreview preview = new VillagePreviewImpl(
            "123",
            "name",
            "fullName",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("Village_Sell"));
    }

    @Test
    public void testPhoneCallVillageWithoutBillingParam() {
        VillagePreview preview = new VillagePreviewImpl(
            "123",
            "name",
            "fullName",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("Village_Sell"));
    }

    @Test
    public void testPaidPhoneCallVillage() {
        VillagePreview preview = new VillagePreviewImpl(
            "123",
            "name",
            "fullName",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("Village_Sell"));
        assertTrue(categories.contains("Village_Sell_Paid"));
    }

    @Test
    public void testOfferFlatInNewbuildingSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview previewNewFlat = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_FLAT,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(previewNewFlat);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));

        OfferPreview previewNewSecondary = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_SECONDARY,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        categories = metricaEventHelper.getCategories(previewNewSecondary);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));
    }

    @Test
    public void testOfferFlatInNewbuildingPrimarySell() {
        Price price = new Price(
            100,
            Price.Unit.PER_OFFER,
            Price.Currency.RUB,
            Price.Period.WHOLE_LIFE
        );
        OfferPreview previewNewPrimary = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                true
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_FLAT,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(previewNewPrimary);
        assertEquals(3, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));
        assertTrue(categories.contains("NewFlatSale_Primary"));
    }

    @Test
    public void testPhoneCallNbOfferWithoutSalesDepartment() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_FLAT,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));

        OfferPreview previewNewSecondary = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_SECONDARY,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        categories = metricaEventHelper.getPhoneCallCategories(previewNewSecondary);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));
    }

    @Test
    public void testPhoneCallNbOfferWithoutBillingParam() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_FLAT,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));

        OfferPreview previewNewSecondary = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_SECONDARY,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        categories = metricaEventHelper.getPhoneCallCategories(previewNewSecondary);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));
    }

    @Test
    public void testPaidPhoneCallNbOffer() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_FLAT,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));
        assertTrue(categories.contains("NewFlatSale_Paid"));
    }

    @Test
    public void testPaidPhoneCallNbPrimaryPaidOffer() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                true
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                FlatType.NEW_FLAT,
                null,
                null,
                null,
                null,
                null,
                null,
                true
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(5, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("NewFlatSale"));
        assertTrue(categories.contains("NewFlatSale_Primary"));
        assertTrue(categories.contains("NewFlatSale_Paid"));
        assertTrue(categories.contains("NewFlatSale_Primary_Paid"));
    }

    @Test
    public void testPhoneCallOfferSellApartment() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryFlat_Sell"));
    }

    @Test
    public void testPhoneCallOfferSellRoom() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Room(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("Room_Sell"));
    }

    @Test
    public void testPhoneCallOfferSellHouse() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new House(
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryHouse_Sell"));
    }

    @Test
    public void testPhoneCallOfferInVillageSellHouse() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new House(
                null,
                null,
                null,
                null,
                new VillageInfo("1", "name")
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("HouseInVillage_Sell"));
    }

    @Test
    public void testPaidPhoneCallOfferInVillageSellHouse() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new House(
                null,
                null,
                null,
                null,
                new VillageInfo("1", "name")
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("HouseInVillage_Sell"));
        assertTrue(categories.contains("HouseInVillage_Sell_Paid"));
    }

    @Test
    public void testPhoneCallOfferSellLot() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Lot(
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryLot_Sell"));
    }

    @Test
    public void testPhoneCallOfferInVillageSellLot() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Lot(
                null,
                null,
                new VillageInfo("1", "name")
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("LotInVillage_Sell"));
    }

    @Test
    public void testPaidPhoneCallOfferInVillageSellLot() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Lot(
                null,
                null,
                new VillageInfo("1", "name")
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("LotInVillage_Sell"));
        assertTrue(categories.contains("LotInVillage_Sell_Paid"));
    }

    @Test
    public void testPhoneCallOfferRentApartment() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(4, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("Flat_LongRent"));
        assertTrue(categories.contains("Flat_LongRent_Realty"));
    }

    @Test
    public void testPhoneCallOfferYandexRentApartment() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                true
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(4, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("Flat_LongRent"));
        assertTrue(categories.contains("Flat_LongRent_Arenda"));
    }

    @Test
    public void testPhoneCallOfferRentRoom() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Room(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("Room_LongRent"));
    }

    @Test
    public void testPhoneCallOfferRentHouse() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new House(
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getPhoneCallCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("House_LongRent"));
    }

    @Test
    public void testOfferHouseInVillageSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new House(
                null,
                null,
                null,
                null,
                new VillageInfo("1", "name")
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("HouseInVillage_Sell"));
    }

    @Test
    public void testOfferSecondaryHouseSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new House(
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryHouse_Sell"));
    }

    @Test
    public void testOfferRoomSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Room(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("Room_Sell"));
    }

    @Test
    public void testOfferLotInVillageSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Lot(
                null,
                null,
                new VillageInfo("1", "name")
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("LotInVillage_Sell"));
    }

    @Test
    public void testOfferSecondaryLotSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Lot(
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("SecondaryLot_Sell"));
    }

    @Test
    public void testOfferCommercialSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Commercial(
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("Commercial_Sell"));
    }

    @Test
    public void testOfferGarageSell() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.WHOLE_LIFE);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Sell(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                null
            ),
            new Garage(
                null,
                GarageType.GARAGE,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Sell"));
        assertTrue(categories.contains("Garage_Sell"));
    }

    @Test
    public void testOfferFlatLongRent() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(4, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("Flat_LongRent"));
        assertTrue(categories.contains("Flat_LongRent_Realty"));
    }

    @Test
    public void testOfferYandexRent() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                true
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(4, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("Flat_LongRent"));
        assertTrue(categories.contains("Flat_LongRent_Arenda"));
    }

    @Test
    public void testOfferFlatDailyRent() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_DAY);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Apartment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("Flat_DailyRent"));
    }

    @Test
    public void testOfferHouseLongRent() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new House(
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("House_LongRent"));
    }

    @Test
    public void testOfferHouseDailyRent() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_DAY);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new House(
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("House_DailyRent"));
    }

    @Test
    public void testOfferRoomLongRent() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Room(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(3, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("LongRent"));
        assertTrue(categories.contains("Room_LongRent"));
    }

    @Test
    public void testOfferRoomDailyRent() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_DAY);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Room(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("Room_DailyRent"));
    }

    @Test
    public void testOfferCommercialRentPricePerMonth() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Commercial(
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("Commercial_Rent"));
    }

    @Test
    public void testOfferCommercialRentPricePerDay() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_DAY);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Commercial(
                null,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("Commercial_Rent"));
    }

    @Test
    public void testOfferGarageRentPricePerMonth() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_MONTH);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Garage(
                null,
                GarageType.BOX,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("Garage_Rent"));
    }

    @Test
    public void testOfferGarageRentPricePerDay() {
        Price price = new Price(100, Price.Unit.PER_OFFER, Price.Currency.RUB, Price.Period.PER_DAY);
        OfferPreview preview = new OfferPreviewImpl(
            "123",
            new Rent(
                new PriceInfo(price, null, price, Trend.UNCHANGED),
                false
            ),
            new Garage(
                null,
                GarageType.BOX,
                null
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            null,
            null
        );
        Set<String> categories = metricaEventHelper.getCategories(preview);

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Rent"));
        assertTrue(categories.contains("Garage_Rent"));
    }

    @Test
    public void testOffersCountRangeUnknown() {
        assertEquals("<unknown>", metricaEventHelper.getCountRange(null));
        assertEquals("<unknown>", metricaEventHelper.getCountRange(-1));
    }

    @Test
    public void testOffersCountRange() {
        assertEquals("0", metricaEventHelper.getCountRange(0));
        assertEquals("1-20", metricaEventHelper.getCountRange(15));
        assertEquals("21-50", metricaEventHelper.getCountRange(21));
        assertEquals("51-100", metricaEventHelper.getCountRange(100));
        assertEquals("101-200", metricaEventHelper.getCountRange(150));
        assertEquals("201-500", metricaEventHelper.getCountRange(300));
        assertEquals("501-1000", metricaEventHelper.getCountRange(800));
        assertEquals("1001-5000", metricaEventHelper.getCountRange(1002));
        assertEquals("5001-10000", metricaEventHelper.getCountRange(7500));
        assertEquals("10001-50000", metricaEventHelper.getCountRange(50000));
        assertEquals("50000+", metricaEventHelper.getCountRange(50001));
        assertEquals("50000+", metricaEventHelper.getCountRange(60001));
    }

    private SitePreviewImpl createSitePreview(boolean isPaid) {
        return new SitePreviewImpl(
            "123",
            "name",
            "fullName",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            isPaid,
            false,
            false,
            null
        );
    }
}
