package ru.auto.tests.realty.vos2.testdata;

import ru.auto.tests.realty.vos2.model.CreateUserRequest;
import ru.auto.tests.realty.vos2.model.RealtyOfferPrice;
import ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate;

import static ru.auto.tests.realty.vos2.model.CreateUserRequest.PaymentTypeEnum.JURIDICAL_PERSON;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.PaymentTypeEnum.NATURAL_PERSON;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.TypeEnum.NUMBER_0;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.TypeEnum.NUMBER_1;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.TypeEnum.NUMBER_2;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.TypeEnum.NUMBER_3;
import static ru.auto.tests.realty.vos2.model.CreateUserRequest.TypeEnum.NUMBER_7;
import static ru.auto.tests.realty.vos2.model.RealtyOfferPrice.CurrencyEnum.BYR;
import static ru.auto.tests.realty.vos2.model.RealtyOfferPrice.CurrencyEnum.EUR;
import static ru.auto.tests.realty.vos2.model.RealtyOfferPrice.CurrencyEnum.KZT;
import static ru.auto.tests.realty.vos2.model.RealtyOfferPrice.CurrencyEnum.RUB;
import static ru.auto.tests.realty.vos2.model.RealtyOfferPrice.CurrencyEnum.UAH;
import static ru.auto.tests.realty.vos2.model.RealtyOfferPrice.CurrencyEnum.USD;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.BY_DEFAULT;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.NO_NEED;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.OTHER;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.SOLD_HERE;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.SOLD_OUTSIDE;

public class TestData {

    private TestData() {
    }

    public static Object[] defaultUserTypes() {
        return new CreateUserRequest.TypeEnum[]{
                NUMBER_0,
                NUMBER_1,
                NUMBER_2,
                NUMBER_3,
                NUMBER_7,
        };
    }

    public static Object[] defaultPaymentTypes() {
        return new CreateUserRequest.PaymentTypeEnum[]{
                JURIDICAL_PERSON,
                NATURAL_PERSON
        };
    }

    public static String[] defaultOffers() {
        return new String[]{
                "offers/apartment_rent.json",
                "offers/apartment_sell.json",
                "offers/commercial_business_center_sell.json",
                "offers/commercial_retail_rent.json",
                "offers/commercial_retail_sell.json",
                "offers/garage_sell.json",
                "offers/house_rent.json",
                "offers/house_sell.json",
                "offers/lot_sell.json",
                "offers/room_rent.json",
                "offers/room_sell.json"
        };
    }

    public static Object[] defaultOfferCurrency() {
        return new RealtyOfferPrice.CurrencyEnum[]{
                RUB,
                USD,
                EUR,
                UAH,
                BYR,
                KZT
        };
    }

    public static Object[] defaultOfferReason() {
        return new RealtyOfferStatusUpdate.ReasonEnum[]{
                BY_DEFAULT,
                SOLD_HERE,
                NO_NEED,
                OTHER,
                SOLD_OUTSIDE
        };
    }

    public static String[] defaultProductTypes() {
        return new String[]{
                "PREMIUM",
                "RAISING",
                "PROMOTION"
        };
    }

    public static String[] defaultRenewalsShowStatus() {
        return new String[]{
                "ALL",
                "ACTIVE",
                "INACTIVE",
                "WARNING",
                "ERROR",
                "CONDITIONS_CHANGED"
        };
    }
}
