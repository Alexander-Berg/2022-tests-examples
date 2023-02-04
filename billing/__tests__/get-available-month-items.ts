import each from 'jest-each';
import { format, subMonths } from 'date-fns';

import { getAvailableMonthItems } from '../get-available-month-items';
import { MONTH_VALUE_FORMAT } from '../../constants';

const MONTHES = 13;

const monthes = getAvailableMonthItems();

let date = new Date();

describe('getAvailableMonthItems', () => {
    describe('By default', () => {
        test(`Must be generated ${MONTHES} months`, () => {
            expect(monthes.length).toBe(MONTHES);
        });

        describe('Test items', () => {
            afterEach(() => {
                date = subMonths(date, 1);
            });

            each(monthes).test('%o', item => {
                expect(item).toBe(format(date, MONTH_VALUE_FORMAT));
            });
        });
    });

    describe('With specified arguments', () => {
        test('Only initial date', () => {
            expect(getAvailableMonthItems(new Date('2018-11-10'))).toEqual([
                '112018',
                '102018',
                '092018',
                '082018',
                '072018',
                '062018',
                '052018',
                '042018',
                '032018',
                '022018',
                '012018',
                '122017',
                '112017'
            ]);
            expect(getAvailableMonthItems(new Date('2019-12-01'))).toEqual([
                '122019',
                '112019',
                '102019',
                '092019',
                '082019',
                '072019',
                '062019',
                '052019',
                '042019',
                '032019',
                '022019',
                '012019',
                '122018'
            ]);
        });

        test('With lenght', () => {
            expect(getAvailableMonthItems(new Date('2018-11-10'), 10)).toEqual([
                '112018',
                '102018',
                '092018',
                '082018',
                '072018',
                '062018',
                '052018',
                '042018',
                '032018',
                '022018'
            ]);
            expect(getAvailableMonthItems(new Date('2018-11-10'), 3)).toEqual([
                '112018',
                '102018',
                '092018'
            ]);
            expect(getAvailableMonthItems(new Date('2018-11-10'), 1)).toEqual(['112018']);
        });
    });
});
