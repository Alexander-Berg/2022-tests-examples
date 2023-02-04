import pick from 'lodash/pick';

import { ConstructionState, ISiteCardBaseType, ISiteRoomStats, SiteRoomStatsTypes } from 'realty-core/types/siteCard';
import { ILocation } from 'realty-core/types/location';

export const getSiteCard = (buildingFeatures?: Partial<ISiteCardBaseType['buildingFeatures']>): ISiteCardBaseType => ({
    id: 123,
    name: 'card',
    locativeFullName: 'site card',
    fullName: 'site card',
    resaleTotalOffers: 0,
    flatStatus: 'ON_SALE',
    timestamp: 0,
    location: ({
        rgid: 123,
        settlementRgid: 123,
        populatedRgid: 123,
        subjectFederationRgid: 123,
        subjectFederationId: 123,
        address: 'Россия',
    } as unknown) as ILocation,
    isFromPik: false,
    buildingFeatures: {
        state: ConstructionState.BUILT,
        ...buildingFeatures,
    },
    regionInfo: {
        parents: [
            {
                id: 120538,
                rgid: '193368',
                name: 'Пресненский район',
                type: 'CITY_DISTRICT',
            },
        ],
        rgid: 197177,
        populatedRgid: 741964,
        name: 'Московский Международный Деловой Центр Москва-Сити',
        locative: 'Москва-Сити',
        isInLO: false,
        isInMO: true,
    },
});

export interface IGetSiteCardWithCustomAreasProps {
    rooms?: Record<SiteRoomStatsTypes, ISiteRoomStats>;
    common?: { areaFrom: number; areaTo: number };
}

export const getSiteCardWithCustomAreas = (props: IGetSiteCardWithCustomAreasProps): ISiteCardBaseType => {
    const { rooms, common } = props;
    const siteCardMock = {
        ...getSiteCard(),
        price: {
            from: 1000000,
            to: 10000000,
            minPricePerMeter: 50000,
            maxPricePerMeter: 110000,
            totalOffers: 10,
            rooms,
        },
    };

    if (common?.areaFrom && common?.areaTo) {
        siteCardMock.areasRange = [common.areaFrom, common.areaTo];
    }

    return siteCardMock;
};

export const getRoomsData = (
    types: Array<SiteRoomStatsTypes>,
    soldTypes: Array<SiteRoomStatsTypes> = []
): Record<SiteRoomStatsTypes, ISiteRoomStats> => {
    const selected = pick(roomsMock, ...types);

    if (soldTypes.length) {
        soldTypes.forEach((type) => {
            selected[type].soldout = true;
            selected[type].status = 'SOLD';
        });
    }

    return selected;
};

const roomsMock: Record<SiteRoomStatsTypes, ISiteRoomStats> = {
    1: {
        areas: { from: '34.6', to: '45' },
        currency: 'RUR',
        from: 10767120,
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        soldout: false,
        status: 'ON_SALE',
        to: 10000000,
    },
    2: {
        areas: { from: '49.5', to: '64.5' },
        currency: 'RUR',
        from: 14236200,
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        soldout: false,
        status: 'ON_SALE',
        to: 22081000,
    },
    3: {
        areas: { from: '75.2', to: '83.8' },
        currency: 'RUR',
        from: 18914000,
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        soldout: false,
        status: 'ON_SALE',
        to: 26352500,
    },
    PLUS_4: {
        areas: { from: '96.4', to: '100' },
        currency: 'RUR',
        from: 26990000,
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        soldout: false,
        status: 'ON_SALE',
        to: 30000000,
    },
    STUDIO: {
        areas: { from: '23.3', to: '27.6' },
        currency: 'RUR',
        from: 8193660,
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        soldout: false,
        status: 'ON_SALE',
        to: 10322400,
    },
    OPEN_PLAN: {
        currency: 'RUR',
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        soldout: false,
    },
};
