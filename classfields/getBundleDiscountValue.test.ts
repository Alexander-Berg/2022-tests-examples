import type { StateVinReportData } from 'auto-core/react/dataDomain/vinReport/types';

import getBundleDiscountValue from './getBundleDiscountValue';

describe('getBundleDiscountValue верно высчитывает', () => {
    it('большую скидку', () => {
        const report = {
            billing: {
                service_prices: [ {
                    price: 100,
                    counter: '1',
                }, {
                    price: 1000,
                    counter: '20',
                } ],
            },
        };
        const result = getBundleDiscountValue(report as StateVinReportData);
        expect(result).toEqual(50);
    });

    it('отсутствие скидки, если она несущественна (меньше 20%)', () => {
        const report = {
            billing: {
                service_prices: [ {
                    price: 100,
                    counter: '1',
                }, {
                    price: 900,
                    counter: '10',
                } ],
            },
        };
        const result = getBundleDiscountValue(report as StateVinReportData);
        expect(result).toBeNull();
    });
});
