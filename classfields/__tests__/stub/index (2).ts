import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import { ImageUploaderEntityIds } from 'app/libs/house-services';

import { IUniversalStore } from 'view/modules/types';
import { ImageUploaderEntityId, ImageUploaderImageId } from 'types/imageUploader';
import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';
import {
    HouseServiceMeterTariff,
    HouseServiceMeterType,
    HouseServiceId,
    HouseServiceType,
} from 'types/houseService';

const imageUrl = generateImageUrl({ width: 1000, height: 1000, size: 10 });

const houseService = {
    houseServiceId: 'qwerty' as HouseServiceId,
    [HouseServiceType.METER]: {
        type: HouseServiceMeterType.POWER,
        number: '12345',
        installedPlace: 'В ванной',
        deliverFromDay: 10,
        deliverToDay: 15,
        tariff: HouseServiceMeterTariff.DOUBLE,
        initialMeterReadings: [
            {
                meterValue: 573,
                meterPhoto: {
                    namespace: 'arenda' as ImageNamespaces.ARENDA,
                    groupId: 1396625,
                    name: '9f4107752ebb0b58713ca84dacde044f',
                    imageUrls: [
                        {
                            alias: 'orig' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                    ],
                },
            },
            {
                meterValue: 626,
                meterPhoto: {
                    namespace: 'arenda' as ImageNamespaces.ARENDA,
                    groupId: 1396625,
                    name: '9f4107752ebb0b58713ca84dacde044f',
                    imageUrls: [
                        {
                            alias: 'orig' as DefaultNamespaceAliases,
                            url: imageUrl,
                        },
                    ],
                },
            },
        ],
    },
};

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
        },
    },
    houseService,
    imageUploader: {
        [ImageUploaderEntityIds.T1 as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: ImageUploaderEntityIds.T1 as ImageUploaderEntityId,
                    imageId: 'b1db5a44-028b-49e6-a327-3dbc7920c073' as ImageUploaderImageId,
                    previewUrl: imageUrl,
                    largeUrl: imageUrl,
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
            ],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        [ImageUploaderEntityIds.T2 as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: ImageUploaderEntityIds.T2 as ImageUploaderEntityId,
                    imageId: 'b1db5a44-028b-49e6-a327-3dbc7920c073' as ImageUploaderImageId,
                    previewUrl: imageUrl,
                    largeUrl: imageUrl,
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
            ],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        [ImageUploaderEntityIds.T3 as ImageUploaderEntityId]: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...store,
    spa: {
        status: RequestStatus.PENDING,
    },
};
