import {
    IOfferCard,
    CommercialPurpose,
    CommercialPurposeWarehouse,
    CommercialType,
} from 'realty-core/types/offerCard';

import { commercialOffer } from '../../__tests__/stubs/offer';

export const commercialOfferWithPurposes = {
    ...commercialOffer,
    commercial: {
        commercialTypes: [CommercialType.BUSINESS_CENTER],
        purposes: [
            CommercialPurpose.BANK,
            CommercialPurpose.BEAUTY_SHOP,
            CommercialPurpose.FOOD_STORE,
            CommercialPurpose.MEDICAL_CENTER,
            CommercialPurpose.SHOW_ROOM,
            CommercialPurpose.TOURAGENCY,
        ],
    },
} as IOfferCard;

export const commercialOfferWithWarehousePurposes = {
    ...commercialOffer,
    commercial: {
        commercialTypes: [CommercialType.BUSINESS_CENTER],
        purposeWarehouses: [
            CommercialPurposeWarehouse.ALCOHOL,
            CommercialPurposeWarehouse.PHARMACEUTICAL_STOREHOUSE,
            CommercialPurposeWarehouse.VEGETABLE_STOREHOUSE,
        ],
    },
} as IOfferCard;

export const commercialOfferWithoutPurposes = {
    ...commercialOffer,
    commercial: {
        commercialTypes: [CommercialType.BUSINESS_CENTER],
    },
} as IOfferCard;
