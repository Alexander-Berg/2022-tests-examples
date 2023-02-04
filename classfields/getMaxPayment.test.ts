import * as creditFilterChangeModule from 'auto-core/react/dataDomain/credit/actions/creditFilterChange/creditFilterChange';

import { getMaxPayment } from './getMaxPayment';

describe('getMaxPayment', () => {
    it('вернет creditPaymentTo (cм реализацию)', () => {
        const creditPaymentTo = 200;
        const priceTo = 150;
        const paymentRight = 210;
        const creditPaymentFrom = 130;

        const result = getMaxPayment({
            amount: 0,
            creditPaymentFrom,
            creditPaymentTo,
            maxProductAmount: 0,
            minRate: 0,
            paymentRight,
            paymentSliderValues: [],
            priceFrom: 0,
            priceTo,
            term: 0,
        });

        expect(result).toEqual(creditPaymentTo);
    });

    it('вызовет getPayment(...) если amount > priceFrom && amount <= maxProductAmount', () => {
        const amount = 200;
        const maxProductAmount = 300 ;
        const priceFrom = 100;
        const term = 12;
        const minRate = 12;

        const spy = jest.spyOn(creditFilterChangeModule, 'getPayment');

        getMaxPayment({
            amount,
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            maxProductAmount,
            minRate,
            paymentRight: 0,
            paymentSliderValues: [],
            priceFrom,
            priceTo: 0,
            term,
        });

        expect(spy).toHaveBeenCalledWith(amount, minRate, term);
    });

    it('вернет последнее значение слайдера при нулях', () => {
        const paymentSliderValues = [
            { value: 1 },
            { value: 2 },
        ];

        const result = getMaxPayment({
            amount: 0,
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            maxProductAmount: 0,
            minRate: 0,
            paymentRight: 0,
            paymentSliderValues: paymentSliderValues,
            priceFrom: 0,
            priceTo: 0,
            term: 0,
        });

        expect(result).toEqual(2);
    });
});
