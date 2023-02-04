import merge from 'lodash/merge';

import { RequestStatus } from 'realty-core/types/network';

import { IEgrnReportsStore } from 'view/common/reducers/egrn-reports';
import { IStore } from 'view/common/reducers';

import { initialStoreReports, withReports } from './reports';
import initialStore from './store';

interface IGetStoreOptions {
    reportsOverrides?: Partial<IEgrnReportsStore>;
    storeOverrides?: Partial<IStore>;
}

const getStore = (options?: IGetStoreOptions): Partial<IStore> => {
    const { reportsOverrides, storeOverrides } = options || {};
    const reportsStore = merge({}, initialStoreReports, reportsOverrides);
    const store = merge({}, initialStore, { egrnReports: reportsStore }, storeOverrides);

    return store;
};

export default {
    default: getStore(),
    withReports: getStore({
        reportsOverrides: {
            // @ts-ignore
            reports: withReports,
        },
    }),
    error: getStore({ reportsOverrides: { network: { pageStatus: RequestStatus.FAILED } } }),
};
