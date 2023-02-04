import { configureMoment } from 'view/libs/configureMoment';

import {
    formatDate,
    parseStandardDate,
    standardDatePattern,
    standardLongDatePattern
} from '../date';

configureMoment(new Date());

describe('parse and format', () => {
    it('should format with standard date pattern', () => {
        expect(formatDate(new Date(2018, 1, 1), standardDatePattern)).toBe('01.02.2018');
    });

    it('should format with standard long date pattern', () => {
        expect(formatDate(new Date(2018, 1, 1), standardLongDatePattern)).toBe('1 февраля 2018, 00:00');
    });

    it('format(parse(date)) = date', () => {
        const date = '01.02.2003';

        expect(formatDate(parseStandardDate(date), standardDatePattern)).toBe(date);
    });

    it('parse(format(date)) = date for every date', () => {
        const now = new Date();
        const date = new Date(now.getFullYear(), now.getMonth(), now.getDate());

        expect(
            parseStandardDate(formatDate(date, standardDatePattern)).toISOString()
        ).toBe(date.toISOString());
    });
});
