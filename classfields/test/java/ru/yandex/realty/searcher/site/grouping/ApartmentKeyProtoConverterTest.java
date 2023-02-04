package ru.yandex.realty.searcher.site.grouping;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.realty.model.message.RealtySchema;
import ru.yandex.realty.model.offer.BalconyType;
import ru.yandex.realty.model.offer.BathroomUnitType;
import ru.yandex.realty.model.sites.CommissioningDate;
import ru.yandex.realty.sites.grouping.ApartmentKey;
import ru.yandex.realty.sites.grouping.ApartmentKeyProtoConverter;

import java.util.Random;

public class ApartmentKeyProtoConverterTest {

    @Test
    public void trivialTest() throws Exception {
        Random r = new Random();

        CommissioningDate randomCommissioningDate = randomCommissioningDate();
        ApartmentKey ak =
                new ApartmentKey(
                        randomCommissioningDate,
                        Math.abs(r.nextInt()),
                        Math.abs(r.nextFloat()),
                        Math.abs(r.nextFloat()),
                        Math.abs(r.nextFloat()),
                        Math.abs(r.nextFloat()),
                        Math.abs(r.nextFloat()),
                        randomCommissioningDate.getFinished(),
                        BathroomUnitType.R.fromValueOrNull(Math.abs(r.nextInt(BathroomUnitType.TWO_AND_MORE.value()))),
                        BalconyType.R.fromValueOrNull(Math.abs(r.nextInt(BalconyType.TWO_BALCONY__TWO_LOGGIA.value()))));

        RealtySchema.ApartmentKeyMessage akm = ApartmentKeyProtoConverter.toMessage(ak);
        ApartmentKey again = ApartmentKeyProtoConverter.fromMessage(akm);

        RealtySchema.ApartmentKeyMessage convertedOnceMore = ApartmentKeyProtoConverter.toMessage(again);

        Assert.assertEquals(akm, convertedOnceMore);
        Assert.assertEquals(ak, again);
    }

    private static CommissioningDate randomCommissioningDate() throws Exception {
        Random r = new Random();
        return new CommissioningDate(r.nextInt(), r.nextInt(), r.nextBoolean());
    }

}
