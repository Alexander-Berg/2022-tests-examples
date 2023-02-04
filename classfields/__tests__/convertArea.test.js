import convertArea from '..';

const SQUARE_METER = 'SQUARE_METER';
const ARE = 'ARE';
const HECTARE = 'HECTARE';

describe('convertArea', () => {
    it('returns meter from meter', () => {
        const value = {
            unit: SQUARE_METER,
            value: 99
        };

        expect(convertArea(value)).toEqual(value);
    });

    it('returns are from are', () => {
        const value = {
            unit: ARE,
            value: 99
        };

        expect(convertArea(value)).toEqual(value);
    });

    it('returns hectare from hectare', () => {
        const value = {
            unit: HECTARE,
            value: 100
        };

        expect(convertArea(value)).toEqual(value);
    });

    it('returns meter from are', () => {
        const value = {
            unit: ARE,
            value: 0.5
        };

        expect(convertArea(value)).toEqual({
            unit: SQUARE_METER,
            value: 50
        });
    });

    it('returns meter from hectare', () => {
        const value = {
            unit: HECTARE,
            value: 0.005
        };

        expect(convertArea(value)).toEqual({
            unit: SQUARE_METER,
            value: 50
        });
    });

    it('returns are from meter', () => {
        const value = {
            unit: SQUARE_METER,
            value: 100
        };

        expect(convertArea(value)).toEqual({
            unit: ARE,
            value: 1
        });
    });

    it('returns are from hectare', () => {
        const value = {
            unit: HECTARE,
            value: 0.5
        };

        expect(convertArea(value)).toEqual({
            unit: ARE,
            value: 50
        });
    });

    it('returns hectare from are', () => {
        const value = {
            unit: ARE,
            value: 10000
        };

        expect(convertArea(value)).toEqual({
            unit: HECTARE,
            value: 100
        });
    });

    it('returns hectare from meter', () => {
        const value = {
            unit: SQUARE_METER,
            value: 1000000
        };

        expect(convertArea(value)).toEqual({
            unit: HECTARE,
            value: 100
        });
    });
});
