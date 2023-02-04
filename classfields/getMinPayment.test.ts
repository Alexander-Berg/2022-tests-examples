import * as creditFilterChangeModule from 'auto-core/react/dataDomain/credit/actions/creditFilterChange/creditFilterChange';

import { getMinPayment } from './getMinPayment';

describe('getMinPayment', () => {
    it('вернет creditPaymentFrom, если он есть, и внутри интервала допустимы значений', () => {
        const creditPaymentFrom = 100;
        const result = getMinPayment({
            creditPaymentFrom,
            creditPaymentTo: 110,
            minProductAmount: 0,
            minRate: 0,
            paymentLeft: 90,
            paymentSliderValues: [],
            priceFrom: 10,
            term: 0,
        });

        expect(result).toEqual(creditPaymentFrom);
    });

    it('вызовет getPayment() если priceFrom >= minProductAmount', () => {
        const creditPaymentFrom = 100;
        const priceFrom = 100;
        const minRate = 10;
        const term = 12;

        const getPaymentSpy = jest.spyOn(creditFilterChangeModule, 'getPayment');

        getMinPayment({
            creditPaymentFrom,
            creditPaymentTo: 110,
            minProductAmount: 0,
            minRate,
            paymentLeft: 120,
            paymentSliderValues: [],
            priceFrom,
            term,
        });

        expect(getPaymentSpy).toHaveBeenCalledWith(
            priceFrom, minRate, term,
        );
    });

    it('вернет первое значение из слайдера если все по нулям', () => {
        const paymentSliderValues = [
            { value: 1 },
            { value: 2 },
            { value: 3 },
        ];

        const result = getMinPayment({
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            minProductAmount: 0,
            minRate: 0,
            paymentLeft: 0,
            paymentSliderValues,
            priceFrom: 0,
            term: 0,
        });

        expect(result).toEqual(paymentSliderValues[0].value);
    });

});
