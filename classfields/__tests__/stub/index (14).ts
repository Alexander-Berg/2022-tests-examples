import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { initialState as fieldsInitialState } from 'view/modules/houseServicesMeterReadingsDeclineForm/reducers/fields';
// eslint-disable-next-line max-len
import { initialState as networkInitialState } from 'view/modules/houseServicesMeterReadingsDeclineForm/reducers/network';
import { IUniversalStore } from 'view/modules/types';

export const store: DeepPartial<IUniversalStore> = {
    spa: {
        status: RequestStatus.LOADED,
    },
    page: {
        params: {
            flatId: '12345',
            periodId: '76576576',
            meterReadingsId: '987654321',
        },
    },
    breadcrumbs: {
        current: {
            route: 'owner-house-services-period-meter-readings-decline',
            params: {
                flatId: '12345',
                periodId: '76576576',
                meterReadingsId: '987654321',
            },
        },
        crumbs: [
            { route: 'user-flat' },
            {
                route: 'owner-house-services-period-meter-readings-preview',
                params: {
                    flatId: '12345',
                    periodId: '76576576',
                    meterReadingsId: '987654321',
                },
            },
        ],
    },
    houseServicesMeterReadingsDeclineForm: {
        fields: fieldsInitialState,
        network: networkInitialState,
    },
};

export const skeletonStore: DeepPartial<IUniversalStore> = {
    ...store,
    spa: {
        status: RequestStatus.PENDING,
    },
};

export const mobileStore: DeepPartial<IUniversalStore> = {
    ...store,
    config: { isMobile: 'iPhone' },
};
