import { getMinMaxPayments } from 'auto-core/react/dataDomain/listing/selectors/getCreditInitialData/helpers/getMinMaxPayments';
import * as creditFilterChangeModule from 'auto-core/react/dataDomain/credit/actions/creditFilterChange/creditFilterChange';
import * as getMaxPaymentModule from 'auto-core/react/dataDomain/listing/selectors/getCreditInitialData/helpers/getMaxPayment';
import * as getMinPaymentModule from 'auto-core/react/dataDomain/listing/selectors/getCreditInitialData/helpers/getMinPayment';

describe('getMinMaxPayments', () => {
    it('если кредит < цены ОТ, то должен рассчитать платежи от и до через getPayment', () => {
        const getPaymentSpy = jest.spyOn(creditFilterChangeModule, 'getPayment');

        const priceTo = 100;
        const priceFrom = 90;
        const creditInitialFee = 90;
        const amount = priceTo - creditInitialFee;
        const minRate = 10;
        const term = 12;

        getMinMaxPayments({
            creditInitialFee,
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            maxProductAmount: 0,
            minProductAmount: 0,
            minRate,
            paymentLeft: 0,
            paymentRight: 0,
            paymentSliderValues: [ { value: 12 }, { value: 1 } ],
            priceFrom,
            priceTo,
            term,
        });

        expect(getPaymentSpy).toHaveBeenCalledWith(amount, minRate, term);
        expect(getPaymentSpy).toHaveBeenCalledWith(priceFrom, minRate, term);
    });

    it('рассчитает минимум и максимум через getMinPayment и getMaxPayment', () => {
        const getMaxPaymentSpy = jest.spyOn(getMaxPaymentModule, 'getMaxPayment');
        const getMinPaymentSpy = jest.spyOn(getMinPaymentModule, 'getMinPayment');

        const priceTo = 201;
        const priceFrom = 19;
        const creditInitialFee = 12;
        const amount = priceTo - creditInitialFee;
        const creditPaymentTo = 120;
        const creditPaymentFrom = 20;
        const minRate = 12;
        const term = 2;
        const maxProductAmount = 31;
        const minProductAmount = 30;
        const paymentLeft = 20;
        const paymentRight = 50;
        const paymentSliderValues = [ { value: 12 }, { value: 1 } ];

        getMinMaxPayments({
            creditInitialFee,
            creditPaymentFrom,
            creditPaymentTo,
            maxProductAmount,
            minProductAmount,
            minRate,
            paymentLeft,
            paymentRight,
            paymentSliderValues,
            priceFrom,
            priceTo,
            term,
        });

        expect(getMinPaymentSpy).toHaveBeenCalledWith({
            creditPaymentFrom,
            creditPaymentTo,
            minProductAmount,
            minRate,
            paymentLeft,
            paymentSliderValues,
            priceFrom,
            term,
        });

        expect(getMaxPaymentSpy).toHaveBeenCalledWith({
            amount,
            creditPaymentFrom,
            creditPaymentTo,
            maxProductAmount,
            minRate,
            paymentRight,
            paymentSliderValues,
            priceFrom,
            priceTo,
            term,
        });
    });
});
