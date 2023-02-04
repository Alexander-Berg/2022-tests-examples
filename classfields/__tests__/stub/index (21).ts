import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import {
    HouseServicesPeriodType,
    HouseServicesAggregatedMeterReadingsStatus,
    HouseServicesPeriodBillStatus,
    HouseServicesPeriodReceiptStatus,
    HouseServicesPeriodPaymentConfirmationStatus,
    HouseServicesPeriodId,
    HouseServicesMeterReadingsId,
} from 'types/houseServices';
import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';
import { HouseServiceMeterTariff, HouseServiceMeterType } from 'types/houseService';
import { HouseServicesMeterReadingsStatus } from 'types/houseServices';

import { ImageUploaderEntityId, ImageUploaderImageId } from 'types/imageUploader';

import { ImageUploaderEntityIds } from 'app/libs/house-services';

import { IUniversalStore } from 'view/modules/types';
import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';

const imageUrl = generateImageUrl({ width: 150, height: 150, size: 15 });

const breadcrumbs: IBreadcrumbsStore = {
    crumbs: [
        {
            route: 'user-flat',
            params: {},
        },
        {
            route: 'tenant-house-services-periods',
            params: { flatId: '123' },
        },
        {
            route: 'tenant-house-services-period',
            params: { flatId: '123', periodId: '83b0e6690ef4c6f65944d0b645bb47d1' },
        },
    ],
    current: {
        route: 'tenant-house-services-period-payment-confirmation',
        params: {},
    },
};

export const notSentStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.UNKNOWN,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.NOT_SENT,
                meter: {
                    type: HouseServiceMeterType.WATER_HOT,
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
                                        url: imageUrl,
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
        ],
    },
    imageUploader: {
        PAYMENT_CONFIRMATION: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};

export const shouldSentStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.UNKNOWN,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.NOT_SENT,
                meter: {
                    type: HouseServiceMeterType.WATER_HOT,
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
                                        url: imageUrl,
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
        ],
    },
    imageUploader: {
        PAYMENT_CONFIRMATION: {
            images: [],
            getImageUploaderUrlStatus: RequestStatus.LOADED,
        },
    },
};

export const sentStore: DeepPartial<IUniversalStore> = {
    ...notSentStore,
    houseServicesPeriod: {
        ...notSentStore.houseServicesPeriod,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
        paymentConfirmation: {
            photos: [
                {
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
                {
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
            ],
        },
    },
    imageUploader: {
        PAYMENT_CONFIRMATION: {
            images: [
                {
                    entityId: ImageUploaderEntityIds.PAYMENT_CONFIRMATION as ImageUploaderEntityId,
                    imageId: 'b1db5a44-028b-49e6-a327-3dbc7920c073' as ImageUploaderImageId,
                    previewUrl: imageUrl,
                    largeUrl: imageUrl,
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
                {
                    entityId: ImageUploaderEntityIds.PAYMENT_CONFIRMATION as ImageUploaderEntityId,
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
    },
};

export const declinedStore: DeepPartial<IUniversalStore> = {
    ...notSentStore,
    houseServicesPeriod: {
        ...notSentStore.houseServicesPeriod,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.DECLINED,
        paymentConfirmation: {
            photos: [
                {
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
                {
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
            ],
            reasonForDecline: 'Это счета за прошлый месяц',
        },
    },
    imageUploader: {
        PAYMENT_CONFIRMATION: {
            images: [
                {
                    entityId: ImageUploaderEntityIds.PAYMENT_CONFIRMATION as ImageUploaderEntityId,
                    imageId: 'b1db5a44-028b-49e6-a327-3dbc7920c073' as ImageUploaderImageId,
                    previewUrl: imageUrl,
                    largeUrl: imageUrl,
                    uploaderData: {
                        groupId: 1396625,
                        name: '9f4107752ebb0b58713ca84dacde044f',
                        namespace: 'arenda' as ImageNamespaces.ARENDA,
                    },
                },
                {
                    entityId: ImageUploaderEntityIds.PAYMENT_CONFIRMATION as ImageUploaderEntityId,
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
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...notSentStore,
    spa: {
        status: RequestStatus.PENDING,
    },
};
