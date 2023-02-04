import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import { ImageUploaderEntityId, ImageUploaderImageId } from 'types/imageUploader';
import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';
import { HouseServiceMeterTariff, HouseServiceMeterType, HouseServiceId, HouseServiceType } from 'types/houseService';

import { IBreadcrumb } from 'types/breadcrumbs';

import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';
import { IUniversalStore } from 'view/modules/types';

const imageUrl = generateImageUrl({ width: 1000, height: 1000, size: 10 });

const breadcrumbs: IBreadcrumbsStore = {
    crumbs: [
        {
            route: 'user-flat',
        } as IBreadcrumb,
        {
            route: 'owner-house-services-settings-preview',
            params: {
                flatId: 'f3529a5d9e854b0b992f8381dfe72ef1',
            },
        },
    ],
    current: {
        route: 'owner-house-service',
        params: {
            flatId: 'f3529a5d9e854b0b992f8381dfe72ef1',
        },
    },
};

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
    breadcrumbs,
    page: {
        params: {
            flatId: '12345',
        },
    },
    houseService,
    imageUploader: {
        ['T-1' as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: 'T-1' as ImageUploaderEntityId,
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
        ['T-2' as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: 'T-2' as ImageUploaderEntityId,
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
        ['T-3' as ImageUploaderEntityId]: { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED },
    },
};

export const onlyContentStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs,
    cookies: { ['only-content']: 'true' },
    page: {
        params: {
            flatId: '12345',
        },
    },
    houseService,
    imageUploader: {
        ['T-1' as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: 'T-1' as ImageUploaderEntityId,
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
        ['T-2' as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: 'T-2' as ImageUploaderEntityId,
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
        ['T-3' as ImageUploaderEntityId]: { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED },
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    config: {
        isMobile: 'iPhone',
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...store,
    spa: {
        status: RequestStatus.PENDING,
    },
};
