import { DeepPartial } from 'utility-types';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { RequestStatus } from 'realty-core/types/network';

import { HouseServiceId } from 'types/houseService';

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

const imageUrl = generateImageUrl({ width: 1000, height: 1000, size: 10 });

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
            {
                route: 'tenant-house-services-settings-preview',
            },
        ],
        current: {
            route: 'tenant-house-service',
        },
    },
    houseService: {
        houseServiceId: 'aa8e18d590494f7480ae49d059692c24' as HouseServiceId,
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
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...store,
    config: { isMobile: 'iOS' },
};

export const onlyContentStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
            {
                route: 'tenant-house-services-settings-preview',
            },
        ],
        current: {
            route: 'tenant-house-service',
        },
    },
    houseService: {
        houseServiceId: 'aa8e18d590494f7480ae49d059692c24' as HouseServiceId,
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
    cookies: {
        ['only-content']: '1',
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.PENDING,
    },
    page: { params: { flatId: '123' } },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
            {
                route: 'tenant-house-services-settings-preview',
            },
        ],
        current: {
            route: 'tenant-house-service',
        },
    },
    houseServicesPeriod: {
        periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
        period: '2021-09',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_PAID,
        receiptStatus: HouseServicesPeriodReceiptStatus.CAN_BE_SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
        meterReadings: [
            {
                meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
                status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
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
                status: HouseServicesMeterReadingsStatus.SENDING,
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
