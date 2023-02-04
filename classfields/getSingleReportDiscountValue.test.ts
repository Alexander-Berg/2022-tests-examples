import type { StateVinReportData } from 'auto-core/react/dataDomain/vinReport/types';

import vinReportBillingMocWithDiscount from '../mocks/vinReportBillingMocWithDiscount';
import vinReportBillingMock from '../mocks/vinReportBillingMock';

import getSingleReportDiscountValue from './getSingleReportDiscountValue';

describe('getReportDiscountValue верно высчитывает', () => {
    it('большую скидку', () => {
        const report = {
            billing: vinReportBillingMocWithDiscount,
        };
        const result = getSingleReportDiscountValue(report as unknown as StateVinReportData);
        expect(result).toEqual(50);
    });

    it('возвращает undefined, если скидки нет', () => {
        const report = {
            billing: vinReportBillingMock,
        };
        const result = getSingleReportDiscountValue(report as unknown as StateVinReportData);
        expect(result).toBeUndefined();
    });
});
