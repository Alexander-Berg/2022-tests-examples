import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import { OutstaffRoles } from 'types/outstaff';

import {
    ImageUploaderEntityId,
    ImageUploaderImageId,
    ImageUploaderImageStatus,
    IUploaderUploadedImage,
} from 'types/imageUploader';

import { initialState as retoucherFormFields } from 'view/modules/outstaffRetoucherForm/reducers/fields';
import { initialState as retoucherFormNetwork } from 'view/modules/outstaffRetoucherForm/reducers/network';
import { Fields } from 'view/modules/outstaffRetoucherForm/types';
import { IUniversalStore } from 'view/modules/types';

import { commonStore } from './common.ts';

export const photoRawUrl = 'https://photos.ru';
export const photoRetouchedUrl = 'https://retouch.ru';

const previewStub = generateImageUrl({ width: 160, height: 140, size: 10 });

export const baseStore: DeepPartial<IUniversalStore> = {
    ...commonStore,
    page: {
        params: {
            role: OutstaffRoles.retoucher,
        },
    },
    outstaffPhotographerForm: {
        fields: retoucherFormFields,
        network: retoucherFormNetwork,
    },
    config: {
        isMobile: '',
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const withPhotoRawUrlStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    managerFlatQuestionnaire: {
        questionnaire: {
            media: {
                photoRawUrl: photoRawUrl,
            },
        },
    },
    imageUploader: {
        '000000': {
            images: [
                {
                    entityId: '' as ImageUploaderEntityId,
                    imageId: '1' as ImageUploaderImageId,
                    previewUrl: previewStub,
                    largeUrl: '',
                    status: ImageUploaderImageStatus.LOADED,
                    uploaderData: {} as IUploaderUploadedImage,
                },
                {
                    entityId: '' as ImageUploaderEntityId,
                    imageId: '2' as ImageUploaderImageId,
                    previewUrl: previewStub,
                    largeUrl: '',
                    status: ImageUploaderImageStatus.LOADED,
                },
                {
                    entityId: '' as ImageUploaderEntityId,
                    imageId: '3' as ImageUploaderImageId,
                    previewUrl: previewStub,
                    largeUrl: '',
                    status: ImageUploaderImageStatus.LOADED,
                    uploaderData: {} as IUploaderUploadedImage,
                },
                {
                    entityId: '' as ImageUploaderEntityId,
                    imageId: '4' as ImageUploaderImageId,
                    previewUrl: previewStub,
                    largeUrl: '',
                    status: ImageUploaderImageStatus.LOADED,
                },
            ],
        },
    },
};

export const fullFilledStore: DeepPartial<IUniversalStore> = {
    ...baseStore,
    managerFlatQuestionnaire: {
        questionnaire: {
            media: {
                photoRawUrl: photoRawUrl,
                photoRetouchedUrl: photoRetouchedUrl,
            },
        },
    },
    outstaffRetoucherForm: {
        fields: {
            [Fields.PHOTO_RETOUCHED_URL]: {
                id: Fields.PHOTO_RETOUCHED_URL,
                value: photoRetouchedUrl,
            },
        },
        network: retoucherFormNetwork,
    },
};
