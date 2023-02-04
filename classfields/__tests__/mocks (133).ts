import { IOfferSnippet } from 'realty-core/types/offerSnippet';

import { newbuildingOffer, rentOffer } from '../../__tests__/stubs/offer';

export const getOfferCard = ({
    offerId,
    priceValue,
    address,
    obsolete = false,
    revokeDate,
}: {
    offerId: string;
    priceValue: string;
    address?: string;
    obsolete?: boolean;
    revokeDate?: string;
}) =>
    (({
        ...newbuildingOffer,
        location: {
            ...newbuildingOffer.location,
            address: address || newbuildingOffer.location.address,
        },
        appMiniSnippetImages: [],
        area: {
            value: 67.13,
            unit: 'SQUARE_METER',
        },
        cosmicImages: [],
        internal: true,
        suspicious: false,
        appMiddleSnippetImages: [],
        roomsTotal: 2,
        totalImages: 12,
        offerId,
        trustedOfferInfo: {
            isFullTrustedOwner: false,
            ownerTrustedStatus: 'NOT_LINKED_MOSRU',
            isCadastrPersonMatched: false,
        },
        trust: 'NORMAL',
        mainImages: [],
        price: {
            ...newbuildingOffer.price,
            value: priceValue,
        },
        vasAvailable: true,
        offerType: 'SELL',
        origImages: [],
        apartment: {
            renovation: 'CLEAN',
            decoration: 'CLEAN',
            siteFlatPlanId: '1706831-0A020802-B40EB9E1CBFE730C',
        },
        withExcerpt: false,
        offerCategory: 'APARTMENT',
        partnerId: '1069252863',
        promoted: true,
        partnerInternalId: '8256',
        large1242Images: [],
        appLargeImages: [],
        appLargeSnippetImages: [],
        appSmallSnippetImages: [],
        alikeImages: [],
        newFlatSale: true,
        appMiddleImages: [],
        house: {
            apartments: false,
            housePart: false,
        },
        raised: true,
        premium: true,
        relevance: 0,
        dealStatus: 'PRIMARY_SALE',
        exclusive: true,
        paidReportAccessibility: 'PPA_NOT_ALLOWED_TO_BUY',
        uid: '4071110006',
        fullImages: [],
        minicardImages: [],
        chargeForCallsType: 'BY_CAMPAIGN',
        flatType: 'NEW_FLAT',
        active: true,
        obsolete,
        revokeDate,
    } as unknown) as IOfferSnippet);

export const getInitialState = () => ({
    offerPhones: {},
});

export const getOffers = () => {
    const cards = [];

    for (let i = 0; i < 8; i++) {
        cards.push(getOfferCard({ offerId: `${i + 1}`, priceValue: `${1000000 + i}` }));
    }

    return cards;
};

export const getOffersWithSmallAddress = () =>
    [getOfferCard({ offerId: 'one-line', priceValue: '10000', address: 'Москва, Ленина' })].concat(getOffers());

export const getOffersWithLongAddress = () =>
    [
        getOfferCard({
            offerId: 'one-line',
            priceValue: '10000',
            address: 'Москва, улица с очень длинным названием, которое точно занимает больше чем 2 строки',
        }),
    ].concat(getOffers());

export const getRentOffers = (): IOfferSnippet[] => {
    const cards = [];

    for (let i = 0; i < 8; i++) {
        cards.push({ ...rentOffer, offerId: `offer-${i}` });
    }

    return cards as IOfferSnippet[];
};

export const getOffersWithDeactivated = () => {
    const revokeDate = new Date();
    revokeDate.setHours(revokeDate.getHours() - 1);

    return [
        getOfferCard({
            offerId: 'deactivated',
            priceValue: '10000',
            address: 'Москва, улица',
            obsolete: true,
            revokeDate: revokeDate.toString(),
        }),
    ].concat(getOffers());
};

export const onIntersection = () => null;

export const fastlinksMock = [
    {
        urlPath: '/moskva/kupit/kvartira/st-malyj-palashyovskij-pereulok-195066/elit-premium-klass/',
        offersCount: 1,
        urlTitle: '',
        title: {
            title: 'Купить квартиру',
            description: 'элит и премиум класс Малый Палашёвский переулок',
        },
    },
    {
        urlPath: '/moskva/kupit/kvartira/st-malyj-palashyovskij-pereulok-195066/ekonom-klass/',
        offersCount: 1,
        urlTitle: '',
        title: {
            title: 'Купить 1-комн.',
            description: 'эконом класс Малый Палашёвский переулок',
        },
    },
    {
        urlPath: '/moskva/kupit/kvartira/odnokomnatnaya/ryadom-metro/',
        offersCount: 783,
        urlTitle: '',
        title: {
            title: 'Купить 1-комн.',
            description: 'рядом с метро',
        },
    },
    {
        urlPath: '/moskva/kupit/kvartira/st-malyj-palashyovskij-pereulok-195066/s-vysokimi-potolkami/',
        offersCount: 1,
        urlTitle: '',
        title: {
            title: 'Купить квартиру',
            description: 'с высокими потолками Малый Палашёвский переулок',
        },
    },
    {
        urlPath: '/moskva/kupit/kvartira/st-malyj-palashyovskij-pereulok-195066/v-pyatietazhnom-dome/',
        offersCount: 1,
        urlTitle: '',
        title: {
            title: 'Купить квартиру',
            description: 'в пятиэтажных домах Малый Палашёвский переулок',
        },
    },
    {
        urlPath: '/moskva/kupit/kvartira/st-malyj-palashyovskij-pereulok-195066/vtorichniy-rynok/',
        offersCount: 1,
        urlTitle: '',
        title: {
            title: 'Купить квартиру',
            description: 'на вторичном рынке Малый Палашёвский переулок',
        },
    },
    {
        urlPath: '/moskva/kupit/kvartira/st-malyj-palashyovskij-pereulok-195066/s-bolshoy-kuhney/',
        offersCount: 1,
        urlTitle: '',
        title: {
            title: 'Купить квартиру',
            description: 'с большой кухней Малый Палашёвский переулок',
        },
    },
];
