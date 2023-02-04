package ru.yandex.realty.sites.builder;

import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import ru.yandex.realty.model.message.Mortgages;
import ru.yandex.realty.model.sites.special.proposals.Discount;
import ru.yandex.realty.model.sites.special.proposals.Gift;
import ru.yandex.realty.model.sites.special.proposals.Installment;
import ru.yandex.realty.model.sites.special.proposals.MortgageSpecialProposal;
import ru.yandex.realty.model.sites.special.proposals.Sale;
import ru.yandex.realty.sites.SpecialProposalType;
import ru.yandex.realty.storage.verba.VerbaStorage;
import ru.yandex.verba2.model.Dictionary;
import ru.yandex.verba2.model.Term;
import ru.yandex.verba2.model.attribute.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * User: azakharov
 * Date: 11.10.16
 */
public class SpecialProposalsBuilderTest {

    private final SpecialProposalsBuilder b = new SpecialProposalsBuilder();
    private final Random random = new Random();
    private static final DateTimeFormatter f = DateTimeFormat.forPattern("dd.MM.yyyy");

    @Test
    public void testSale() {
        final long id = 10;
        final String shortDescription = "Акция три по цене двух";
        final String fullDescription = "Акция три по цене двух подробная информация...";
        final DateTime startDate = DateTime.parse(DateTime.now().minusDays(1).toString(f), f);
        final DateTime endDate = DateTime.parse(DateTime.now().plusDays(10).toString(f), f);
        final boolean isMainProposal = true;

        final long siteId1 = 101;
        final long siteId2 = 202;

        List<Attribute> links = new ArrayList<>(1);
        List<LinkContainer> containers = new ArrayList<>(2);
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId1), "ЖК 1", 1));
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId2), "ЖК 2", 1));
        LinkAttribute sites = new LinkAttribute("building-link", containers);
        links.add(sites);

        List<Term> terms = Collections.singletonList(
                createSpecialProposal(SpecialProposalType.SALE,
                        id, shortDescription, fullDescription, startDate, endDate, isMainProposal, links));
        Dictionary specialOffers = new Dictionary(2L, 2L, "special_offer", "name", "/path", terms);
        VerbaStorage service = new VerbaStorage(Collections.singletonList(specialOffers));

        SpecialProposalsBuilderResult result = b.buildSiteIdToSpecialProposalsMapping(service,
                Collections.<Long, List<Long>>emptyMap());
        List<Sale> sales1 = result.getSales(siteId1);
        List<Sale> sales2 = result.getSales(siteId2);
        assertNotNull(sales1);
        assertNotNull(sales2);

        Sale sale1 = sales1.get(0);
        assertEquals(id, sale1.getId());
        assertEquals(shortDescription, sale1.getShortDescription());
        assertEquals(fullDescription, sale1.getFullDescription());
        assertEquals(startDate, sale1.getProposalStartDate());
        assertEquals(endDate, sale1.getProposalEndDate());
        assertEquals(isMainProposal, sale1.getMainProposal());

        Sale expectedSale = new Sale(id, shortDescription);
        assertFalse(expectedSale.equals(sale1));
        expectedSale.setFullDescription(fullDescription);
        expectedSale.setProposalStartDate(startDate);
        expectedSale.setProposalEndDate(endDate);
        expectedSale.setMainProposal(isMainProposal);

        assertEquals(expectedSale, sale1);

        Sale sale2 = sales2.get(0);
        assertEquals(expectedSale, sale2);
    }

    @Test
    public void testGift() {
        final long id = 11;
        final String shortDescription = "Машина в подарок при покупке квартиры";
        final String fullDescription = "Подробная информация о подарке...";
        final DateTime startDate = DateTime.parse(DateTime.now().minusDays(1).toString(f), f);
        final DateTime endDate = DateTime.parse(DateTime.now().plusDays(10).toString(f), f);
        final boolean isMainProposal = true;

        final long siteId1 = 111;
        final long siteId2 = 212;

        List<Attribute> links = new ArrayList<>(1);
        List<LinkContainer> containers = new ArrayList<>(2);
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId1), "ЖК 1", 1));
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId2), "ЖК 2", 1));
        LinkAttribute sites = new LinkAttribute("building-link", containers);
        links.add(sites);

        List<Term> terms = Collections.singletonList(
                createSpecialProposal(SpecialProposalType.GIFT,
                        id, shortDescription, fullDescription, startDate, endDate, isMainProposal, links));
        Dictionary specialOffers = new Dictionary(2L, 2L, "special_offer", "name", "/path", terms);
        VerbaStorage service = new VerbaStorage(Collections.singletonList(specialOffers));

        SpecialProposalsBuilderResult result = b.buildSiteIdToSpecialProposalsMapping(service,
                Collections.<Long, List<Long>>emptyMap());
        List<Gift> gifts1 = result.getGifts(siteId1);
        List<Gift> gifts2 = result.getGifts(siteId2);
        assertNotNull(gifts1);
        assertNotNull(gifts2);

        Gift gift1 = gifts1.get(0);
        assertEquals(id, gift1.getId());
        assertEquals(shortDescription, gift1.getShortDescription());
        assertEquals(fullDescription, gift1.getFullDescription());
        assertEquals(startDate, gift1.getProposalStartDate());
        assertEquals(endDate, gift1.getProposalEndDate());
        assertEquals(isMainProposal, gift1.getMainProposal());

        Gift expectedGift = new Gift(id, shortDescription);
        assertFalse(expectedGift.equals(gift1));
        expectedGift.setFullDescription(fullDescription);
        expectedGift.setProposalStartDate(startDate);
        expectedGift.setProposalEndDate(endDate);
        expectedGift.setMainProposal(isMainProposal);

        assertEquals(expectedGift, gift1);

        Gift gift2 = gifts2.get(0);
        assertEquals(expectedGift, gift2);
    }

    @Test
    public void testDiscount() {
        final long id = 11;
        final String shortDescription = "Скидка 5 %";
        final String fullDescription = "Подробная информация о скидке...";
        final DateTime startDate = DateTime.parse(DateTime.now().minusDays(1).toString(f), f);
        final DateTime endDate = DateTime.parse(DateTime.now().plusDays(10).toString(f), f);
        final Float percentOfFullPrice = 5.0f;
        final Float discountSquareMeter = 1.0f;
        final boolean isMainProposal = true;

        final long siteId1 = 121;
        final long siteId2 = 222;

        List<Attribute> links = new ArrayList<>(1);
        List<LinkContainer> containers = new ArrayList<>(2);
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId1), "ЖК 1", 1));
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId2), "ЖК 2", 1));
        LinkAttribute sites = new LinkAttribute("building-link", containers);
        links.add(sites);

        List<Term> terms = Collections.singletonList(
                createDiscount(id, shortDescription, fullDescription, startDate, endDate, isMainProposal,
                        percentOfFullPrice, discountSquareMeter, links));
        Dictionary specialOffers = new Dictionary(2L, 2L, "special_offer", "name", "/path", terms);
        VerbaStorage service = new VerbaStorage(Collections.singletonList(specialOffers));

        SpecialProposalsBuilderResult result = b.buildSiteIdToSpecialProposalsMapping(service,
                Collections.<Long, List<Long>>emptyMap());
        List<Discount> discounts1 = result.getDiscounts(siteId1);
        List<Discount> discounts2 = result.getDiscounts(siteId2);
        assertNotNull(discounts1);
        assertNotNull(discounts2);

        Discount discount1 = discounts1.get(0);
        assertEquals(id, discount1.getId());
        assertEquals(shortDescription, discount1.getShortDescription());
        assertEquals(fullDescription, discount1.getFullDescription());
        assertEquals(startDate, discount1.getProposalStartDate());
        assertEquals(endDate, discount1.getProposalEndDate());
        assertEquals(isMainProposal, discount1.getMainProposal());
        assertEquals(percentOfFullPrice, discount1.getPercentOfFullPrice());
        assertEquals(discountSquareMeter, discount1.getDiscountForSquareMeter());

        Discount expectedDiscount = new Discount(id, shortDescription);
        assertFalse(expectedDiscount.equals(discount1));
        expectedDiscount.setFullDescription(fullDescription);
        expectedDiscount.setProposalStartDate(startDate);
        expectedDiscount.setProposalEndDate(endDate);
        expectedDiscount.setMainProposal(isMainProposal);
        expectedDiscount.setPercentOfFullPrice(percentOfFullPrice);
        expectedDiscount.setDiscountForSquareMeter(discountSquareMeter);

        assertEquals(expectedDiscount, discount1);
        assertEquals(expectedDiscount.hashCode(), discount1.hashCode());

        Discount discount2 = discounts2.get(0);
        assertEquals(expectedDiscount, discount2);
    }

    @Test
    public void testInstallment() {
        final long id = 11;
        final String shortDescription = "Рассрочка 5 %";
        final String fullDescription = "Подробная информация о скидке...";
        final DateTime startDate = DateTime.parse(DateTime.now().minusDays(1).toString(f), f);
        final DateTime endDate = DateTime.parse(DateTime.now().plusDays(10).toString(f), f);
        final Boolean interestFreeInstallment = false;
        final Float initialPayment = 50000.0f;
        final Float initialPaymentPercent = 95.0f;
        final Integer durationMonths = 12;
        final Float percentOfFullPrice = 5.0f;
        final boolean isMainProposal = true;

        final long siteId1 = 121;
        final long siteId2 = 222;

        List<Attribute> links = new ArrayList<>(1);
        List<LinkContainer> containers = new ArrayList<>(2);
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId1), "ЖК 1", 1));
        containers.add(new LinkContainer(random.nextInt(), Long.toString(siteId2), "ЖК 2", 1));
        LinkAttribute sites = new LinkAttribute("building-link", containers);
        links.add(sites);

        List<Term> terms = Collections.singletonList(
                createInstallment(id, shortDescription, fullDescription, startDate, endDate, isMainProposal,
                        interestFreeInstallment, initialPayment, initialPaymentPercent, durationMonths,
                        percentOfFullPrice, links));
        Dictionary specialOffers = new Dictionary(2L, 2L, "special_offer", "name", "/path", terms);
        VerbaStorage service = new VerbaStorage(Collections.singletonList(specialOffers));

        SpecialProposalsBuilderResult result = b.buildSiteIdToSpecialProposalsMapping(service,
                Collections.<Long, List<Long>>emptyMap());
        List<Installment> installments1 = result.getInstallments(siteId1);
        List<Installment> installments2 = result.getInstallments(siteId2);
        assertNotNull(installments1);
        assertNotNull(installments2);

        Installment installment1 = installments1.get(0);
        assertEquals(id, installment1.getId());
        assertEquals(shortDescription, installment1.getShortDescription());
        assertEquals(fullDescription, installment1.getFullDescription());
        assertEquals(startDate, installment1.getProposalStartDate());
        assertEquals(endDate, installment1.getProposalEndDate());
        assertEquals(isMainProposal, installment1.getMainProposal());
        assertEquals(interestFreeInstallment, installment1.getInterestFreeInstallment());
        assertEquals(initialPayment, installment1.getInitialPayment());
        assertEquals(initialPaymentPercent, installment1.getInitialPaymentPercent());
        assertEquals(durationMonths, installment1.getDurationMonths());
        assertEquals(percentOfFullPrice, installment1.getInstallmentPercentOfFullPrice());

        Installment expectedInstallment = new Installment(id, shortDescription);
        assertFalse(expectedInstallment.equals(installment1));
        expectedInstallment.setFullDescription(fullDescription);
        expectedInstallment.setProposalStartDate(startDate);
        expectedInstallment.setProposalEndDate(endDate);
        expectedInstallment.setMainProposal(isMainProposal);
        expectedInstallment.setInterestFreeInstallment(interestFreeInstallment);
        expectedInstallment.setInitialPayment(initialPayment);
        expectedInstallment.setInitialPaymentPercent(initialPaymentPercent);
        expectedInstallment.setDurationMonths(durationMonths);
        expectedInstallment.setInstallmentPercentOfFullPrice(percentOfFullPrice);

        assertEquals(expectedInstallment, installment1);
        assertEquals(expectedInstallment.hashCode(), installment1.hashCode());

        Installment installment2 = installments2.get(0);
        assertEquals(expectedInstallment, installment2);
    }

    @Test
    public void testMortgage() {
        final long id = 33;
        final String shortDescription = "Ипотека 2,99 % на весь срок";
        final String fullDescription = "Ипотека 2,99 % на весь срок подробная информация...";
        final DateTime startDate = DateTime.parse(DateTime.now().minusDays(2).toString(f), f);
        final DateTime endDate = DateTime.parse(DateTime.now().plusDays(22).toString(f), f);
        final boolean isMainProposal = true;

        final long siteId1 = 101;
        final long siteId2 = 202;
        final long bankId = 303;

        List<Attribute> other = new ArrayList<>(1);
        List<LinkContainer> siteContainers = new ArrayList<>(2);
        siteContainers.add(new LinkContainer(random.nextInt(), Long.toString(siteId1), "ЖК 1", 1));
        siteContainers.add(new LinkContainer(random.nextInt(), Long.toString(siteId2), "ЖК 2", 1));
        LinkAttribute sites = new LinkAttribute("building-link", siteContainers);
        other.add(sites);

        List<LinkContainer> bankContainers = new ArrayList<>(1);
        bankContainers.add(new LinkContainer(random.nextInt(), Long.toString(bankId), "Банк 0", 1));
        LinkAttribute banks = new LinkAttribute("banks", bankContainers);
        other.add(banks);

        String mortgageType = "military-mortgage";
        List<LinkContainer> mortgageTypeContainers = new ArrayList<>(1);
        mortgageTypeContainers.add(new LinkContainer(random.nextInt(), mortgageType, mortgageType, 1));
        LinkAttribute type = new LinkAttribute("mortgage-type", mortgageTypeContainers);
        other.add(type);

        List<Term> terms = Collections.singletonList(
                createSpecialProposal(SpecialProposalType.MORTGAGE,
                        id, shortDescription, fullDescription, startDate, endDate, isMainProposal, other));
        Dictionary specialOffers = new Dictionary(25L, 2L, "special_offer", "name", "/path", terms);
        VerbaStorage service = new VerbaStorage(Collections.singletonList(specialOffers));

        SpecialProposalsBuilderResult result = b.buildSiteIdToSpecialProposalsMapping(service,
                Collections.<Long, List<Long>>emptyMap());
        List<MortgageSpecialProposal> mortgages1 = result.getMortgages(siteId1);
        List<MortgageSpecialProposal> mortgages2 = result.getMortgages(siteId2);
        assertNotNull(mortgages1);
        assertNotNull(mortgages2);

        MortgageSpecialProposal mortgage1 = mortgages1.get(0);
        assertEquals(id, mortgage1.getId());
        assertEquals(shortDescription, mortgage1.getShortDescription());
        assertEquals(fullDescription, mortgage1.getFullDescription());
        assertEquals(startDate, mortgage1.getProposalStartDate());
        assertEquals(endDate, mortgage1.getProposalEndDate());
        assertEquals(isMainProposal, mortgage1.getMainProposal());
        assertEquals(List.of(bankId), mortgage1.getBankIds());
        assertEquals(Mortgages.MortgageType.M_MILITARY, mortgage1.getMortgageType());

        MortgageSpecialProposal expectedMortgage = new MortgageSpecialProposal(id, shortDescription);
        assertNotEquals(expectedMortgage, mortgage1);
        expectedMortgage.setFullDescription(fullDescription);
        expectedMortgage.setProposalStartDate(startDate);
        expectedMortgage.setProposalEndDate(endDate);
        expectedMortgage.setMainProposal(isMainProposal);
        expectedMortgage.setBankIds(List.of(bankId));
        expectedMortgage.setMortgageType(Mortgages.MortgageType.M_MILITARY);

        assertEquals(expectedMortgage, mortgage1);

        MortgageSpecialProposal mortgage2 = mortgages2.get(0);
        assertEquals(expectedMortgage, mortgage2);
    }

    private Term createSpecialProposal(SpecialProposalType type, long id, String shortDescription, String fullDescription,
                                       DateTime startDate, DateTime endDate, Boolean isMainProposal,
                                       @Nullable List<Attribute> extraAttributes) {
        List<Attribute> attributes = new ArrayList<>(6);
        if (type == SpecialProposalType.MORTGAGE) {
            attributes.add(new LinkAttribute("special-offer-type",
                    Collections.singletonList(new LinkContainer(random.nextInt(), SpecialProposalType.SALE.name(), "<name>", 1))));
            attributes.add(new LinkAttribute("special-type",
                    Collections.singletonList(new LinkContainer(random.nextInt(), type.name(), "<name>", 1))));
        } else {
            attributes.add(new LinkAttribute("special-offer-type",
                    Collections.singletonList(new LinkContainer(random.nextInt(), type.name(), "<name>", 1))));
        }
        attributes.add(new StringAttribute("id", Collections.singletonList(Long.toString(id))));
        attributes.add(new StringAttribute("short-description", Collections.singletonList(shortDescription)));
        attributes.add(new StringAttribute("long-description", Collections.singletonList(fullDescription)));
        attributes.add(new StringAttribute("start", Collections.singletonList(startDate.toString(f))));
        attributes.add(new StringAttribute("stop", Collections.singletonList(endDate.toString(f))));
        attributes.add(new BooleanAttribute("main-suggestion", isMainProposal));
        if (extraAttributes != null) {
            attributes.addAll(extraAttributes);
        }
        Term mortgageTerm = new Term(random.nextInt(), Long.toString(id), shortDescription, 1,
                "/realty/special_offer/" + id, DateTime.now(), DateTime.now());
        return new Term(mortgageTerm, attributes, Collections.<Dictionary>emptyList());
    }

    private Term createDiscount(long id, String shortDescription, String fullDescription,
                                DateTime startDate, DateTime endDate, Boolean isMainProposal,
                                Float percentOfFullPrice, Float discountForSquareMeter,
                                @Nullable List<Attribute> extraAttributes) {
        int extraSize = extraAttributes != null ? extraAttributes.size() : 0;
        List<Attribute> attributes = new ArrayList<>(2 + extraSize);
        if (extraAttributes != null) {
            attributes.addAll(extraAttributes);
        }
        if (percentOfFullPrice != null) {
            attributes.add(new StringAttribute("discount-amount", Collections.singletonList(Float.toString(percentOfFullPrice))));
        }
        if (discountForSquareMeter != null) {
            attributes.add(new StringAttribute("discount-square-meter", Collections.singletonList(Float.toString(discountForSquareMeter))));
        }
        return createSpecialProposal(SpecialProposalType.DISCOUNT, id, shortDescription,
                fullDescription, startDate, endDate, isMainProposal, attributes);
    }

    private Term createInstallment(long id, String shortDescription, String fullDescription,
                                   DateTime startDate, DateTime endDate, Boolean isMainProposal,
                                   Boolean interestFreeInstallment, Float initialPayment, Float initialPaymentPercent,
                                   Integer durationMonths, Float installmentPercentOfFullPrice,
                                   @Nullable List<Attribute> extraAttributes) {
        int extraSize = extraAttributes != null ? extraAttributes.size() : 0;
        List<Attribute> attributes = new ArrayList<>(5 + extraSize);
        if (extraAttributes != null) {
            attributes.addAll(extraAttributes);
        }
        if (interestFreeInstallment != null) {
            attributes.add(new BooleanAttribute("free-installments", interestFreeInstallment));
        }
        if (initialPayment != null) {
            attributes.add(new StringAttribute("initial-payment", Collections.singletonList(Float.toString(initialPayment))));
        }
        if (initialPaymentPercent != null) {
            attributes.add(new StringAttribute("initial-payment-percent",
                    Collections.singletonList(Float.toString(initialPaymentPercent))));
        }
        if (durationMonths != null) {
            attributes.add(new StringAttribute("installment-period",
                    Collections.singletonList(Integer.toString(durationMonths))));
        }
        if (installmentPercentOfFullPrice != null) {
            attributes.add(new StringAttribute("discount-amount",
                    Collections.singletonList(Float.toString(installmentPercentOfFullPrice))));
        }
        return createSpecialProposal(SpecialProposalType.INSTALLMENTS, id, shortDescription,
                fullDescription, startDate, endDate, isMainProposal, attributes);

    }
}
