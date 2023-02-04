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
    IHouseServicesPeriodWithInfo,
} from 'types/houseServices';
import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';
import { HouseServiceMeterTariff, HouseServiceMeterType } from 'types/houseService';
import { HouseServicesMeterReadingsStatus } from 'types/houseServices';

import { IUniversalStore } from 'view/modules/types';

const imageUrl = generateImageUrl({ width: 1000, height: 1000, size: 10 });

const houseServicesPeriod: IHouseServicesPeriodWithInfo = {
    periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
    period: '2021-09',
    periodType: HouseServicesPeriodType.REGULAR,
    meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
    billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
    receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
    confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
    meterReadings: [
        {
            meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
            status: HouseServicesMeterReadingsStatus.SENT,
            meterReadings: [
                {
                    meterValue: 596,
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
            meterReadingsId: '50f507891b19219a74cb8081864a3407' as HouseServicesMeterReadingsId,
            status: HouseServicesMeterReadingsStatus.SENT,
            meterReadings: [
                {
                    meterValue: 1652,
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
            meter: {
                type: HouseServiceMeterType.WATER_COLD,
                number: '12345',
                installedPlace: 'В ванной',
                deliverFromDay: 10,
                deliverToDay: 15,
                tariff: HouseServiceMeterTariff.SINGLE,
                initialMeterReadings: [
                    {
                        meterValue: 1565,
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
            status: HouseServicesMeterReadingsStatus.SENDING,
            meterReadings: [
                {
                    meterValue: 585,
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
                    meterValue: 352,
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
            meter: {
                type: HouseServiceMeterType.POWER,
                number: '82345',
                installedPlace: 'В коридоре',
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
                        meterValue: 341,
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
};

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
            {
                route: 'owner-house-services-periods',
                params: { flatId: '123' },
            },
        ],
        current: {
            route: 'owner-house-services-period',
        },
    },
    houseServicesPeriod,
};

export const shouldSendBillStore: DeepPartial<IUniversalStore> = {
    ...store,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_SENT,
    },
};

export const declinedBillStore: DeepPartial<IUniversalStore> = {
    ...store,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        billStatus: HouseServicesPeriodBillStatus.DECLINED,
    },
};

export const sentBillStore: DeepPartial<IUniversalStore> = {
    ...store,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_PAID,
        bill: {
            amount: 741800,
            comment: 'Придётся заплатить',
        },
    },
};

export const paidBillStore: DeepPartial<IUniversalStore> = {
    ...store,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        billStatus: HouseServicesPeriodBillStatus.PAID,
        bill: {
            amount: 741800,
            comment: 'Придётся заплатить',
        },
    },
};

export const declinedMetricStore: DeepPartial<IUniversalStore> = {
    ...store,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.DECLINED,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.DECLINED,
                meterReadings: [
                    {
                        meterValue: 596,
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
                meterReadingsId: '50f507891b19219a74cb8081864a3407' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SENT,
                meterReadings: [
                    {
                        meterValue: 1652,
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
                meter: {
                    type: HouseServiceMeterType.WATER_COLD,
                    number: '12345',
                    installedPlace: 'В ванной',
                    deliverFromDay: 10,
                    deliverToDay: 15,
                    tariff: HouseServiceMeterTariff.SINGLE,
                    initialMeterReadings: [
                        {
                            meterValue: 1565,
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
                status: HouseServicesMeterReadingsStatus.DECLINED,
                meterReadings: [
                    {
                        meterValue: 585,
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
                        meterValue: 352,
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
                meter: {
                    type: HouseServiceMeterType.POWER,
                    number: '82345',
                    installedPlace: 'В коридоре',
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
                            meterValue: 341,
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
    ...store,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
    },
};

export const declinedConfirmationStore: DeepPartial<IUniversalStore> = {
    ...store,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.DECLINED,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...store,
    spa: {
        status: RequestStatus.PENDING,
    },
};
