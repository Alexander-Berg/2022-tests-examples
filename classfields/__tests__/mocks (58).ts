import { ISiteCardMobile } from 'realty-core/types/siteCard';
import { ILocation } from 'realty-core/types/location';

export const siteCard: ISiteCardMobile = {
    id: 1,
    name: 'Октябрьское поле',
    fullName: 'ЖК «Октябрьское поле»',
    locativeFullName: 'в ЖК «Октябрьское поле»',
    resaleTotalOffers: 56,
    flatStatus: 'ON_SALE',
    timestamp: 0,
    location: ({
        address: 'Москва, ш. Киевское, пос. Московский',
        rgid: 12439,
        settlementRgid: 165705,
        populatedRgid: 741964,
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        metroList: [],
    } as unknown) as ILocation,
    developers: [
        {
            id: 52308,
            name: 'Группа Компаний ПИК',
        },
    ],
    isFromPik: false,
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
};
