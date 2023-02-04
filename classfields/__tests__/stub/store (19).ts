import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { IBreadcrumb } from 'types/breadcrumbs';

import { IUniversalStore } from 'view/modules/types';
import { initialState as fieldsInitialState } from 'view/modules/houseServicesSettingsForm/reducers/fields';
import { initialState as networkInitialState } from 'view/modules/houseServicesSettingsForm/reducers/network';
import { IBreadcrumbsStore } from 'view/modules/breadcrumbs/reducers';

const breadcrumbs: IBreadcrumbsStore = {
    crumbs: [
        {
            route: 'user-flat',
        } as IBreadcrumb,
        {
            route: 'owner-house-services-settings-preview',
            params: {
                flatId: '99ef4c3d93474534989837f1ae3bbb9c',
            },
        },
    ],
    current: {
        route: 'owner-house-services-settings-form',
        params: {
            flatId: '99ef4c3d93474534989837f1ae3bbb9c',
        },
    },
};

export const store: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettingsForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};

export const onlyContentStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    cookies: { ['only-content']: 'true' },
    page: { params: {} },
    spa: {
        status: RequestStatus.LOADED,
    },
    houseServicesSettingsForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    breadcrumbs,
    page: { params: {} },
    spa: {
        status: RequestStatus.PENDING,
    },
    houseServicesSettingsForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};
