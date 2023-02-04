import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { ImageUploaderEntityId, ImageUploaderImageId } from 'types/imageUploader';

import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';

import { HouseServiceMeterTariff, HouseServiceMeterType, HouseServiceId, HouseServiceType } from 'types/houseService';

import { IBreadcrumb } from 'types/breadcrumbs';

import { getFields } from 'app/libs/house-services/house-service-form';
import { ImageUploaderEntityIds } from 'app/libs/house-services';

import { IUniversalStore } from 'view/modules/types';

import { initialState } from 'view/modules/houseServiceForm/reducers/fields';

import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';

const breadcrumbs: IBreadcrumbsStore = {
    crumbs: [
        {
            route: 'user-flat',
        } as IBreadcrumb,
        {
            route: 'owner-house-services-settings-preview',
            params: {
                flatId: '99ef4c3d93474534989837f1ae3bbb9c',
            },
        },
        {
            route: 'owner-house-services-settings-form',
            params: {
                flatId: '99ef4c3d93474534989837f1ae3bbb9c',
            },
        },
        {
            route: 'owner-house-service-list',
            params: {
                flatId: '99ef4c3d93474534989837f1ae3bbb9c',
            },
        },
    ],
    current: {
        route: 'owner-house-service-form',
        params: {
            flatId: '99ef4c3d93474534989837f1ae3bbb9c',
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
                            url:
                                // eslint-disable-next-line max-len
                                '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/orig',
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
                            url:
                                // eslint-disable-next-line max-len
                                '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/orig',
                        },
                    ],
                },
            },
        ],
    },
};

export const store: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
            counterType: 'WATER_COLD',
        },
    },
    houseServiceForm: {
        network: {
            updateHouseServiceFormStatus: RequestStatus.LOADED,
        },
        fields: initialState,
    },
    imageUploader: {
        [ImageUploaderEntityIds.T1 as ImageUploaderEntityId]: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        [ImageUploaderEntityIds.T2 as ImageUploaderEntityId]: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        [ImageUploaderEntityIds.T3 as ImageUploaderEntityId]: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};

export const onlyContentStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    cookies: { ['only-content']: 'true' },
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
            counterType: 'WATER_COLD',
        },
    },
    houseServiceForm: {
        network: {
            updateHouseServiceFormStatus: RequestStatus.LOADED,
        },
        fields: initialState,
    },
    imageUploader: {
        ['T-1' as ImageUploaderEntityId]: { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED },
        ['T-2' as ImageUploaderEntityId]: { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED },
        ['T-3' as ImageUploaderEntityId]: { images: [], getImageUploaderUrlStatus: RequestStatus.LOADED },
    },
};

export const filledStore: DeepPartial<IUniversalStore> = {
    ...store,
    page: {
        params: {
            flatId: '12345',
            houseServiceId: '435234',
        },
    },
    houseService,
    houseServiceForm: {
        network: {
            updateHouseServiceFormStatus: RequestStatus.LOADED,
        },
        fields: getFields(houseService[HouseServiceType.METER]),
    },
    imageUploader: {
        [ImageUploaderEntityIds.T1 as ImageUploaderEntityId]: {
            images: [
                {
                    entityId: ImageUploaderEntityIds.T1 as ImageUploaderEntityId,
                    imageId: 'b1db5a44-028b-49e6-a327-3dbc7920c073' as ImageUploaderImageId,
                    previewUrl: '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/128x128',
                    largeUrl: '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/orig',
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
                    previewUrl: '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/128x128',
                    largeUrl: '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/orig',
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
