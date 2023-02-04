import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import {
    HouseServicesPeriodType,
    HouseServicesAggregatedMeterReadingsStatus,
    HouseServicesPeriodBillStatus,
    HouseServicesPeriodReceiptStatus,
    HouseServicesPeriodPaymentConfirmationStatus,
    HouseServicesPeriodId,
    HouseServicesMeterReadingsId,
    IHouseServicesMeterReadings,
    IHouseServicesPeriodWithInfo,
} from 'types/houseServices';
import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';
import { HouseServiceMeterTariff, HouseServiceMeterType } from 'types/houseService';
import { HouseServicesMeterReadingsStatus } from 'types/houseServices';

const imageUrl = generateImageUrl({ width: 1000, height: 1000, size: 10 });

const emptyFns = {
    onHouseServiceClick: () => {
        return;
    },
    onPaymentConfirmationClick: () => {
        return;
    },
    onReceiptsClick: () => {
        return;
    },
};

const hotWaterCounterMetric: IHouseServicesMeterReadings = {
    meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
    status: HouseServicesMeterReadingsStatus.NOT_SENT,
    meterReadings: [
        {
            meterValue: 594,
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
};

const coldWaterCounterMetric: IHouseServicesMeterReadings = {
    meterReadingsId: '50f507891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
    status: HouseServicesMeterReadingsStatus.NOT_SENT,
    meterReadings: [
        {
            meterValue: 895,
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
                meterValue: 852,
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

const powerCounterMetric: IHouseServicesMeterReadings = {
    meterReadingsId: '50f5072891b19219a74cb8081864a3406' as HouseServicesMeterReadingsId,
    status: HouseServicesMeterReadingsStatus.NOT_SENT,
    meterReadings: [
        {
            meterValue: 595,
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
            meterValue: 436,
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
        number: '12345',
        installedPlace: 'в коридоре',
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
                meterValue: 425,
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

const houseServicesPeriod: IHouseServicesPeriodWithInfo = {
    periodId: '83b0e6690ef4c6f65944d0b645bb47d1' as HouseServicesPeriodId,
    period: '2021-09',
    periodType: HouseServicesPeriodType.REGULAR,
    meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.UNKNOWN,
    billStatus: HouseServicesPeriodBillStatus.UNKNOWN,
    receiptStatus: HouseServicesPeriodReceiptStatus.UNKNOWN,
    confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.UNKNOWN,
    meterReadings: [hotWaterCounterMetric, coldWaterCounterMetric, powerCounterMetric],
};

export const notSentState = {
    ...emptyFns,
    houseServicesPeriod,
};

export const shouldBeSentState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SHOULD_BE_SENT,
        meterReadings: [
            {
                ...hotWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
            },
            {
                ...coldWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
            },
            {
                ...powerCounterMetric,
                status: HouseServicesMeterReadingsStatus.SHOULD_BE_SENT,
            },
        ],
    },
};

export const toSentState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        meterReadings: [
            {
                ...hotWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENDING,
            },
            {
                ...coldWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENDING,
            },
            {
                ...powerCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENDING,
            },
        ],
    },
};

export const sentState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        meterReadings: [
            {
                ...hotWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENT,
            },
            {
                ...coldWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENT,
            },
            {
                ...powerCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENT,
            },
        ],
    },
};

export const declinedState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.DECLINED,
        meterReadings: [
            {
                ...hotWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.DECLINED,
            },
            {
                ...coldWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.DECLINED,
            },
            {
                ...powerCounterMetric,
                status: HouseServicesMeterReadingsStatus.DECLINED,
            },
        ],
    },
};

export const expiredState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.EXPIRED,
        meterReadings: [
            {
                ...hotWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.EXPIRED,
            },
            {
                ...coldWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.EXPIRED,
            },
            {
                ...powerCounterMetric,
                status: HouseServicesMeterReadingsStatus.EXPIRED,
            },
        ],
    },
};

export const withDocsState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
        receiptStatus: HouseServicesPeriodReceiptStatus.NOT_SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
        meterReadings: [
            {
                ...hotWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENT,
            },
            {
                ...coldWaterCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENT,
            },
            {
                ...powerCounterMetric,
                status: HouseServicesMeterReadingsStatus.SENT,
            },
        ],
    },
};

export const docsCanBeSentState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
        receiptStatus: HouseServicesPeriodReceiptStatus.CAN_BE_SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.CAN_BE_SENT,
        meterReadings: [],
    },
};

export const docsShouldBeSentState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
        receiptStatus: HouseServicesPeriodReceiptStatus.SHOULD_BE_SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
        meterReadings: [],
    },
};

export const docsSentState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
        receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
        meterReadings: [],
    },
};

export const docsDeclinedState = {
    ...emptyFns,
    houseServicesPeriod: {
        ...houseServicesPeriod,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.NOT_SENT,
        receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.DECLINED,
        meterReadings: [],
    },
};
