import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';
import gallery from '@realty-front/icons/common/gallery-24.svg';

import { GallerySlideType, IGalleryV2Badge, IGalleryV2Image, IGalleryV2ImageWithFs } from 'realty-core/types/galleryV2';

export const baseImages: IGalleryV2Image[] = [
    {
        src: generateImageUrl({ width: 400, height: 700 }),
        preview: generateImageUrl({ width: 30, height: 20 }),
        type: GallerySlideType.COMMON,
    },
    {
        src: generateImageUrl({ width: 300, height: 100 }),
        preview: generateImageUrl({ width: 20, height: 10 }),
        type: GallerySlideType.COMMON,
    },
    {
        src: generateImageUrl({ width: 900, height: 700 }),
        preview: generateImageUrl({ width: 30, height: 20 }),
        type: GallerySlideType.COMMON,
    },
    {
        src: generateImageUrl({ width: 900, height: 700 }),
        preview: generateImageUrl({ width: 30, height: 20 }),
        type: GallerySlideType.COMMON,
    },
];

export const youtubePreviewImage: IGalleryV2Image = {
    src: generateImageUrl({ width: 400, height: 700 }),
    preview: generateImageUrl({ width: 30, height: 20 }),
    type: GallerySlideType.YOUTUBE_PREVIEW,
};

export const tourImage: IGalleryV2Image = {
    src: generateImageUrl({ width: 400, height: 700 }),
    preview: generateImageUrl({ width: 30, height: 20 }),
    type: GallerySlideType.TOUR_PREVIEW,
};

export const planImage: IGalleryV2Image = {
    src: generateImageUrl({ width: 400, height: 700 }),
    preview: generateImageUrl({ width: 30, height: 20 }),
    type: GallerySlideType.PLAN,
};

export const genPlanImage: IGalleryV2ImageWithFs = {
    src: generateImageUrl({ width: 400, height: 700 }),
    preview: generateImageUrl({ width: 30, height: 20 }),
    type: GallerySlideType.GEN_PLAN,
    fs: {
        houseInfo: {
            id: '1839212',
            buildingSiteName: 'корпус 58',
            name: '1',
            maxFloor: 17,
            genplanPolygon: {
                points: [
                    {
                        x: 0.2,
                        y: 0.2,
                    },
                    {
                        x: 0.8,
                        y: 0.2,
                    },
                    {
                        x: 0.8,
                        y: 0.8,
                    },
                    {
                        x: 0.2,
                        y: 0.8,
                    },
                ],
                center: {
                    x: 0.5,
                    y: 0.5,
                },
            },
        },
    },
} as IGalleryV2ImageWithFs;

export const wideImages: IGalleryV2Image[] = [
    {
        src: generateImageUrl({ width: 1400, height: 700 }),
        preview: generateImageUrl({ width: 130, height: 20 }),
        type: GallerySlideType.COMMON,
    },
    {
        src: generateImageUrl({ width: 1300, height: 700 }),
        preview: generateImageUrl({ width: 120, height: 10 }),
        type: GallerySlideType.COMMON,
    },
] as IGalleryV2Image[];

export const badges: IGalleryV2Badge[] = [
    {
        id: '1',
        text: 'без иконки',
        slideIndex: 0,
    },
    {
        id: '2',
        text: 'с иконкой',
        icon: gallery,
        slideIndex: 1,
    },
    {
        id: '3',
        icon: gallery,
        slideIndex: 2,
    },
];
