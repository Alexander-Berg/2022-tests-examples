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

export const metricSendStore: DeepPartial<IUniversalStore> = {
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
            settingsStatus: HouseServicesSettingsStatus.FILLED_BY_OWNER,
        },
    },
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.NOT_SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
            },
            {
                periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
                period: '2020-07',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
            },
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

export const paymentConfirmationSendStore: DeepPartial<IUniversalStore> = {
    ...metricSendStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.NOT_SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
            },
            {
                periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
                period: '2020-07',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
            },
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

export const receiptSendStore: DeepPartial<IUniversalStore> = {
    ...metricSendStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.NOT_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
            },
            {
                periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
                period: '2020-07',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
            },
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

export const shouldSendBillStore: DeepPartial<IUniversalStore> = {
    ...metricSendStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_SENT,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
            },
            {
                periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
                period: '2020-07',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
            },
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

export const shouldPaidBillStore: DeepPartial<IUniversalStore> = {
    ...metricSendStore,
    houseServicesPeriods: {
        periods: [
            {
                periodId: 'f2b53938d9aaa5e5dbeac48aca2c379d' as HouseServicesPeriodId,
                period: '2021-08',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.SHOULD_BE_PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.NOT_SENT,
            },
            {
                periodId: 'dafd43e398c858fe1d54d22f1bf7ba46' as HouseServicesPeriodId,
                period: '2020-07',
                periodType: HouseServicesPeriodType.REGULAR,
                meterReadingsStatus: HouseServicesAggregatedMeterReadingsStatus.SENT,
                billStatus: HouseServicesPeriodBillStatus.PAID,
                receiptStatus: HouseServicesPeriodReceiptStatus.SENT,
                confirmationStatus: HouseServicesPeriodPaymentConfirmationStatus.SENT,
            },
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
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {},
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
    ...metricSendStore,
    spa: {
        status: RequestStatus.PENDING,
        route: 'owner-house-services-periods',
    },
};
