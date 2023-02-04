import { AuthorCategoryTypes, IOfferCard } from 'realty-core/types/offerCard';
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
} as unknown) as ISiteCard;

export const offerCard = ({
    author: {
        category: AuthorCategoryTypes.AGENCY,
    },
} as unknown) as IOfferCard;
