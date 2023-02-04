import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { ImageUploaderEntityIds } from 'app/libs/house-services';

import { IUniversalStore } from 'view/modules/types';
import { ImageUploaderEntityId, ImageUploaderImageId } from 'types/imageUploader';
import { HouseServicesMeterReadingsStatus, HouseServicesMeterReadingsId } from 'types/houseServices';
import { HouseServiceMeterTariff, HouseServiceMeterType } from 'types/houseService';
import { Fields } from 'view/modules/houseServicesMeterReadingsForm/types';
import { initialState } from 'view/modules/houseServicesMeterReadingsForm/reducers/fields';
import { IImage, ImageNamespaces } from 'types/image';

export const storeSingle: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
            periodId: '76576576',
            meterReadingsId: '12313',
        },
    },
    houseServicesMeterReadings: {
        meterReadingsId: '12313' as HouseServicesMeterReadingsId,
        status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
        meter: {
            type: HouseServiceMeterType.WATER_COLD,
            number: '123123',
            installedPlace: 'В ванной',
            deliverFromDay: 20,
            deliverToDay: 25,
            tariff: HouseServiceMeterTariff.SINGLE,
            initialMeterReadings: [],
        },
        meterReadings: [],
        previousMeterReadings: [
            {
                meterValue: 123,
                meterPhoto: {} as IImage,
            },
        ],
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

export const storeDouble: DeepPartial<IUniversalStore> = {
    ...storeSingle,
    houseServicesMeterReadings: {
        meterReadingsId: '562623435' as HouseServicesMeterReadingsId,
        status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
        meter: {
            type: HouseServiceMeterType.POWER,
            number: '64352345',
            installedPlace: 'В коридоре',
            deliverFromDay: 20,
            deliverToDay: 25,
            tariff: HouseServiceMeterTariff.DOUBLE,
            initialMeterReadings: [],
        },
        meterReadings: [],
        previousMeterReadings: [
            {
                meterValue: 123,
                meterPhoto: {} as IImage,
            },
            {
                meterValue: 352,
                meterPhoto: {} as IImage,
            },
        ],
    },
    page: {
        params: {
            ...storeSingle.page?.params,
            meterReadingsId: '562623435',
        },
    },
};

export const storeTriple: DeepPartial<IUniversalStore> = {
    ...storeSingle,
    houseServicesMeterReadings: {
        meterReadingsId: '867373365' as HouseServicesMeterReadingsId,
        status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
        meter: {
            type: HouseServiceMeterType.HEATING,
            number: '4362345',
            installedPlace: 'На кухне',
            deliverFromDay: 20,
            deliverToDay: 25,
            tariff: HouseServiceMeterTariff.TRIPLE,
            initialMeterReadings: [],
        },
        meterReadings: [],
        previousMeterReadings: [
            {
                meterValue: 123,
                meterPhoto: {} as IImage,
            },
            {
                meterValue: 352,
                meterPhoto: {} as IImage,
            },
            {
                meterValue: 323,
                meterPhoto: {} as IImage,
            },
        ],
    },
    page: {
        params: {
            ...storeSingle.page?.params,
            meterReadingsId: '867373365',
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...storeSingle,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...storeTriple,
    config: {
        isMobile: 'yes',
    },
};

export const filledStore: DeepPartial<IUniversalStore> = {
    ...storeSingle,
    houseServicesMeterReadingsForm: {
        network: {
            sendHouseServicesMeterReadingsForm: RequestStatus.LOADED,
        },
        fields: {
            ...initialState,
            [Fields.COUNTER_VALUE_0]: {
                id: Fields.COUNTER_VALUE_0,
                value: 100,
            },
        },
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
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
        [ImageUploaderEntityIds.T3 as ImageUploaderEntityId]: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};
