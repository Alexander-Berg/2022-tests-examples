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

import { IUniversalStore } from 'view/modules/types';
import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';

const imageUrl = generateImageUrl({ width: 1000, height: 1000, size: 10 });

const imageUrls = [
    {
        alias: 'orig' as DefaultNamespaceAliases,
        url: imageUrl,
    },
    {
        alias: '64x64' as DefaultNamespaceAliases,
        url: imageUrl,
    },
    {
        alias: '128x128' as DefaultNamespaceAliases,
        url: imageUrl,
    },
    {
        alias: '280x210' as DefaultNamespaceAliases,
        url: imageUrl,
    },
    {
        alias: '560x420' as DefaultNamespaceAliases,
        url: imageUrl,
    },
    {
        alias: '1024x1024' as DefaultNamespaceAliases,
        url: imageUrl,
    },
];

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
    ],
    current: {
        route: 'tenant-house-services-period',
        params: {},
    },
};

export const shouldBeSentStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SHOULD_BE_SENT,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.UNKNOWN,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.UNKNOWN,
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
                                imageUrls: imageUrls,
                            },
                        },
                    ],
                },
            },
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
                                imageUrls: imageUrls,
                            },
                        },
                    ],
                },
            },
        ],
    },
};

export const sentStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.UNKNOWN,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.UNKNOWN,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 125 }],
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
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 852 }],
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
};

export const declinedMetricStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.DECLINED,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.UNKNOWN,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.UNKNOWN,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.DECLINED,
                meterReadings: [{ meterValue: 125 }],
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
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 852 }],
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
};

export const declinedReceiptStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 125 }],
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
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 852 }],
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
};

export const declinedConfirmationStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.DECLINED,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 125 }],
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
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 852 }],
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
};

export const declinedSeveralDataStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.DECLINED,
        billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
        receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.DECLINED,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.DECLINED,
                meterReadings: [{ meterValue: 125 }],
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
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 852 }],
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
};

export const shouldPayBillStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_PAID,
        receiptStatus: HouseServicesPeriodReceiptStatus.SHOULD_BE_SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
        bill: {
            amount: 741800,
            comment: 'Придётся заплатить',
            photos: [
                {
                    namespace: 'arenda' as ImageNamespaces.ARENDA,
                    groupId: 1396625,
                    name: '9f4107752ebb0b58713ca84dacde044f',
                    imageUrls: imageUrls,
                },
                {
                    namespace: 'arenda' as ImageNamespaces.ARENDA,
                    groupId: 1396625,
                    name: '9f4107752ebb0b58713ca84dacde044c',
                    imageUrls: imageUrls,
                },
            ],
        },
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 125 }],
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
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 852 }],
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
};

export const billPayedStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs,
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.PAID,
        receiptStatus: HouseServicesPeriodReceiptStatus.SHOULD_BE_SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
        bill: {
            amount: 741800,
            comment: 'Придётся заплатить',
        },
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 125 }],
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
            {
                meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [{ meterValue: 852 }],
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '12345',
                    installedPlace: 'в коридоре',
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
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...shouldBeSentStore,
    spa: {
        status: RequestStatus.PENDING,
    },
};
