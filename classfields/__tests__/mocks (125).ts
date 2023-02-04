import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IOfferCard } from 'realty-core/types/offerCard';

export const state = {
    fsGallery: {
        isClosed: true,
    },
};

const appLargeImages: string[] = [
    generateImageUrl({ width: 300, height: 700 }),
    generateImageUrl({ width: 300, height: 700 }),
    generateImageUrl({ width: 300, height: 700 }),
];

const photoPreviews: string[] = [
    generateImageUrl({ width: 30, height: 70 }),
    generateImageUrl({ width: 30, height: 70 }),
    generateImageUrl({ width: 30, height: 70 }),
];

const fsImages = {
    fullImages: [],
    minicardImages: [],
    cosmicImages: [],
};

const planImages = appLargeImages.slice(0, 1);
const tourImage = generateImageUrl({ width: 300, height: 700 });

export const baseOffer: IOfferCard = ({
    flatType: 'NEW_FLAT',
    appLargeImages,
    photoPreviews,
    price: {},
    ...fsImages,
} as unknown) as IOfferCard;

export const readonlyOfferCardWithoutImages: IOfferCard = ({
    isEditable: false,
    appLargeImages: [],
    photoPreviews: [],
    price: {},
    ...fsImages,
} as unknown) as IOfferCard;

export const editableOfferCardWithoutImages: IOfferCard = ({
    isEditable: true,
    appLargeImages: [],
    photoPreviews: [],
    price: {},
    ...fsImages,
} as unknown) as IOfferCard;

export const offerCardWithAllFeatures: IOfferCard = ({
    ...baseOffer,
    extImages: {
        IMAGE_PLAN: {
            appLargeImages: planImages,
        },
    },
    virtualTours: [
        {
            matterportTour: {
                previewUrl: {
                    origin: tourImage,
                },
            },
        },
    ],
} as unknown) as IOfferCard;
