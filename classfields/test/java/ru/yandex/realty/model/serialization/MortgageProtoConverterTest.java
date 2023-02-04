package ru.yandex.realty.model.serialization;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import ru.yandex.realty.model.message.Mortgages;
import ru.yandex.realty.model.sites.Mortgage;
import ru.yandex.realty.model.sites.special.proposals.Discount;
import ru.yandex.realty.model.sites.special.proposals.Gift;
import ru.yandex.realty.model.sites.special.proposals.Installment;
import ru.yandex.realty.model.sites.special.proposals.Sale;

import static org.junit.Assert.assertEquals;

/**
 * User: azakharov
 * Date: 11.10.16
 */
public class MortgageProtoConverterTest {

    private static final DateTimeFormatter f = DateTimeFormat.forPattern("dd.MM.yyyy");

    @Test
    public void testMortgageToProtoToMortgage() {

        final long bankId = 1;
        final String bankName = "Сбербанк";
        final long mortgageId = 7;
        final String mortgageName = "Супер ипотека 5%";
        final boolean privileged = true;
        final boolean military = true;
        final float minRate = 5.0f;
        final float maxRate = 5.2f;
        final int minPeriod = 12;
        final int maxPeriod = 60;
        final float minFirstPay = 10.5f;
        final float maxFirstPay = 50.0f;
        final int minAmount = 2000000;
        final int maxAmount = 50000000;
        final DateTime mortgageDate = DateTime.parse("22.03.2016", f);
        final boolean mainProposal = true;

        Mortgage mortgage = new Mortgage(mortgageId, bankId, mortgageName);
        mortgage.setPrivilegedMortgage(privileged);
        mortgage.setMilitaryMortgage(military);
        mortgage.setMinRate(minRate);
        mortgage.setMaxRate(maxRate);
        mortgage.setMinDurationMonths(minPeriod);
        mortgage.setMaxDurationMonths(maxPeriod);
        mortgage.setMinFirstPayment(minFirstPay);
        mortgage.setMaxFirstPayment(maxFirstPay);
        mortgage.setMinAmount(minAmount);
        mortgage.setMaxAmount(maxAmount);
        mortgage.setProposalEndDate(mortgageDate);
        mortgage.setMainProposal(mainProposal);

        Mortgages.MortgageMessage mm = MortgageProtoConverter.toMessage(mortgage);
        Mortgage convertedMortgage = MortgageProtoConverter.fromMessage(mm);

        assertEquals("equals check", mortgage, convertedMortgage);
        assertEquals("hashCode check", mortgage.hashCode(), convertedMortgage.hashCode());
    }

    @Test
    public void testSaleToSpecialProposalMessageToSale() {

        final long id = 10;
        final String shortDescription = "Акция три по цене двух";
        final String fullDescription = "Акция три по цене двух подробная информация...";
        final DateTime startDate = DateTime.parse("15.03.2016", f);
        final DateTime endDate = DateTime.parse("22.12.2016", f);
        final boolean isMainProposal = true;

        Sale sale = new Sale(id, shortDescription);
        sale.setFullDescription(fullDescription);
        sale.setProposalStartDate(startDate);
        sale.setProposalEndDate(endDate);
        sale.setMainProposal(isMainProposal);

        Mortgages.SaleMessage message = MortgageProtoConverter.toMessage(sale);
        Sale convertedSale = MortgageProtoConverter.fromMessage(message);

        assertEquals(sale, convertedSale);
        assertEquals(sale.hashCode(), convertedSale.hashCode());
    }

    @Test
    public void testGiftToSpecialProposalMessageToGift() {

        final long id = 11;
        final String shortDescription = "Машина в подарок при покупке квартиры";
        final String fullDescription = "Подробная информация о подарке...";
        final DateTime startDate = DateTime.parse("16.03.2016", f);
        final DateTime endDate = DateTime.parse("23.12.2016", f);
        final boolean isMainProposal = true;

        Gift gift = new Gift(id, shortDescription);
        gift.setFullDescription(fullDescription);
        gift.setProposalStartDate(startDate);
        gift.setProposalEndDate(endDate);
        gift.setMainProposal(isMainProposal);

        Mortgages.GiftMessage message = MortgageProtoConverter.toMessage(gift);
        Gift convertedGift = MortgageProtoConverter.fromMessage(message);

        assertEquals(gift, convertedGift);
        assertEquals(gift.hashCode(), convertedGift.hashCode());
    }

    @Test
    public void testDiscountToSpecialProposalMessageToDiscount() {

        final long id = 11;
        final String shortDescription = "Скидка 5 %";
        final String fullDescription = "Подробная информация о скидке...";
        final DateTime startDate = DateTime.parse("17.03.2016", f);
        final DateTime endDate = DateTime.parse("24.12.2016", f);
        final Float percentOfFullPrice = 5.0f;
        final Float discountSquareMeter = 1.0f;
        final boolean isMainProposal = true;

        Discount discount = new Discount(id, shortDescription);
        discount.setFullDescription(fullDescription);
        discount.setProposalStartDate(startDate);
        discount.setProposalEndDate(endDate);
        discount.setMainProposal(isMainProposal);
        discount.setPercentOfFullPrice(percentOfFullPrice);
        discount.setDiscountForSquareMeter(discountSquareMeter);

        Mortgages.DiscountMessage message = MortgageProtoConverter.toMessage(discount);
        Discount convertedDiscount = MortgageProtoConverter.fromMessage(message);

        assertEquals(discount, convertedDiscount);
        assertEquals(discount.hashCode(), convertedDiscount.hashCode());
    }

    @Test
    public void testInstallmentToSpecialProposalMessageToInstallment() {

        final long id = 11;
        final String shortDescription = "Рассрочка 5 %";
        final String fullDescription = "Подробная информация о скидке...";
        final DateTime startDate = DateTime.parse("17.03.2016", f);
        final DateTime endDate = DateTime.parse("24.12.2016", f);
        final Boolean interestFreeInstallment = true;
        final Float initialPayment = 50000.0f;
        final Float initialPaymentPercent = 95.0f;
        final Integer durationMonths = 12;
        final Float percentOfFullPrice = 5.0f;
        final boolean isMainProposal = true;

        Installment installment = new Installment(id, shortDescription);
        installment.setFullDescription(fullDescription);
        installment.setProposalStartDate(startDate);
        installment.setProposalEndDate(endDate);
        installment.setMainProposal(isMainProposal);
        installment.setInterestFreeInstallment(interestFreeInstallment);
        installment.setInitialPayment(initialPayment);
        installment.setInitialPaymentPercent(initialPaymentPercent);
        installment.setDurationMonths(durationMonths);
        installment.setInstallmentPercentOfFullPrice(percentOfFullPrice);

        Mortgages.InstallmentMessage message = MortgageProtoConverter.toMessage(installment);
        Installment convertedInstallment = MortgageProtoConverter.fromMessage(message);

        assertEquals(installment, convertedInstallment);
        assertEquals(installment.hashCode(), convertedInstallment.hashCode());
    }
}
