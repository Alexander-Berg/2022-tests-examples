import type { StateVinReportData } from 'auto-core/react/dataDomain/vinReport/types';

import vinReportBillingMock from '../mocks/vinReportBillingMock';
import vinReportBillingMocWithDiscount from '../mocks/vinReportBillingMocWithDiscount';

import getMaxBundleDiscountValue from './getMaxBundleDiscountValue';

it('вернет undefined, если скидки нет', () => {
    const report = {
        billing: vinReportBillingMock,
    };
    const result = getMaxBundleDiscountValue(report as unknown as StateVinReportData);
    expect(result).toBeUndefined();
});

it('вернет скидку в %, когда она есть', () => {
    const report = {
        billing: vinReportBillingMocWithDiscount,
    };
    const result = getMaxBundleDiscountValue(report as unknown as StateVinReportData);
    expect(result).toEqual(50);
});
