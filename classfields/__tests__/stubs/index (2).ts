import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import {
    IImageUploaderImage,
    ImageUploaderEntityId,
    ImageUploaderImageId,
    ImageUploaderImageStatus,
    IUploaderUploadedImage,
} from 'types/imageUploader';

import { IUniversalStore } from 'view/modules/types';

const previewUrl = generateImageUrl({ width: 357, height: 140, size: 10 });

export const images: IImageUploaderImage[] = [
    {
        entityId: '1' as ImageUploaderEntityId,
        imageId: '2' as ImageUploaderImageId,
        previewUrl: previewUrl,
        largeUrl: '',
        status: ImageUploaderImageStatus.SAVED,
        uploaderData: {} as IUploaderUploadedImage,
    },
];

export const withUploadedImage: DeepPartial<IUniversalStore> = {
    config: {
        isMobile: 'mobile',
    },
    imageUploader: {
        NEW: {
            images: [images[0]],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};

export const withoutUploadedImage: DeepPartial<IUniversalStore> = {
    config: {
        isMobile: 'mobile',
    },
    imageUploader: {
        NEW: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};
