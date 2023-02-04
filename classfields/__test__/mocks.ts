import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IConstructionQuarterData, IConstructionPhoto, ISiteCardMobile } from 'realty-core/types/siteCard';
import { ILocation } from 'realty-core/types/location';
import { IFSGalleryStore } from 'realty-core/view/react/modules/fs-gallery/redux/types';

export const fsGalleryMock: IFSGalleryStore = {
    index: 0,
    images: [],
    isClosed: true,
    isPlayingInteractiveSlide: false,
    id: null,
};

const photoMock: IConstructionPhoto = {
    full: generateImageUrl({ width: 1280, height: 720 }),
    appMiddleSnippet: generateImageUrl({ width: 960, height: 540 }),
    appSmallSnippet: generateImageUrl({ width: 640, height: 360 }),
};

const quarterData: IConstructionQuarterData = {
    quarter: 1,
    year: 2020,
    photos: Array(7).fill(photoMock),
};

const card: ISiteCardMobile = {
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
        settlementRgid: 587795,
        populatedRgid: 741964,
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

export const cardWithOneQuarter = {
    ...card,
    construction: [quarterData],
};

export const cardWithFourQuarters = {
    ...card,
    construction: Array(4)
        .fill(quarterData)
        .map((quarterData, index) => ({
            ...quarterData,
            quarter: 4 - index,
        })),
};

export const baseInitialState = {
    similar: {
        sites: [],
    },
    fsGallery: fsGalleryMock,
    pageParams: {
        isFromPik: false,
        page: 1,
        directGalleryPhoto: 1,
    },
};
