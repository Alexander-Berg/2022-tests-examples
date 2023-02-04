import { ISiteCard } from 'realty-core/types/siteCard';

export const siteCard: ISiteCard = ({
    id: 1,
    name: 'Октябрьское поле',
    fullName: 'ЖК «Октябрьское поле»',
    locativeFullName: 'в ЖК «Октябрьское поле»',
    resaleTotalOffers: 56,
    flatStatus: 'ON_SALE',
    timestamp: 0,
    developer: {
        id: 52308,
        name: 'Группа Компаний ПИК',
        hasChat: true,
    },
    developers: [
        {
            id: 52308,
            name: 'Группа Компаний ПИК',
        },
    ],
    isFromPik: false,
    location: {
        address: 'Москва, ш. Киевское, пос. Московский',
        rgid: 12439,
        settlementRgid: 165705,
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        metroList: [],
    },
    regionInfo: {
        parents: [],
        rgid: 417899,
        name: 'Москва',
        sitesRgids: {},
        locative: 'в Москве',
        isInLO: false,
        isInMO: true,
    },
    offerStat: {
        primarySaleOffers: 1,
        primarySaleHouses: 1,
        primarySalePlans: 1,
        primarySaleRoomWithMinOffersCount: {
            roomType: 'PLUS_4',
            offersCount: 10,
        },
    },
} as unknown) as ISiteCard;
