import { getPriceWithArea } from '../get-price-with-area';

describe('getPriceWithArea', () => {
    it('return same price with no area params', () => {
        expect(getPriceWithArea(100, {}))
            .toEqual(100);
    });

    it('return valid price for hectares with price of square meter', () => {
        expect(getPriceWithArea(30, { areaValue: 2.6, areaUnit: 'HECTARE', priceUnit: 'SQ_M' }))
            .toEqual(780000);
    });

    it('return valid price for square meters with price of sotka', () => {
        expect(getPriceWithArea(128000, { areaValue: 63, areaUnit: 'SQ_M', priceUnit: 'SOTKA' }))
            .toEqual(80640);
    });

    it('return valid price if units are equal', () => {
        expect(getPriceWithArea(347000, { areaValue: 3.7, areaUnit: 'HECTARE', priceUnit: 'HECTARE' }))
            .toEqual(1283900);
    });
});
