import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import * as fsGalleryActions from 'realty-core/view/react/modules/fs-gallery/redux/actions';
import { IConstructionQuarterData, IConstructionPhoto } from 'realty-core/types/siteCard';

import { INewbuildingProgressQuarterPhotosProps } from '../index';

const photoMock: IConstructionPhoto = {
    full: generateImageUrl({ width: 1280, height: 720 }),
    appMiddleSnippet: generateImageUrl({ width: 960, height: 540 }),
    appSmallSnippet: generateImageUrl({ width: 640, height: 360 }),
};

const quarterWithLotPhotos: IConstructionQuarterData = {
    quarter: 1,
    year: 2021,
    photos: Array(7).fill(photoMock),
};

const quarterWithFewPhotos: IConstructionQuarterData = {
    quarter: 1,
    year: 2021,
    photos: Array(2).fill(photoMock),
};

export const propsWithLotPhotos: INewbuildingProgressQuarterPhotosProps = {
    fsGalleryActions,
    quarterData: quarterWithLotPhotos,
};

export const propsWithFewPhotos = {
    fsGalleryActions,
    quarterData: quarterWithFewPhotos,
};
