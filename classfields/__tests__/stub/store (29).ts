import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import {
    HouseServicesPeriodType,
    HouseServicesAggregatedMeterReadingsStatus,
    HouseServicesPeriodBillStatus,
    HouseServicesPeriodReceiptStatus,
    HouseServicesPeriodPaymentConfirmationStatus,
    HouseServicesPeriodId,
} from 'types/houseServices';

import { HouseServicesSettingsStatus } from 'app/libs/house-services/settings-form';

import { IUniversalStore } from 'view/modules/types';

const previousPeriods = [
    {
        periodId: '34a6cb9a71eb4d5dedaf57e1fb813463' as HouseServicesPeriodId,
        period: '2021-07',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.PAID,
        receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
    },
    {
        periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
        period: '2020-05',
        periodType: HouseServicesPeriodType.REGULAR,
        meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
        billStatus: HouseServicesPeriodBillStatus.PAID,
        receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
        confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
    },
];

export const canSendMetricsStore: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    breadcrumbs: {
        crumbs: [
            {
                route: 'user-flat',
            },
        ],
        current: {
            route: 'owner-house-services-periods',
        },
    },
    houseServicesSettings: {
        settings: {
            settingsStatus: HouseServicesSettingsStatus.CONFIRMED_BY_TENANT,
        },
    },
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SHOULD_BE_SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.NOT_SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const sholdPayStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.CAN_BE_SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const declinedReceiptStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const declinedConfirmationStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const declinedReceiptAndConfirmationStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.DECLINED,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.DECLINED,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const shouldSendReceiptStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.SHOULD_BE_SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.CAN_BE_SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const shouldSendConfirmationStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.CAN_BE_SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const shouldSendAllDataStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SHOULD_BE_SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.SHOULD_BE_SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SHOULD_BE_SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const withoutNotificationStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
            },
            ...previousPeriods,
        ],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 3,
            pageCount: 1,
        },
    },
};

export const emptyStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    houseServicesPeriods: {
        periods: [],
        paging: {
            page: {
                num: 1,
                size: 100,
            },
            total: 1,
            pageCount: 1,
        },
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...canSendMetricsStore,
    spa: {
        status: RequestStatus.PENDING,
        route: 'owner-house-services-periods',
    },
};
