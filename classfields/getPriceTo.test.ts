import { getPrice } from 'auto-core/react/dataDomain/credit/actions/creditFilterChange/creditFilterChange';

import { getPriceTo } from './getPriceTo';

describe('getPriceTo', () => {
    it('вернет priceTo как есть', () => {
        const priceTo = getPriceTo({
            amountRangeTo: 0,
            creditInitialFee: 90,
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            minRate: 0,
            priceFrom: 20,
            priceTo: 100,
            term: 0,
        });

        expect(priceTo).toBe(100);
    });

    it('вернет 0 если creditInitialFee > priceTo', () => {
        const priceTo = getPriceTo({
            amountRangeTo: 0,
            creditInitialFee: 240,
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            minRate: 0,
            priceFrom: 20,
            priceTo: 100,
            term: 0,
        });

        expect(priceTo).toBe(0);
    });

    it('вернет 0 если priceFrom >= priceTo', () => {
        const priceTo = getPriceTo({
            amountRangeTo: 0,
            creditInitialFee: 90,
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            minRate: 0,
            priceFrom: 120,
            priceTo: 100,
            term: 0,
        });

        expect(priceTo).toBe(0);
    });

    it('если priceTo = 0 вернет getPrice(creditPaymentTo, minRate, term, creditInitialFee)', () => {
        const creditPaymentTo = 10;
        const minRate = 12;
        const term = 14;
        const creditInitialFee = 200;

        const price = getPrice(creditPaymentTo, minRate, term, creditInitialFee);

        const priceTo = getPriceTo({
            amountRangeTo: 0,
            creditInitialFee,
            creditPaymentFrom: 0,
            creditPaymentTo,
            minRate,
            priceFrom: 120,
            priceTo: 0,
            term,
        });

        expect(priceTo).toEqual(price);
    });

    it('если priceTo = 0 и creditPaymentTo = 0, вернет Math.round(amountRangeTo + creditInitialFee)', () => {
        const amountRangeTo = 10;
        const creditInitialFee = 200;

        const price = Math.round(amountRangeTo + creditInitialFee);

        const priceTo = getPriceTo({
            amountRangeTo,
            creditInitialFee,
            creditPaymentFrom: 0,
            creditPaymentTo: 0,
            minRate: 0,
            priceFrom: 120,
            priceTo: 0,
            term: 0,
        });

        expect(priceTo).toEqual(price);
    });

    it('если priceTo = 0 и creditPaymentTo < creditPaymentFrom, вернет Math.round(amountRangeTo + creditInitialFee)', () => {
        const amountRangeTo = 10;
        const creditInitialFee = 200;

        const price = Math.round(amountRangeTo + creditInitialFee);

        const priceTo = getPriceTo({
            amountRangeTo,
            creditInitialFee,
            creditPaymentFrom: 100,
            creditPaymentTo: 90,
            minRate: 0,
            priceFrom: 120,
            priceTo: 0,
            term: 0,
        });

        expect(priceTo).toEqual(price);
    });
});
