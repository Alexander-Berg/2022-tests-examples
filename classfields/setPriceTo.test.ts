import { maxInitialFee, priceToFieldName } from 'auto-core/react/dataDomain/credit/actions/creditFilterChange/creditFilterChange';

import { setPriceTo } from './setPriceTo';

const onChange = jest.fn();

describe('setPriceTo', () => {
    it('price > maxInitialFee + maxAmount => maxInitialFee + maxAmount', () => {
        const expected = maxInitialFee + 10;
        expect(setPriceTo(
            maxInitialFee + 20,
            80,
            maxInitialFee,
            10,
            10,
            onChange,
        )).toEqual(expected);

        expect(onChange).toHaveBeenCalledWith(
            expected, { name: priceToFieldName },
        );
    });

    it('price > maxAmount + initialFee => maxAmount + initialFee', () => {
        const expected = 20 + 90;
        expect(setPriceTo(
            20 + 90 + 10,
            80,
            90,
            10,
            20,
            onChange,
        )).toEqual(expected);

        expect(onChange).toHaveBeenCalledWith(
            expected, { name: priceToFieldName },
        );
    });

    it('price < minAmount + initialFee => Math.max(priceFrom, minAmount) + initialFee', () => {
        const priceFrom = 10;
        const minAmount = 20;
        const initialFee = 30;
        const expected = Math.max(priceFrom, minAmount) + initialFee;
        expect(setPriceTo(
            20,
            priceFrom,
            initialFee,
            minAmount,
            20,
            onChange,
        )).toEqual(expected);

        expect(onChange).toHaveBeenCalledWith(
            expected, { name: priceToFieldName },
        );
    });

    it('иначе вернет price', () => {
        expect(setPriceTo(
            100,
            11,
            12,
            13,
            120,
            onChange,
        )).toEqual(100);

        expect(onChange).toHaveBeenCalledWith(
            100, { name: priceToFieldName },
        );
    });
});
