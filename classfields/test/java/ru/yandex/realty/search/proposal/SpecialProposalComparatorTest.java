package ru.yandex.realty.search.proposal;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import ru.yandex.realty.model.sites.special.proposals.Discount;
import ru.yandex.realty.model.sites.special.proposals.Gift;
import ru.yandex.realty.model.sites.special.proposals.Installment;
import ru.yandex.realty.model.sites.special.proposals.Sale;
import ru.yandex.realty.model.sites.special.proposals.SpecialProposal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: azakharov
 * Date: 21.10.16
 */
public class SpecialProposalComparatorTest {

    private final Comparator<SpecialProposal> c = new SpecialProposalComparator();

    @Test
    public void testCompareDiscountAndDiscount() {
        // discounts without percent field
        Discount discountWithoutPercent1 = createDiscount(0, "discount", null);
        Discount discountWithoutPercent2 = createDiscount(1, "discount", null);
        // discounts with percent field less than 1%
        Discount discountWithSmallPercent1 = createDiscount(2, "discount", 0.5f);
        Discount discountWithSmallPercent2 = createDiscount(3, "discount", 0.7f);
        // discounts with percent field greater than 1%
        Discount discountWithBigPercent1 = createDiscount(4, "discount", 10.0f);
        Discount discountWithBigPercent2 = createDiscount(5, "discount", 20.0f);

        // Discounts without percent are equal
        assertEquals(0, c.compare(discountWithoutPercent1, discountWithoutPercent2));
        assertEquals(0, c.compare(discountWithoutPercent2, discountWithoutPercent1));

        // Discount without percent is always greater than discount with percent
        assertTrue(c.compare(discountWithoutPercent1, discountWithSmallPercent1) > 0);
        assertTrue(c.compare(discountWithoutPercent1, discountWithBigPercent1) > 0);

        // Discount with percent is lower than discount without percent
        assertTrue(c.compare(discountWithSmallPercent1, discountWithoutPercent1) < 0);
        assertTrue(c.compare(discountWithBigPercent1, discountWithoutPercent1) < 0);

        // Discount with percents are ordered by descending of percent
        assertTrue(c.compare(discountWithSmallPercent1, discountWithSmallPercent2) > 0);
        assertTrue(c.compare(discountWithSmallPercent1, discountWithBigPercent1) > 0);
        assertTrue(c.compare(discountWithBigPercent1, discountWithSmallPercent1) < 0);
        assertTrue(c.compare(discountWithBigPercent1, discountWithBigPercent2) > 0);
        assertTrue(c.compare(discountWithBigPercent2, discountWithBigPercent1) < 0);
    }

    @Test
    public void testCompareDiscountAndInstallment() {
        Discount discountWithoutPercent = createDiscount(0, "discount", null);
        Discount discountWithSmallPercent = createDiscount(2, "discount", 0.5f);
        Discount discountWithBigPercent = createDiscount(4, "discount", 10.0f);
        Installment goodInstallment = createInstallment(10, "installment", true, 37, null);
        Installment badInstallment = createInstallment(11, "installment", false, 1, null);
        Installment emptyInstallment = createInstallment(12, "installment", null, null, null);

        // Discount with big percent less than any installment
        assertTrue(c.compare(discountWithBigPercent, goodInstallment) < 0);
        assertTrue(c.compare(discountWithBigPercent, badInstallment) < 0);
        assertTrue(c.compare(discountWithBigPercent, emptyInstallment) < 0);
        // Symmetric case: any installment greater than discount with big percent
        assertTrue(c.compare(goodInstallment, discountWithBigPercent) > 0);
        assertTrue(c.compare(badInstallment, discountWithBigPercent) > 0);
        assertTrue(c.compare(emptyInstallment, discountWithBigPercent) > 0);

        // Discount with small percent and discount without percent greater than good installment
        assertTrue(c.compare(discountWithSmallPercent, goodInstallment) > 0);
        assertTrue(c.compare(discountWithoutPercent, goodInstallment) > 0);
        // Symmetric case: good installment less than discount with small percent and empty discount
        assertTrue(c.compare(goodInstallment, discountWithSmallPercent) < 0);
        assertTrue(c.compare(goodInstallment, discountWithoutPercent) < 0);

        // Discount with small (or empty) percent less than bad (or empty) installment installment
        assertTrue(c.compare(discountWithSmallPercent, badInstallment) < 0);
        assertTrue(c.compare(discountWithoutPercent, badInstallment) < 0);
        assertTrue(c.compare(discountWithSmallPercent, emptyInstallment) < 0);
        assertTrue(c.compare(discountWithoutPercent, emptyInstallment) < 0);
        // Symmetric case: bad (or empty) installment greater discount with small (or empty) percent
        assertTrue(c.compare(badInstallment, discountWithSmallPercent) > 0);
        assertTrue(c.compare(badInstallment, discountWithoutPercent) > 0);
        assertTrue(c.compare(emptyInstallment, discountWithSmallPercent) > 0);
        assertTrue(c.compare(emptyInstallment, discountWithoutPercent) > 0);
    }

    @Test
    public void testCompareDiscountAndSaleOrGift() {
        Discount discountWithBigPercent = createDiscount(4, "discount", 10.0f);
        Discount discountWithSmallPercent = createDiscount(2, "discount", 0.5f);
        Sale sale = new Sale(10, "sale");
        Gift gift = new Gift(11, "gift");

        // Any discount less than any sale (any gift)
        assertTrue(c.compare(discountWithBigPercent, sale) < 0);
        assertTrue(c.compare(discountWithBigPercent, gift) < 0);
        assertTrue(c.compare(discountWithSmallPercent, sale) < 0);
        assertTrue(c.compare(discountWithSmallPercent, gift) < 0);
        // Symmetric case
        assertTrue(c.compare(sale, discountWithBigPercent) > 0);
        assertTrue(c.compare(gift, discountWithBigPercent) > 0);
        assertTrue(c.compare(sale, discountWithSmallPercent) > 0);
        assertTrue(c.compare(gift, discountWithSmallPercent) > 0);
    }

    @Test
    public void testCompareInstallments() {
        Installment goodInstallment1 = createInstallment(1, "installment", true, 37, null);
        Installment goodInstallment2 = createInstallment(2, "installment", true, 40, null);
        Installment badInstallment1 = createInstallment(3, "installment", false, 1, 50.0f);
        Installment badInstallment2 = createInstallment(4, "installment", false, 1, 70.0f);
        Installment emptyInstallment1 = createInstallment(5, "installment", null, null, null);
        Installment emptyInstallment2 = createInstallment(6, "installment", null, null, null);

        // Good installments are ordered by duration descending
        assertTrue(c.compare(goodInstallment1, goodInstallment2) > 0);
        assertTrue(c.compare(goodInstallment2, goodInstallment1) < 0);

        // Good installment less than others
        assertTrue(c.compare(goodInstallment1, badInstallment1) < 0);
        assertTrue(c.compare(goodInstallment1, emptyInstallment1) < 0);
        // Symmetric case
        assertTrue(c.compare(badInstallment1, goodInstallment1) > 0);
        assertTrue(c.compare(emptyInstallment1, goodInstallment1) > 0);

        // Bad installments are ordered initialPayment
        assertTrue(c.compare(badInstallment1, badInstallment2) < 0);
        assertTrue(c.compare(badInstallment2, badInstallment1) > 0);

        // Bad installments less than empty installments
        assertTrue(c.compare(badInstallment1, emptyInstallment1) < 0);
        assertTrue(c.compare(emptyInstallment1, badInstallment1) > 0);

        // Empty installments are equal
        assertEquals(0, c.compare(emptyInstallment1, emptyInstallment2));
    }

    @Test
    public void testCompareInstallmentAndSaleOrGift() {
        Installment goodInstallment = createInstallment(1, "installment", true, 37, null);
        Installment badInstallment = createInstallment(3, "installment", false, 1, 50.0f);
        Installment emptyInstallment = createInstallment(5, "installment", null, null, null);
        Sale sale = new Sale(10, "sale");
        Gift gift = new Gift(11, "gift");

        // Good installment less than sale or gift
        assertTrue(c.compare(goodInstallment, sale) < 0);
        assertTrue(c.compare(goodInstallment, gift) < 0);
        // Symmetric case
        assertTrue(c.compare(sale, goodInstallment) > 0);
        assertTrue(c.compare(gift, goodInstallment) > 0);

        // Bad (or empty) installment greater than sale or gift
        assertTrue(c.compare(badInstallment, sale) > 0);
        assertTrue(c.compare(emptyInstallment, sale) > 0);
        assertTrue(c.compare(badInstallment, gift) > 0);
        assertTrue(c.compare(emptyInstallment, gift) > 0);
        // Symmetric case
        assertTrue(c.compare(sale, badInstallment) < 0);
        assertTrue(c.compare(sale, emptyInstallment) < 0);
        assertTrue(c.compare(gift, badInstallment) < 0);
        assertTrue(c.compare(gift, emptyInstallment) < 0);
    }

    @Test
    public void testSalesAndGifts() {
        Sale saleA = new Sale(10, "a");
        Sale saleB = new Sale(10, "b");
        Gift giftA = new Gift(11, "a");
        Gift giftB = new Gift(11, "b");

        // Sales are ordered by shortDescription ascending
        assertEquals(-1, c.compare(saleA, saleB));
        assertEquals(1, c.compare(saleB, saleA));

        // Gifts are ordered by shortDescription ascending
        assertEquals(-1, c.compare(giftA, giftB));
        assertEquals(1, c.compare(giftB, giftA));

        // Sales less than gifts
        assertEquals(-1, c.compare(saleA, giftA));
        assertEquals(1, c.compare(giftA, saleA));
    }

    @Test
    public void testMainProposals() {
        Discount discount = createDiscount(1, "discount", 23.0f);
        Installment installment = createInstallment(2, "installment", true, 37, 10.0f);
        Sale sale = new Sale(3, "sale");
        Gift gift = new Gift(4, "gift");

        Discount mainDiscount = createDiscount(2, "main discount", null);
        Installment mainInstallment = createInstallment(3, "main installment", true, 37, 10.0f);
        Sale mainSale = new Sale(3, "main sale");
        Gift mainGift = new Gift(4, "main gift");

        List<SpecialProposal> specialProposals = new ArrayList<>(4);
        specialProposals.add(discount);
        specialProposals.add(installment);
        specialProposals.add(sale);
        specialProposals.add(gift);

        List<SpecialProposal> mainProposals = new ArrayList<>(4);
        mainProposals.add(mainDiscount);
        mainProposals.add(mainInstallment);
        mainProposals.add(mainSale);
        mainProposals.add(mainGift);
        for (SpecialProposal p : mainProposals) {
            p.setMainProposal(true);
        }

        for (SpecialProposal m : mainProposals) {
            for (SpecialProposal p : specialProposals) {
                assertEquals(-1, c.compare(m, p));
                assertEquals(1, c.compare(p, m));
            }
        }
    }

    @Test
    public void testSortAllTheTypes() {
        Discount goodDiscount = createDiscount(1, "discount", 23.0f);
        Discount badDiscount = createDiscount(1, "discount", 0.1f);
        Installment goodInstallment = createInstallment(2, "installment", true, 37, 10.0f);
        Installment badInstallment = createInstallment(2, "installment", false, 1, 10.0f);
        Sale sale = new Sale(3, "sale");
        Gift gift = new Gift(4, "gift");

        List<SpecialProposal> specialProposals = new ArrayList<>(4);
        specialProposals.add(badInstallment);
        specialProposals.add(badDiscount);
        specialProposals.add(sale);
        specialProposals.add(goodDiscount);
        specialProposals.add(gift);
        specialProposals.add(goodInstallment);

        Collections.sort(specialProposals, c);
        assertEquals(goodDiscount, specialProposals.get(0));
        assertEquals(goodInstallment, specialProposals.get(1));
        assertEquals(badDiscount, specialProposals.get(2));
        assertEquals(sale, specialProposals.get(3));
        assertEquals(gift, specialProposals.get(4));
        assertEquals(badInstallment, specialProposals.get(5));
    }

    static Installment createInstallment(int id, String description, @Nullable Boolean free,
            @Nullable Integer duration, @Nullable Float initialPayment) {
        Installment i = new Installment(id, description);
        i.setInterestFreeInstallment(free);
        i.setInitialPayment(initialPayment);
        i.setDurationMonths(duration);
        return i;
    }

    static Discount createDiscount(int id, String description, @Nullable Float percent) {
        Discount d = new Discount(id, description);
        if (percent != null) {
            d.setPercentOfFullPrice(percent);
        }
        return d;
    }
}
