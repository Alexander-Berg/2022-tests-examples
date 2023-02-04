import { parsePeriod } from '../parsers';

describe('parsePeriod', () => {
    it('parse hyphen separated', () => {
        expect(parsePeriod('12.03.04-13.22.05')).toEqual({
            from: '12.03.04',
            to: '13.22.05'
        });
    });

    it('parse * as undefined in start date', () => {
        expect(parsePeriod('*-13.22.05')).toEqual({
            from: undefined,
            to: '13.22.05'
        });
    });

    it('parse * as undefined in end date', () => {
        expect(parsePeriod('12.03.04-*')).toEqual({
            from: '12.03.04',
            to: undefined
        });
    });
});
