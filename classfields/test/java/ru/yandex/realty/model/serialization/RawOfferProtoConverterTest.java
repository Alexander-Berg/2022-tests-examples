package ru.yandex.realty.model.serialization;

import org.junit.Test;
import ru.yandex.realty.model.message.RealtySchema;
import ru.yandex.realty.model.offer.PaymentType;
import ru.yandex.realty.model.raw.RawImageImpl;
import ru.yandex.realty.model.raw.RawOfferImpl;
import ru.yandex.realty.model.raw.RawTemporaryPriceImpl;
import ru.yandex.realty.picapica.MdsUrlBuilder;
import ru.yandex.realty.proto.unified.offer.images.UnifiedImages;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Anton Ivanov <antonio@yandex-team.ru> on 03.03.16
 */
public class RawOfferProtoConverterTest {
    private final MdsUrlBuilder mdsUrlBuilder = new MdsUrlBuilder("//a.z");

    @Test
    public void testCorrectWork() throws Exception {
        RawOfferImpl first = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(true);
        first.setId("1");
        first.setInternal(true);
        first.setPaymentType(PaymentType.JURIDICAL_PERSON);
        first.setPhotoPreviews(new ArrayList<>());
        RealtySchema.RawOfferMessage msg = RawOfferProtoConverter.toMessage(first);
        RawOfferImpl clone = (RawOfferImpl)RawOfferProtoConverter.fromMessage(msg, mdsUrlBuilder);
        MockUtils.assertEquals(first, clone);
    }

    @Test
    public void testCorrectForExperimentalFields() throws Exception {
        RawOfferImpl first = MockRawOfferBuilder.createRawOfferWithExperimentalFields();
        first.setId("1");
        first.setInternal(true);
        first.setPhotoPreviews(new ArrayList<>());
        RealtySchema.RawOfferMessage msg = RawOfferProtoConverter.toMessage(first);
        RawOfferImpl second = (RawOfferImpl)RawOfferProtoConverter.fromMessage(msg, mdsUrlBuilder);
        MockUtils.assertEquals(first, second);
    }

    @Test
    public void removesRawImagesIfModernArePresent() {
        RawOfferImpl offer = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(true);
        offer.setId("1");
        offer.setInternal(true);
        offer.setPhotoPreviews(new ArrayList<>());

        UnifiedImages.Builder modernImagesBuilder = UnifiedImages.newBuilder();
        assertFalse(offer.getAllImages().isEmpty());
        for (RawImageImpl rawImage : offer.getAllImages()) {
            modernImagesBuilder.addImageBuilder()
                    .setExternalUrl(rawImage.getImage());
        }
        offer.setModernImages(modernImagesBuilder.build());

        RealtySchema.RawOfferMessage message = RawOfferProtoConverter.toMessage(offer);
        assertTrue(message.hasModernImages());
        assertEquals(0, message.getRawImageCount());
    }

    @Test
    public void testNewRentFields() throws Exception {
        RawOfferImpl first = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(true);
        first.setId("1");
        first.setPhotoPreviews(new ArrayList<>());

        first.setRentDeposit(11L);
        first.setUtilitiesFee("METER");

        RealtySchema.RawOfferMessage msg = RawOfferProtoConverter.toMessage(first);
        RawOfferImpl clone = (RawOfferImpl)RawOfferProtoConverter.fromMessage(msg, mdsUrlBuilder);
        MockUtils.assertEquals(first, clone);
    }

    @Test
    public void testTemporaryPrice() throws Exception {
        RawOfferImpl first = MockRawOfferBuilder.createMockRawOfferOfficeRentExt(true);
        first.setId("1");
        first.setPhotoPreviews(new ArrayList<>());
        RawTemporaryPriceImpl p = new RawTemporaryPriceImpl();
        p.setValue(100000.0F);
        p.setCurrency("RUB");
        p.setDuration(2);
        first.setTemporaryPrice(p);
        RealtySchema.RawOfferMessage msg = RawOfferProtoConverter.toMessage(first);
        RawOfferImpl second = (RawOfferImpl)RawOfferProtoConverter.fromMessage(msg, mdsUrlBuilder);
        MockUtils.assertEquals(first, second);
    }
}
