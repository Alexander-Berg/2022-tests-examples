import { IOfferCard } from 'realty-core/types/offerCard';
import { IAddressComponent } from 'realty-core/types/location';
import { ISiteCard } from 'realty-core/types/siteCard';

import { newbuildingOffer } from '../../__tests__/stubs/offer';

const generateOffer = (offer: IOfferCard, structuredAddressData: IAddressComponent[]): IOfferCard => ({
    ...offer,
    location: {
        ...offer.location,
        structuredAddress: {
            ...offer.location.structuredAddress,
            component: structuredAddressData,
        },
    },
});

export const offerWithoutStructuredAddress = generateOffer(newbuildingOffer, []);

export const offerWithCity = generateOffer(newbuildingOffer, ([
    { address: 'Город Зеро', regionType: 'CITY' },
] as unknown) as IAddressComponent[]);

export const offerWithDistrict = generateOffer(newbuildingOffer, ([
    { address: 'Какой-то округ', regionType: 'SUBJECT_FEDERATION_DISTRICT' },
] as unknown) as IAddressComponent[]);

export const offerWithCityAndDistrict = generateOffer(newbuildingOffer, ([
    { address: 'Город Зеро', regionType: 'CITY' },
    { address: 'Какой-то округ', regionType: 'SUBJECT_FEDERATION_DISTRICT' },
] as unknown) as IAddressComponent[]);

export const site = ({
    fullName: 'ЖК Ромашка',
} as unknown) as ISiteCard;
