import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IBreadcrumb } from 'types/breadcrumbs';

import { HouseServicesResponsibleForPayment, HouseServicesSettingsStatus } from 'app/libs/house-services/settings-form';

import { IUniversalStore } from 'view/modules/types';
import { initialState as fieldsInitialState } from 'view/modules/houseServicesSettingsForm/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/houseServicesSettingsForm/reducers/network';
import { houseServices } from 'view/libs/houseService';

import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';

const breadcrumbs: IBreadcrumbsStore = {
    crumbs: [
        {
            route: 'user-flat',
        } as IBreadcrumb,
    ],
    current: {
        route: 'owner-house-services-settings-preview',
        params: {
            flatId: '99ef4c3d93474534989837f1ae3bbb9c',
        },
    },
};

export const tenantFilledStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.TENANT,
            shouldSendReceiptPhotos: true,
            shouldSendReadings: true,
            shouldTenantRefund: true,
            tenantRefundPaymentsDescription: 'Коммуналка, гор вода',
            tenantRefundPaymentAmount: 3220,
            paidByTenantHouseServices: 'Интернет, ТВ',
            paymentConfirmation: true,
            settingsStatus: HouseServicesSettingsStatus.DRAFT,
            hasServicesPaidByTenant: true,
            paidByTenantAmount: 1660,
            paymentDetails: '4442 4244 2222 5252',
            paymentAmount: 5400,
        },
        houseServices,
    },
};

export const onlyContentStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    cookies: { ['only-content']: 'true' },
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.TENANT,
            shouldSendReceiptPhotos: true,
            shouldSendReadings: true,
            shouldTenantRefund: true,
            tenantRefundPaymentsDescription: 'Коммуналка, гор вода',
            tenantRefundPaymentAmount: 3220,
            paidByTenantHouseServices: 'Интернет, ТВ',
            paymentConfirmation: true,
            settingsStatus: HouseServicesSettingsStatus.DRAFT,
            hasServicesPaidByTenant: true,
            paidByTenantAmount: 1660,
            paymentDetails: '4442 4244 2222 5252',
            paymentAmount: 5400,
        },
        houseServices,
    },
};

export const tenantConfirmedFilledStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.TENANT,
            shouldSendReceiptPhotos: true,
            shouldSendReadings: true,
            shouldTenantRefund: true,
            tenantRefundPaymentsDescription: 'Коммуналка, гор вода',
            tenantRefundPaymentAmount: 3220,
            paidByTenantHouseServices: 'Интернет, ТВ',
            paymentConfirmation: true,
            settingsStatus: HouseServicesSettingsStatus.CONFIRMED_BY_TENANT,
            hasServicesPaidByTenant: true,
            paidByTenantAmount: 1660,
            paymentDetails: '4442 4244 2222 5252',
            paymentAmount: 5400,
        },
        houseServices,
    },
};

export const ownerMinFilledStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.OWNER,
            shouldSendReceiptPhotos: false,
            shouldSendReadings: false,
            shouldTenantRefund: false,
            paymentConfirmation: false,
            settingsStatus: HouseServicesSettingsStatus.DRAFT,
            hasServicesPaidByTenant: false,
        },
    },
};

export const ownerFilledStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.OWNER,
            shouldSendReceiptPhotos: true,
            shouldSendReadings: true,
            shouldTenantRefund: true,
            tenantRefundPaymentsDescription: 'Коммуналка, гор вода',
            tenantRefundPaymentAmount: 3220,
            paidByTenantHouseServices: 'Интернет, ТВ',
            paymentConfirmation: true,
            hasServicesPaidByTenant: true,
            paidByTenantAmount: 1660,
            paymentDetails: '4442 4244 2222 5252',
            paymentAmount: 5400,
            settingsStatus: HouseServicesSettingsStatus.DRAFT,
        },
        houseServices,
    },
};

export const withoutHouseServicesListStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.OWNER,
            shouldSendReceiptPhotos: true,
            shouldSendReadings: true,
            shouldTenantRefund: true,
            tenantRefundPaymentsDescription: 'Коммуналка, гор вода',
            tenantRefundPaymentAmount: 3220,
            paidByTenantHouseServices: 'Интернет, ТВ',
            paymentConfirmation: true,
            hasServicesPaidByTenant: true,
            paidByTenantAmount: 1660,
            paymentDetails: '4442 4244 2222 5252',
            paymentAmount: 5400,
            settingsStatus: HouseServicesSettingsStatus.DRAFT,
        },
        houseServices: [],
    },
};

export const ownerFullFilledStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.OWNER,
            shouldSendReceiptPhotos: true,
            shouldSendReadings: true,
            shouldTenantRefund: true,
            tenantRefundPaymentsDescription: 'Коммуналка, гор вода',
            tenantRefundPaymentAmount: 3220,
            paidByTenantHouseServices: 'Интернет, ТВ',
            paymentConfirmation: true,
            hasServicesPaidByTenant: true,
            paidByTenantAmount: 1660,
            paymentDetails: '4442 4244 2222 5252',
            paymentAmount: 5400,
            settingsStatus: HouseServicesSettingsStatus.FILLED_BY_OWNER,
        },
        houseServices,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    spa: {
        status: RequestStatus.PENDING,
    },
    houseServicesSettings: {
        settings: {
            responsibleForPayment: HouseServicesResponsibleForPayment.OWNER,
            shouldSendReceiptPhotos: true,
            shouldSendReadings: true,
            shouldTenantRefund: true,
            tenantRefundPaymentsDescription: 'Коммуналка, гор вода',
            tenantRefundPaymentAmount: 3220,
            paidByTenantHouseServices: 'Интернет, ТВ',
            paymentConfirmation: true,
            hasServicesPaidByTenant: true,
            paidByTenantAmount: 1660,
            paymentDetails: '4442 4244 2222 5252',
            paymentAmount: 5400,
            settingsStatus: HouseServicesSettingsStatus.DRAFT,
        },
    },
    houseServicesSettingsForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};
