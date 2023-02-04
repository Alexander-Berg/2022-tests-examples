package ru.yandex.realty.model.serialization;

import org.junit.Test;
import ru.yandex.realty.model.message.RealtySchema;
import ru.yandex.realty.model.offer.Offer;
import ru.yandex.realty.model.offer.PaymentType;
import ru.yandex.realty.model.offer.Transaction;
import ru.yandex.realty.proto.Currency;
import ru.yandex.realty.proto.unified.offer.rent.TemporaryPrice;

import static org.junit.Assert.assertEquals;

/**
 * Created by Anton Ivanov <antonio@yandex-team.ru> on 10.03.16
 */
public class OfferProtoConverterTest {
    @Test
    public void testCorrectWork() throws Exception {
        Offer first = MockOfferBuilder.createMockOffer();
        first.setPaymentType(PaymentType.NATURAL_PERSON);
        RealtySchema.OfferMessage msg = OfferProtoConverter.toMessage(first);
        Offer clone = OfferProtoConverter.fromMessage(msg);
        MockUtils.assertEquals(first, clone);
    }

    @Test
    public void test33SlonaOfferFields() throws Exception {
        Offer first = MockOfferBuilder.createMockOffer();

        first.setMortgageApprove(1);

        RealtySchema.OfferMessage msg = OfferProtoConverter.toMessage(first);
        Offer clone = OfferProtoConverter.fromMessage(msg);
        MockUtils.assertEquals(first, clone);
    }

    @Test
    public void testNewRentFields() throws Exception {
        Offer first = MockOfferBuilder.createMockOffer();

        long rentDeposit = 22L;
        String utilitiesFee = "NOT_INCLUDED";

        Transaction transaction = first.getTransaction();
        transaction.setRentDeposit(rentDeposit);
        transaction.setUtilitiesFee(utilitiesFee);
        first.setTransaction(transaction);

        RealtySchema.OfferMessage msg = OfferProtoConverter.toMessage(first);
        assertEquals(msg.getTransaction().getRentDeposit(), rentDeposit);
        assertEquals(msg.getTransaction().getUtilitiesFee(), utilitiesFee);

        Offer clone = OfferProtoConverter.fromMessage(msg);
        MockUtils.assertEquals(first, clone);
    }

    @Test
    public void testTemporaryPrice() throws Exception {
        Offer first = MockOfferBuilder.createMockOffer();
        TemporaryPrice p = TemporaryPrice.newBuilder()
                .setValue(1100000)
                .setCurrency(Currency.CURRENCY_RUB)
                .setDuration(2)
                .build();
        first.setTemporaryPrice(p);

        RealtySchema.OfferMessage msg = OfferProtoConverter.toMessage(first);
        MockUtils.assertEquals(first.getTemporaryPrice(), msg.getTemporaryPrice());

        Offer clone = OfferProtoConverter.fromMessage(msg);
        MockUtils.assertEquals(first, clone);
    }
}
