import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IConstructionQuarterData, IConstructionPhoto, ISiteCard } from 'realty-core/types/siteCard';
import { ILocation } from 'realty-core/types/location';

const photoMock: IConstructionPhoto = {
    full: generateImageUrl({ width: 1280, height: 720 }),
    appMiddleSnippet: generateImageUrl({ width: 960, height: 540 }),
    appSmallSnippet: generateImageUrl({ width: 640, height: 360 }),
};

const quarterWithLotPhotos: IConstructionQuarterData = {
    quarter: 1,
    year: 2021,
    photos: Array(10).fill(photoMock),
};

const quarterWithFewPhotos: IConstructionQuarterData = {
    quarter: 1,
    year: 2021,
    photos: Array(2).fill(photoMock),
};

const card: ISiteCard = {
    id: 1686471,
    name: 'Люблинский парк',
    locativeFullName: 'в жилом комплексе «Люблинский парк»',
    fullName: 'жилой комплекс «Люблинский парк»',
    withOffers: false,
    resaleTotalOffers: 0,
    flatStatus: 'ON_SALE',
    timestamp: 1627466842541,
    location: ({
        rgid: 193348,
        populatedRgid: 741964,
        settlementRgid: 587795,
        subjectFederationRgid: 741964,
        subjectFederationId: 1,
        address: 'Москва, ул. Люблинская, вл. 72',
    } as unknown) as ILocation,
    isFromPik: false,
    regionInfo: {
        parents: [],
        rgid: 417899,
        populatedRgid: 741964,
        name: 'Москва',
        locative: 'в Москве',
        isInLO: false,
        isInMO: true,
    },
};

export const cardWithLotPhotos = {
    ...card,
    construction: Array(5).fill(quarterWithLotPhotos),
};

export const cardWithFewPhotos = {
    ...card,
    construction: Array(5).fill(quarterWithFewPhotos),
};
