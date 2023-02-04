import { AnyObject } from 'realty-core/types/utils';
import { RequestStatus } from 'realty-core/types/network';

import { IStore } from 'view/common/reducers';

const initialState = {
    tariff: {
        status: RequestStatus.INITIAL,
    },
} as IStore;

const getState = (overrides: AnyObject) => {
    return { ...initialState, tariff: { ...initialState.tariff, ...overrides } } as IStore;
};

export const mocks = {
    error: initialState,
    callsMinimum: getState({ tariffType: { callsMinimum: {} } }),
    listingMaximum: getState({ tariffType: { listingMaximum: {} } }),
    pending1: getState({
        status: RequestStatus.PENDING,
        tariffType: { callsExtended: {} },
        plannedSwitchTime: '2022-06-30T07:00:00Z',
        plannedTariffType: { callsExtended: {} },
    }),
    pending2: getState({ status: RequestStatus.PENDING, tariffType: { callsExtended: {} } }),
    plannedTime: getState({
        tariffType: { callsMaximum: {} },
        plannedSwitchTime: '2022-06-30T07:00:00Z',
        plannedTariffType: { callsExtended: {} },
    }),
    zeroPlannedTime: getState({
        tariffType: { callsExtended: {} },
        plannedSwitchTime: '1970-01-01T00:00:00Z',
        plannedTariffType: { callsMaximum: {} },
    }),
    callsMaximum: getState({ tariffType: { callsMaximum: {} } }),
};
