import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { ImageUploaderEntityId, ImageUploaderImageId, ImageUploaderImageStatus } from 'types/imageUploader';

import { ImageNamespaces, DefaultNamespaceAliases, ArendaNamespaceAliasesBase } from 'types/image';

import { HouseServiceMeterTariff, HouseServiceMeterType, HouseServiceId, HouseServiceType } from 'types/houseService';

import { getFields } from 'app/libs/house-services/house-service-form';
import { ImageUploaderEntityIds } from 'app/libs/house-services';

import { IUniversalStore } from 'view/modules/types';
import { Fields } from 'view/modules/houseServiceForm/types';
import { initialState } from 'view/modules/houseServiceForm/reducers/fields';

const houseService = {
    houseServiceId: 'qwerty' as HouseServiceId,
    [HouseServiceType.METER]: {
        type: HouseServiceMeterType.POWER,
        number: '12345',
        installedPlace: 'В ванной',
        deliverFromDay: 10,
        deliverToDay: 15,
        tariff: HouseServiceMeterTariff.SINGLE,
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
                            url: '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/orig',
                        },
                        {
                            alias: '64x64' as ArendaNamespaceAliasesBase,
                            url: '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/64x64',
                        },
                        {
                            alias: '128x128' as ArendaNamespaceAliasesBase,
                            url:
                                '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/128x128',
                        },
                        {
                            alias: '280x210' as ArendaNamespaceAliasesBase,
                            url:
                                '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/280x210',
                        },
                        {
                            alias: '560x420' as ArendaNamespaceAliasesBase,
                            url:
                                '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/560x420',
                        },
                        {
                            alias: '1024x1024' as ArendaNamespaceAliasesBase,
                            url:
                                // eslint-disable-next-line max-len
                                '//avatars.mdst.yandex.net/get-arenda/1396625/9f4107752ebb0b58713ca84dacde044f/1024x1024',
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
    config: {
        isMobile: '',
    },
};

export const storeMobile: DeepPartial<IUniversalStore> = {
    ...store,
    config: {
        isMobile: 'iPhone',
    },
};

export const storeDoubleTariff: DeepPartial<IUniversalStore> = {
    ...store,
    houseServiceForm: {
        fields: {
            ...initialState,
            [Fields.TARIFF]: {
                id: Fields.TARIFF,
                value: HouseServiceMeterTariff.DOUBLE,
            },
        },
    },
};

export const storeTripleTariff: DeepPartial<IUniversalStore> = {
    ...store,
    houseServiceForm: {
        fields: {
            ...initialState,
            [Fields.TARIFF]: {
                id: Fields.TARIFF,
                value: HouseServiceMeterTariff.TRIPLE,
            },
        },
    },
};

export const filledStore: DeepPartial<IUniversalStore> = {
    ...store,
    page: {
        params: {
            flatId: '12345',
            houseServiceId: '12345',
        },
    },
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
                    status: ImageUploaderImageStatus.LOADED,
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
            images: [],
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
