import MockDate from 'mockdate';

import { generateDates } from './generateDates';

describe('generateDates', () => {

    afterEach(() => {
        MockDate.reset();
    });

    it('правильно генерирует даты текущего месяца', () => {
        MockDate.set('2021-11-08');

        expect(generateDates(4)).toEqual([
            { label: 'сегодня', value: '08-11-2021' },
            { label: '9 ноября', value: '09-11-2021' },
            { label: '10 ноября', value: '10-11-2021' },
            { label: '11 ноября', value: '11-11-2021' },
        ]);
    });

    it('правильно генерирует даты с переходом на следующий месяц', () => {
        MockDate.set('2021-11-28');

        expect(generateDates(5)).toEqual([
            { label: 'сегодня', value: '28-11-2021' },
            { label: '29 ноября', value: '29-11-2021' },
            { label: '30 ноября', value: '30-11-2021' },
            { label: '1 декабря', value: '01-12-2021' },
            { label: '2 декабря', value: '02-12-2021' },
        ]);
    });

    it('правильно генерирует даты с переходом на следующий год', () => {
        MockDate.set('2021-12-29');

        expect(generateDates(5)).toEqual([
            { label: 'сегодня', value: '29-12-2021' },
            { label: '30 декабря', value: '30-12-2021' },
            { label: '31 декабря', value: '31-12-2021' },
            { label: '1 января 2022', value: '01-01-2022' },
            { label: '2 января 2022', value: '02-01-2022' },
        ]);
    });

    it('правильно генерирует даты без текущего дня, если время подачи заявки после 12:00', () => {
        let date = new Date(2021, 11, 29, 13);
        MockDate.set(date);

        expect(generateDates(5)).toEqual([
            { label: '30 декабря', value: '30-12-2021' },
            { label: '31 декабря', value: '31-12-2021' },
            { label: '1 января 2022', value: '01-01-2022' },
            { label: '2 января 2022', value: '02-01-2022' },
        ]);

        date = new Date(2021, 11, 29, 12, 1);
        MockDate.set(date);
        expect(generateDates(5)).toEqual([
            { label: '30 декабря', value: '30-12-2021' },
            { label: '31 декабря', value: '31-12-2021' },
            { label: '1 января 2022', value: '01-01-2022' },
            { label: '2 января 2022', value: '02-01-2022' },
        ]);
    });
});
