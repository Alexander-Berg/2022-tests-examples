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
} from 'types/houseServices';
import { ImageNamespaces, DefaultNamespaceAliases } from 'types/image';

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
            route: 'owner-house-services-periods',
            params: { flatId: '123' },
        },
        {
            route: 'owner-house-services-period',
            params: { flatId: '123', periodId: '83b0e6690ef4c6f65944d0b645bb47d1' },
        },
    ],
    current: {
        route: 'owner-house-services-period-payment-confirmation',
        params: {},
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
};

export const declinedStore: DeepPartial<IUniversalStore> = {
    ...sentStore,
    houseServicesPeriod: {
        ...sentStore.houseServicesPeriod,
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
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...sentStore,
    spa: {
        status: RequestStatus.PENDING,
        route: 'owner-house-services-period-payment-confirmation',
    },
};
