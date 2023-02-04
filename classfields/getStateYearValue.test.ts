import MockDate from 'mockdate';

import { getStateYearValue } from './getStateYearValue';

describe('getStateYearValue', () => {
    beforeEach(() => {
        MockDate.set('2022-01-01');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('вернет введенный пользователем год', () => {
        const YEAR = '2013';
        const MIN_YEAR = '2013';
        const actual = getStateYearValue(YEAR, MIN_YEAR);

        expect(actual).toBe(YEAR);
    });

    it('вернет минимальный год если введенный год меньше минимального', () => {
        const YEAR = '2014';
        const MIN_YEAR = '2015';
        const actual = getStateYearValue(YEAR, MIN_YEAR);

        expect(actual).toBe(MIN_YEAR);
    });

    it('вернет текущий год если введенный год больше текущего', () => {
        const CURRENT_YAR = '2022';
        const YEAR = '2023';
        const MIN_YEAR = '2015';
        const actual = getStateYearValue(YEAR, MIN_YEAR);

        expect(actual).toBe(CURRENT_YAR);
    });

    it('вернет введенный пользователем год, так как минимальный год не задан он рассчитывается как текущий минус N лет', () => {
        const YEAR = '2007';
        const actual = getStateYearValue(YEAR, '');

        expect(actual).toBe(YEAR);
    });

    it('вернет введенный минимальный год, который был рассчитан как текущий год минус 15 лет', () => {
        const YEAR = '2006';
        const MIN_YEAR = '2007';
        const actual = getStateYearValue(YEAR, '');

        expect(actual).toBe(MIN_YEAR);
    });

    it('вернет текущий год если если введенный год больше текущего', () => {
        const YEAR = '2023';
        const CURRENT_YAR = '2022';
        const actual = getStateYearValue(YEAR, '');

        expect(actual).toBe(CURRENT_YAR);
    });

    it('вернет ввод пользователя, указан минимальный год', () => {
        const YEAR = '202';
        const actual = getStateYearValue(YEAR, '2018');

        expect(actual).toBe(YEAR);
    });

    it('вернет ввод пользователя, минимальный год не указан', () => {
        const YEAR = '202';
        const actual = getStateYearValue(YEAR, '');

        expect(actual).toBe(YEAR);
    });

    it('вернет ввод пользователя, т.е пустую строку, если введенная строка пустая', () => {
        const YEAR = '2010';
        const actual = getStateYearValue('', YEAR);

        expect(actual).toBe('');
    });

    it('вернет ввод пользователя, если пользователь ввел 0 или меньше 4 символов', () => {
        const YEAR = '2010';
        const actual = getStateYearValue('0', YEAR);

        expect(actual).toBe('0');
    });
});
