import * as creditFilterChangeModule from 'auto-core/react/dataDomain/credit/actions/creditFilterChange/creditFilterChange';

import { getPriceFrom } from './getPriceFrom';

describe('getPriceFrom', () => {
    it('вернет priceFrom как есть, если передан', () => {
        const priceFrom = 100;

        const result = getPriceFrom({
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            minProductAmount: 0,
            minRate: 0,
            priceFrom,
            term: 0,
        });

        expect(result).toEqual(priceFrom);
    });

    it('вернет minProductAmount если creditPaymentFrom = 0', () => {
        const minProductAmount = 100;
        const result = getPriceFrom({
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            minProductAmount,
            minRate: 0,
            priceFrom: 0,
            term: 0,
        });

        expect(result).toEqual(minProductAmount);
    });

    it('вернет minProductAmount если creditPaymentTo < creditPaymentFrom', () => {
        const minProductAmount = 100;
        const creditPaymentTo = 100;
        const creditPaymentFrom = 200;

        const result = getPriceFrom({
            creditPaymentFrom,
            creditPaymentTo,
            minProductAmount,
            minRate: 0,
            priceFrom: 0,
            term: 0,
        });

        expect(result).toEqual(minProductAmount);
    });

    it('вызовет getPrice(...) если priceFrom и creditPaymentFrom переданы как ноль', () => {
        const creditPaymentFrom = 100;
        const minRate = 12;
        const term = 11;

        const getPriceSpy = jest.spyOn(creditFilterChangeModule, 'getPrice');

        getPriceFrom({
            creditPaymentFrom,
            creditPaymentTo: 120,
            minProductAmount: 0,
            minRate,
            priceFrom: 0,
            term,
        });

        expect(getPriceSpy).toHaveBeenCalledWith(creditPaymentFrom, minRate, term, 0);
    });
});
