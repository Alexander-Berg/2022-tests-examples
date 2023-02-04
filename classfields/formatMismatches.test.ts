import type { ApplicationMismatch } from '../consts/types';

import { formatMismatches } from './formatMismatches';

describe('Форматирование ошибок в форме заведения заявки на Выкуп для АМов', () => {
    it('Форматирует данные', () => {
        const mismatches = [
            {
                field_name: 'vin',
                value: 'XXX',
                description: 'should be unique value',
                reason: 'INVALID',
            },
            {
                field_name: 'mileage',
                value: 'Some(5000000)',
                description: '>= 200000',
                reason: 'INVALID',
            },
            {
                field_name: 'some_field',
            },
        ] as Array<ApplicationMismatch>;

        expect(formatMismatches(mismatches)).toEqual([
            [
                { title: 'Поле', value: 'vin' },
                { title: 'Статус', value: 'INVALID' },
                { title: 'Правило', value: 'should be unique value' },
                { title: 'Текущее значение', value: 'XXX' },
            ],
            [
                { title: 'Поле', value: 'mileage' },
                { title: 'Статус', value: 'INVALID' },
                { title: 'Правило', value: '>= 200000' },
                { title: 'Текущее значение', value: 'Some(5000000)' },
            ],
            [
                { title: 'Поле', value: 'some_field' },
                { title: 'Статус', value: undefined },
                { title: 'Правило', value: undefined },
                { title: 'Текущее значение', value: undefined },
            ],
        ]);
    });

    it('При пустом массиве возвращает пустой массив', () => {
        expect(formatMismatches([])).toEqual([]);
    });
});
