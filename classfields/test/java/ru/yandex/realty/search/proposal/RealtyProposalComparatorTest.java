package ru.yandex.realty.search.proposal;

import org.junit.Test;
import ru.yandex.realty.model.sites.Mortgage;
import ru.yandex.realty.model.sites.special.proposals.Discount;
import ru.yandex.realty.model.sites.special.proposals.Gift;
import ru.yandex.realty.model.sites.special.proposals.Installment;
import ru.yandex.realty.model.sites.special.proposals.Sale;

import static org.junit.Assert.assertTrue;
import static ru.yandex.realty.search.proposal.SpecialProposalComparatorTest.createDiscount;
import static ru.yandex.realty.search.proposal.SpecialProposalComparatorTest.createInstallment;

/**
 * User: azakharov
 * Date: 25.10.16
 */
public class RealtyProposalComparatorTest {

    private final RealtyProposalComparator c = new RealtyProposalComparator();

    @Test
    public void testMortgageDiscount() {
        Mortgage goodMortgage = new Mortgage(1, 1L, "Ипотека 1");
        goodMortgage.setMinRate(7.0f);
        Mortgage badMortgage = new Mortgage(2, 1L, "Ипотека 2");
        badMortgage.setMinRate(15.0f);
        Mortgage mainMortgageWithoutMinRate = new Mortgage(3, 1L, "Ипотека 3");
        mainMortgageWithoutMinRate.setMainProposal(true);
        Mortgage mainMortgageWithGoodMinRate = new Mortgage(4, 1L, "Ипотека 3");
        mainMortgageWithGoodMinRate.setMinRate(7.0f);
        mainMortgageWithGoodMinRate.setMainProposal(true);
        Mortgage mainMortgageWithBadMinRate = new Mortgage(5, 1L, "Ипотека 3");
        mainMortgageWithBadMinRate.setMinRate(14.0f);
        mainMortgageWithBadMinRate.setMainProposal(true);

        // discounts without percent field
        Discount discountWithoutPercent = createDiscount(0, "discount", null);
        // discounts with percent field less than 1%
        Discount discountWithSmallPercent = createDiscount(2, "discount", 0.5f);
        // discounts with percent field greater than 1%
        Discount discountWithBigPercent = createDiscount(4, "discount", 10.0f);
        Discount mainDiscountWithoutPercent = createDiscount(6, "discount", null);
        mainDiscountWithoutPercent.setMainProposal(true);
        Discount mainDiscountWithSmallPercent = createDiscount(6, "discount", 0.8f);
        mainDiscountWithSmallPercent.setMainProposal(true);
        Discount mainDiscountWithBigPercent = createDiscount(6, "discount", 9.0f);
        mainDiscountWithBigPercent.setMainProposal(true);

        // main mortgage less than any not main discount
        assertTrue(c.compare(mainMortgageWithGoodMinRate, discountWithBigPercent) < 0);
        assertTrue(c.compare(mainMortgageWithBadMinRate, discountWithBigPercent) < 0);
        assertTrue(c.compare(mainMortgageWithBadMinRate, discountWithSmallPercent) < 0);
        // symmetric case
        assertTrue(c.compare(discountWithBigPercent, mainMortgageWithGoodMinRate) > 0);
        assertTrue(c.compare(discountWithBigPercent, mainMortgageWithBadMinRate) > 0);
        assertTrue(c.compare(discountWithSmallPercent, mainMortgageWithBadMinRate) > 0);

        // main discount less than any not main mortgage
        assertTrue(c.compare(mainDiscountWithSmallPercent, goodMortgage) < 0);
        assertTrue(c.compare(mainDiscountWithSmallPercent, badMortgage) < 0);
        assertTrue(c.compare(mainDiscountWithBigPercent, goodMortgage) < 0);
        assertTrue(c.compare(mainDiscountWithBigPercent, badMortgage) < 0);
        // symmetric case
        assertTrue(c.compare(goodMortgage, mainDiscountWithSmallPercent) > 0);
        assertTrue(c.compare(badMortgage, mainDiscountWithSmallPercent) > 0);
        assertTrue(c.compare(goodMortgage, mainDiscountWithBigPercent) > 0);
        assertTrue(c.compare(badMortgage, mainDiscountWithBigPercent) > 0);

        // main good discount less than any main mortgage
        assertTrue(c.compare(mainDiscountWithBigPercent, mainMortgageWithGoodMinRate) < 0);
        assertTrue(c.compare(mainDiscountWithBigPercent, mainMortgageWithBadMinRate) < 0);
        assertTrue(c.compare(mainDiscountWithBigPercent, mainMortgageWithoutMinRate) < 0);
        // symmetric case
        assertTrue(c.compare(mainMortgageWithGoodMinRate, mainDiscountWithBigPercent) > 0);
        assertTrue(c.compare(mainMortgageWithBadMinRate, mainDiscountWithBigPercent) > 0);
        assertTrue(c.compare(mainMortgageWithoutMinRate, mainDiscountWithBigPercent) > 0);

        // main good mortgage less than main bad discount
        assertTrue(c.compare(mainMortgageWithGoodMinRate, mainDiscountWithoutPercent) < 0);
        assertTrue(c.compare(mainMortgageWithGoodMinRate, mainDiscountWithSmallPercent) < 0);
        // symmetric case
        assertTrue(c.compare(mainDiscountWithoutPercent, mainMortgageWithGoodMinRate) > 0);
        assertTrue(c.compare(mainDiscountWithSmallPercent, mainMortgageWithGoodMinRate) > 0);

        // main bad discount less than main bad mortgage
        assertTrue(c.compare(mainDiscountWithoutPercent, mainMortgageWithBadMinRate) < 0);
        assertTrue(c.compare(mainDiscountWithoutPercent, mainMortgageWithoutMinRate) < 0);
        assertTrue(c.compare(mainDiscountWithSmallPercent, mainMortgageWithBadMinRate) < 0);
        assertTrue(c.compare(mainDiscountWithSmallPercent, mainMortgageWithoutMinRate) < 0);
        // symmetric case
        assertTrue(c.compare(mainMortgageWithBadMinRate, mainDiscountWithoutPercent) > 0);
        assertTrue(c.compare(mainMortgageWithoutMinRate, mainDiscountWithoutPercent) > 0);
        assertTrue(c.compare(mainMortgageWithBadMinRate, mainDiscountWithSmallPercent) > 0);
        assertTrue(c.compare(mainMortgageWithoutMinRate, mainDiscountWithSmallPercent) > 0);

        // good discount less than any mortgage
        assertTrue(c.compare(discountWithBigPercent, goodMortgage) < 0);
        assertTrue(c.compare(discountWithBigPercent, badMortgage) < 0);
        // symmetric case
        assertTrue(c.compare(goodMortgage, discountWithBigPercent) > 0);
        assertTrue(c.compare(badMortgage, discountWithBigPercent) > 0);

        // good mortgage less than bad discount
        assertTrue(c.compare(goodMortgage, discountWithSmallPercent) < 0);
        assertTrue(c.compare(goodMortgage, discountWithoutPercent) < 0);
        // symmetric case
        assertTrue(c.compare(discountWithSmallPercent, goodMortgage) > 0);
        assertTrue(c.compare(discountWithoutPercent, goodMortgage) > 0);

        // bad discount less than bad mortgage
        assertTrue(c.compare(discountWithSmallPercent, badMortgage) < 0);
        assertTrue(c.compare(discountWithoutPercent, badMortgage) < 0);
        // symmetric case
        assertTrue(c.compare(badMortgage, discountWithSmallPercent) > 0);
        assertTrue(c.compare(badMortgage, discountWithoutPercent) > 0);
    }

    @Test
    public void testMortgageInstallment() {
        Mortgage goodMortgage = new Mortgage(1, 1L, "Ипотека 1");
        goodMortgage.setMinRate(7.0f);
        Mortgage badMortgage = new Mortgage(2, 1L, "Ипотека 2");
        badMortgage.setMinRate(15.0f);

        Installment goodInstallment = createInstallment(1, "installment", true, 37, null);
        Installment badInstallment = createInstallment(3, "installment", false, 1, 50.0f);
        Installment emptyInstallment = createInstallment(5, "installment", null, null, null);

        Mortgage mainMortgageWithGoodMinRate = new Mortgage(3, 1L, "Ипотека 3");
        mainMortgageWithGoodMinRate.setMinRate(7.0f);
        mainMortgageWithGoodMinRate.setMainProposal(true);
        Mortgage mainMortgageWithBadMinRate = new Mortgage(4, 1L, "Ипотека 3");
        mainMortgageWithBadMinRate.setMinRate(14.0f);
        mainMortgageWithBadMinRate.setMainProposal(true);

        Installment mainGoodInstallment = createInstallment(1, "installment", true, 37, null);
        mainGoodInstallment.setMainProposal(true);
        Installment mainBadInstallment = createInstallment(3, "installment", false, 1, 50.0f);
        mainBadInstallment.setMainProposal(true);

        // good mortgage less than any installment
        assertTrue(c.compare(goodMortgage, goodInstallment) < 0);
        assertTrue(c.compare(goodMortgage, badInstallment) < 0);
        assertTrue(c.compare(goodMortgage, emptyInstallment) < 0);
        // symmetric case
        assertTrue(c.compare(goodInstallment, goodMortgage) > 0);
        assertTrue(c.compare(badInstallment, goodMortgage) > 0);
        assertTrue(c.compare(emptyInstallment, goodMortgage) > 0);

        // good installment less than bad mortgage
        assertTrue(c.compare(goodInstallment, badMortgage) < 0);
        assertTrue(c.compare(badMortgage, goodInstallment) > 0);
        // bad mortgage less than bad installment
        assertTrue(c.compare(badMortgage, badInstallment) < 0);
        assertTrue(c.compare(badMortgage, emptyInstallment) < 0);
        assertTrue(c.compare(badInstallment, badMortgage) > 0);
        assertTrue(c.compare(emptyInstallment, badMortgage) > 0);

        // main mortgage less than any not main installment
        assertTrue(c.compare(mainMortgageWithBadMinRate, goodInstallment) < 0);
        assertTrue(c.compare(mainMortgageWithBadMinRate, badInstallment) < 0);
        assertTrue(c.compare(mainMortgageWithGoodMinRate, goodInstallment) < 0);
        assertTrue(c.compare(mainMortgageWithGoodMinRate, emptyInstallment) < 0);
        // symmetric case
        assertTrue(c.compare(goodInstallment, mainMortgageWithBadMinRate) > 0);
        assertTrue(c.compare(badInstallment, mainMortgageWithBadMinRate) > 0);
        assertTrue(c.compare(goodInstallment, mainMortgageWithGoodMinRate) > 0);
        assertTrue(c.compare(emptyInstallment, mainMortgageWithGoodMinRate) > 0);

        // main installment less than any not main mortgage
        assertTrue(c.compare(mainGoodInstallment, goodMortgage) < 0);
        assertTrue(c.compare(mainGoodInstallment, badMortgage) < 0);
        assertTrue(c.compare(mainBadInstallment, goodMortgage) < 0);
        assertTrue(c.compare(mainBadInstallment, badMortgage) < 0);
        // symmetric case
        assertTrue(c.compare(goodMortgage, mainGoodInstallment) > 0);
        assertTrue(c.compare(badMortgage, mainGoodInstallment) > 0);
        assertTrue(c.compare(goodMortgage, mainBadInstallment) > 0);
        assertTrue(c.compare(badMortgage, mainBadInstallment) > 0);

        // good main mortgage less than any main installment
        assertTrue(c.compare(mainMortgageWithGoodMinRate, mainGoodInstallment) < 0);
        assertTrue(c.compare(mainMortgageWithGoodMinRate, mainBadInstallment) < 0);
        // symmetric case
        assertTrue(c.compare(mainGoodInstallment, mainMortgageWithGoodMinRate) > 0);
        assertTrue(c.compare(mainBadInstallment, mainMortgageWithGoodMinRate) > 0);

        // main good installment less than main bad mortgage
        assertTrue(c.compare(mainGoodInstallment, mainMortgageWithBadMinRate) < 0);
        assertTrue(c.compare(mainMortgageWithBadMinRate, mainGoodInstallment) > 0);

        // main bad mortgage less than main bad installment
        assertTrue(c.compare(mainMortgageWithBadMinRate, mainBadInstallment) < 0);
        assertTrue(c.compare(mainBadInstallment, mainMortgageWithBadMinRate) > 0);
    }

    @Test
    public void testMortgageSaleOrGift() {
        Mortgage goodMortgage = new Mortgage(1, 1L, "Ипотека 1");
        goodMortgage.setMinRate(7.0f);
        Mortgage badMortgage = new Mortgage(2, 1L, "Ипотека 2");
        badMortgage.setMinRate(15.0f);

        Sale sale = new Sale(10, "sale");
        Gift gift = new Gift(11, "gift");

        // good mortgage less than sale (gift)
        assertTrue(c.compare(goodMortgage, sale) < 0);
        assertTrue(c.compare(goodMortgage, gift) < 0);
        // symmetric case
        assertTrue(c.compare(sale, goodMortgage) > 0);
        assertTrue(c.compare(gift, goodMortgage) > 0);

        // sale (gift) less than bad mortgage
        assertTrue(c.compare(sale, badMortgage) < 0);
        assertTrue(c.compare(gift, badMortgage) < 0);
        // symmetric case
        assertTrue(c.compare(badMortgage, sale) > 0);
        assertTrue(c.compare(badMortgage, gift) > 0);
    }
}
