import _ from 'lodash';

import reportsBundlesWithDiscount from 'auto-core/react/dataDomain/reportsBundles/mocks/reportsBundlesWithDiscount.mock.json';

import type { TReportsBundle, TReportsBundleDiscount } from 'auto-core/types/TBilling';

import getReportsBundleOnSale from './getReportsBundleOnSale';

let REPORTS_BUNDLES: Array<TReportsBundle> = [];

beforeEach(() => {
    REPORTS_BUNDLES = _.cloneDeep(reportsBundlesWithDiscount) as Array<TReportsBundle>;
});

it('должен вернуть ничего, так как нет скидки', () => {
    const state = { reportsBundles: { data: REPORTS_BUNDLES } };
    const result = getReportsBundleOnSale(state);
    expect(result).toBeUndefined();
});

it('должен вернуть самый большой бандл на скидке, 1 из 2', () => {
    REPORTS_BUNDLES[1].price.modifier = {
        discount: {
            discount: 30,
        } as TReportsBundleDiscount,
    };
    REPORTS_BUNDLES[2].price.modifier = {
        discount: {
            discount: 29,
        } as TReportsBundleDiscount,
    };
    const state = { reportsBundles: { data: REPORTS_BUNDLES } };
    const result = getReportsBundleOnSale(state);
    expect(result).toEqual(REPORTS_BUNDLES[2]);
});

it('должен вернуть самый большой бандл на скидке, 1 из 1', () => {
    REPORTS_BUNDLES[1].price.modifier = {
        discount: {
            discount: 30,
        } as TReportsBundleDiscount,
    };
    const state = { reportsBundles: { data: REPORTS_BUNDLES } };
    const result = getReportsBundleOnSale(state);
    expect(result).toEqual(REPORTS_BUNDLES[1]);
});
