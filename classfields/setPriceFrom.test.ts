import { priceFromFieldName } from '../creditFilterChange';

import { setPriceFrom } from './setPriceFrom';

const onChange = jest.fn();

describe('setPriceFrom', () => {
    it('если price > maxAmount, то вернет maxAmount', () => {
        expect(setPriceFrom(
            100,
            80,
            90,
            onChange,
        )).toEqual(90);

        expect(onChange).toHaveBeenCalledWith(
            90, { name: priceFromFieldName },
        );
    });

    it('если price < minAmount, то вернет minAmount', () => {
        expect(setPriceFrom(
            70,
            80,
            90,
            onChange,
        )).toEqual(80);

        expect(onChange).toHaveBeenCalledWith(
            80, { name: priceFromFieldName },
        );
    });

    it('если price цена между мин и макс то вернет ее', () => {
        expect(setPriceFrom(
            85,
            80,
            90,
            onChange,
        )).toEqual(85);

        expect(onChange).toHaveBeenCalledWith(
            85, { name: priceFromFieldName },
        );
    });
});
