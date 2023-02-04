import { ISiteCard } from 'realty-core/types/siteCard';
import { ILocation } from 'realty-core/types/location';

export const item: ISiteCard = {
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
    location: ({
        address: 'Москва, ш. Киевское, пос. Московский',
        rgid: 12439,
        settlementRgid: 165705,
        populatedRgid: 741964,
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        metroList: [],
    } as unknown) as ILocation,
    regionInfo: {
        parents: [],
        rgid: 417899,
        populatedRgid: 741964,
        name: 'Москва',
        sitesRgids: {},
        locative: 'в Москве',
        isInLO: false,
        isInMO: true,
    },
} as ISiteCard;

export const longDeveloperItem: ISiteCard = {
    ...item,
    developer: {
        ...item.developer,
        name: 'Застройщик с очень длинным названием',
    },
} as ISiteCard;
