import MockDate from 'mockdate';

import getPreviousMonths from './getPreviousMonths';

describe('для текущего года мы ограничиваем список месяцев до текущего', () => {
    it('если задан не текущий год, показываем все месяцы', () => {
        MockDate.set('2020-05-20');

        expect(getPreviousMonths()).toHaveLength(12);
    });
    it('если задан текущий год, ограничиваем массив месяцев до текущего месяца', () => {
        MockDate.set('2021-05-20');

        expect(getPreviousMonths(2021)).toHaveLength(5);
    });

    it('если задан текущий год и декабрь, показываем все месяцы', () => {
        MockDate.set('2021-12-20');

        expect(getPreviousMonths(2021)).toHaveLength(12);
    });
});
