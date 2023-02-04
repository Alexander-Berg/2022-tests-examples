import each from 'jest-each';
import { humariseMonth } from '../humarise-month';

const data = [
    ['012018', 'январь, 2018'],
    ['122016', 'декабрь, 2016'],
    ['102000', 'октябрь, 2000'],
    ['022019', 'февраль, 2019']
];

describe('humariseMonth', () => {
    each(data).test('%s', (val, expected) => {
        expect(humariseMonth(val)).toBe(expected);
    });
});
