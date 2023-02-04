import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IConstructionQuarterData, IConstructionPhoto } from 'realty-core/types/siteCard';

const photoMock: IConstructionPhoto = {
    full: generateImageUrl({ width: 1280, height: 720 }),
    appMiddleSnippet: generateImageUrl({ width: 960, height: 540 }),
    appSmallSnippet: generateImageUrl({ width: 640, height: 360 }),
};

const verticalPhotoMock: IConstructionPhoto = {
    full: generateImageUrl({ width: 720, height: 1280 }),
    appMiddleSnippet: generateImageUrl({ width: 540, height: 960 }),
    appSmallSnippet: generateImageUrl({ width: 360, height: 640 }),
};

export const dataWithOnePhoto: IConstructionQuarterData = {
    year: 2021,
    quarter: 1,
    photos: [photoMock],
};

export const dataWithTwoPhotos: IConstructionQuarterData = {
    year: 2021,
    quarter: 1,
    photos: Array(2).fill(photoMock),
};

export const dataWithThreePhotos: IConstructionQuarterData = {
    year: 2021,
    quarter: 1,
    photos: Array(3).fill(photoMock),
};

export const dataWithFourPhotos: IConstructionQuarterData = {
    year: 2021,
    quarter: 1,
    photos: Array(4).fill(photoMock),
};

export const dataWithSixPhotos: IConstructionQuarterData = {
    year: 2021,
    quarter: 1,
    photos: Array(6).fill(photoMock),
};

export const dataWithSevenPhotos: IConstructionQuarterData = {
    year: 2021,
    quarter: 1,
    photos: Array(7).fill(photoMock),
};

export const dataWithVerticalPhotos: IConstructionQuarterData = {
    year: 2021,
    quarter: 1,
    photos: Array(7).fill(verticalPhotoMock),
};
