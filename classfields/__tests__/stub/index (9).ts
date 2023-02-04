import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';
import { Flavor } from 'realty-core/types/utils';

import { ImageUploaderEntityId, ImageUploaderImageId } from 'types/imageUploader';
import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';
import { HouseServiceMeterTariff, HouseServiceMeterType, HouseServiceId, HouseServiceType } from 'types/houseService';
import { FlatStatus } from 'types/flat';

import { getFields } from 'app/libs/house-services/house-service-form';
import { ImageUploaderEntityIds } from 'app/libs/house-services';

import { IUniversalStore } from 'view/modules/types';
import { initialState } from 'view/modules/houseServiceForm/reducers/fields';

const houseService = {
    houseServiceId: 'qwerty' as HouseServiceId,
    createTime: '2022-07-05T11:14:17.603Z',
    updateTime: '2022-07-06T11:10:23.603Z',
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
    spa: {
        status: RequestStatus.LOADED,
    },
    managerFlat: {
        flat: {
            address: {
                address: 'г Москва, ул Фрязевская, д 10, кв 1',
                flatNumber: '10',
            },
            flatId: 'flat10' as Flavor<string, 'FlatID'>,
            assignedUsers: [],
            desiredRentAmount: '3000000',
            status: FlatStatus.CONFIRMED,
            code: '12-DDFF',
        },
        actualContract: {},
    },
    breadcrumbs: {
        crumbs: [
            {
                route: 'manager-search-flats',
            },
            {
                route: 'manager-flat-form',
                params: {
                    flatId: 'e47ee70238524841bcff89acb3356bce',
                },
            },
            {
                route: 'manager-flat-house-services',
                params: {
                    flatId: 'e47ee70238524841bcff89acb3356bce',
                },
            },
            {
                route: 'manager-flat-house-service-list',
                params: {
                    flatId: 'e47ee70238524841bcff89acb3356bce',
                },
            },
        ],
        current: {
            route: 'manager-flat-house-service-form',
        },
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
