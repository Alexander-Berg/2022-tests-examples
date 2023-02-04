import MockDate from 'mockdate';

import getNextMonths from './getNextMonths';

describe('тесты для функции getNextMonths', () => {
    it('если задан не текущий год, показываем все месяцы', () => {
        MockDate.set('2020-05-20');

        expect(getNextMonths(2021)).toHaveLength(12);
    });
    it('для текущего года мы начинаем показывать месяцы со следующего', () => {
        MockDate.set('2021-05-20');
        expect(getNextMonths(2021)).toHaveLength(7);
    });

    it('если текущий год и декабрь месяц, возвращаем пустой массив', () => {
        MockDate.set('2021-12-20');
        expect(getNextMonths(2021)).toHaveLength(0);
    });
});
