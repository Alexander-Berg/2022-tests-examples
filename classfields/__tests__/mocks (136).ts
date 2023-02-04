import { AreaUnits, IOfferCard } from 'realty-core/types/offerCard';

import {
    newbuildingOffer,
    rentOffer,
    secondaryOffer,
    commercialOffer,
    houseOffer,
    garageOffer,
    rentCommercialOffer,
} from '../../__tests__/stubs/offer';

export { newbuildingOffer, rentOffer, secondaryOffer, commercialOffer, houseOffer, garageOffer, rentCommercialOffer };

export const newbuildingOfferWithPriceChange = {
    ...newbuildingOffer,
    price: { ...newbuildingOffer.price, trend: 'INCREASED' },
} as IOfferCard;

export const rentOfferWithPriceChange = {
    ...rentOffer,
    price: { ...rentOffer.price, trend: 'INCREASED' },
} as IOfferCard;

export const secondaryOfferWithPriceChange = {
    ...secondaryOffer,
    price: { ...secondaryOffer.price, trend: 'INCREASED' },
} as IOfferCard;

export const commercialOfferWithPriceChange = {
    ...commercialOffer,
    price: { ...commercialOffer.price, trend: 'INCREASED' },
} as IOfferCard;

export const houseOfferWithPriceChange = {
    ...houseOffer,
    price: { ...houseOffer.price, trend: 'DECREASED' },
} as IOfferCard;

export const garageOfferWithPriceChange = {
    ...garageOffer,
    price: { ...garageOffer.price, trend: 'DECREASED' },
} as IOfferCard;

export const rentCommercialOfferWithPriceChange = {
    ...rentCommercialOffer,
    price: { ...rentCommercialOffer.price, trend: 'DECREASED' },
} as IOfferCard;

export const houseOfferWithAreCost = {
    ...houseOffer,
    price: { ...houseOffer.price, unitPerPart: AreaUnits.ARE },
};
