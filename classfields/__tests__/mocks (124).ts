import noop from 'lodash/noop';
import { DeepPartial } from 'utility-types';

import { BackCallStatus } from 'realty-core/view/react/modules/back-call/types';

import {
    IOfferCard,
    OfferType,
    AuthorCategoryTypes,
    OfferCategory,
} from 'realty-core/types/offerCard';
import { TransportDistanceType } from 'realty-core/types/location';

export const offer: DeepPartial<IOfferCard> = {
    offerId: '1',
    uid: '123 123 144',
    offerType: OfferType.SELL,
    offerCategory: OfferCategory.APARTMENT,
    socialPessimization: true,
    location: {
        rgid: 1,
        address: 'Москва, переулок Сивцев Вражек, 21',
        metroList: [
            {
                name: 'Кропоткинская',
                timeToMetro: 22,
                rgbColor: 'e4402d',
                metroTransport: TransportDistanceType.ON_FOOT,
                metroGeoId: 20494,
            },
            {
                name: 'Кропоткинская',
                timeToMetro: 32,
                rgbColor: 'e4402d',
                metroTransport: TransportDistanceType.ON_FOOT,
                metroGeoId: 20495,
            },
            {
                name: 'Смоленская',
                timeToMetro: 42,
                rgbColor: '099dd4',
                metroTransport: TransportDistanceType.ON_FOOT,
                metroGeoId: 20482,
            },
            {
                name: 'Кропоткинская',
                timeToMetro: 32,
                rgbColor: 'e4402d',
                metroTransport: TransportDistanceType.ON_FOOT,
                metroGeoId: 20495,
            },
        ],
    },
    area: {
        unit: 'SQUARE_METER',
        value: 120,
    },
    floorsOffered: [2],
    floorsTotal: 22,
    author: {
        category: AuthorCategoryTypes.OWNER,
        creationDate: '',
        agentName: 'Иванов Иван Иванович',
    },
    trustedOfferInfo: {
        isFullTrustedOwner: true,
    },
    price: {
        currency: 'RUR',
        value: 123456789,
    },
    roomsTotal: 4,
};

export const yandexRentOffer = {
    ...offer,
    yandexRent: true,
    author: {
        category: AuthorCategoryTypes.AGENCY,
        creationDate: '',
        agentName: 'Яндекс.Аренда',
    },
};

export const emptyMetroListOffer = {
    ...offer,
    location: {
        ...offer.location,
        metroList: undefined,
    },
};

export const defaultState = {
    cards: { offers: offer },
    user: {
        favoritesMap: {},
        isAuth: true,
        uid: '123',
    },
    offerPhones: {},
    backCall: {
        status: BackCallStatus.UNSET,
    },
};

export const stateWithPhone = {
    ...defaultState,
    backCall: {
        status: 'normal',
        value: '+7 123 456-78-90',
    },
};

export const stateWithFavorite = {
    ...defaultState,
    user: {
        favoritesMap: { 1: true },
    },
};

export const backCallSuccessState = {
    ...defaultState,
    backCallApi: {
        SITE: {
            123: { sourceNumber: '+79991110833' },
        },
    },
};

export const GatePending = {
    get: () => new Promise(noop),
};

export const PhoneGateSuccess = {
    get: () =>
        Promise.resolve([
            {
                isRedirectPhones: true,
                shouldShowRedirectIndicator: true,
                phones: [{ phoneNumber: '+79998887766', redirectId: '+79998887766' }],
            },
        ]),
};

export const TwoPhonesGateSuccess = {
    get: () =>
        Promise.resolve([
            {
                isRedirectPhones: true,
                shouldShowRedirectIndicator: true,
                phones: [
                    { phoneNumber: '+79998887766', redirectId: '+79998887766' },
                    { phoneNumber: '+71234567890', redirectId: '+71234567890' },
                ],
            },
        ]),
};

export const PhoneGateWithHintInfo = {
    get: () =>
        Promise.resolve([
            {
                withBilling: true,
                isRedirectPhones: true,
                phones: [{ phoneNumber: '+79998887766', redirectId: '+79998887766' }],
            },
        ]),
};
