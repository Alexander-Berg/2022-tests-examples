import { nbsp, mdash, laquo, raquo } from 'auto-core/react/lib/html-entities';

import { formatRequirements } from './formatRequirements';

describe('formatRequirements: Форматирование требований к автомобилям для Выкупа', () => {
    it('Заменяет html сущности на печатаемые символы', () => {
        expect(formatRequirements([
            'Примерная стоимость от&nbsp;500&nbsp;000 до&nbsp;4&nbsp;000&nbsp;000&nbsp;₽',
            '&laquo;Авто.ру Выкуп&raquo;&nbsp;&mdash; быстрый способ продать автомобиль',
        ])).toEqual([
            `Примерная стоимость от${ nbsp }500${ nbsp }000 до${ nbsp }4${ nbsp }000${ nbsp }000${ nbsp }₽`,
            `${ laquo }Авто.ру Выкуп${ raquo }${ nbsp }${ mdash } быстрый способ продать автомобиль`,
        ]);
    });
});
