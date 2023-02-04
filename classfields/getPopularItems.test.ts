import filtersMock from 'auto-core/models/equipment/mock';

import getPopularItems from './getPopularItems';

it('правильно фильтрует категории', () => {
    const result = getPopularItems(filtersMock, [ 'led-lights', 'abs' ]);

    expect(result).toEqual([
        {
            groups: [
                {
                    code: 'airbag',
                    multiple_choice: true,
                    name: 'Подушки безопасности',
                    options: [
                        {
                            code: 'airbag-driver',
                            full_name: 'Подушка безопасности водителя',
                            is_popular: true,
                            name: 'Водителя',
                            weight: 302990,
                        },
                        {
                            code: 'airbag-passenger',
                            full_name: 'Подушка безопасности пассажира',
                            is_popular: false,
                            name: 'Пассажира',
                            weight: 302990,
                        },
                    ],
                },
                {
                    code: undefined,
                    multiple_choice: false,
                    name: undefined,
                    options: [
                        {
                            code: 'abs',
                            full_name: 'Антиблокировочная система (ABS)',
                            is_popular: true,
                            name: 'ABS',
                            weight: 302990,
                        },
                    ],
                },
            ],
            name: 'Безопасность',
        },
        {
            groups: [
                {
                    code: 'lights',
                    multiple_choice: false,
                    name: 'Фары',
                    options: [
                        {
                            code: 'laser-lights',
                            full_name: 'Лазерные фары',
                            is_popular: false,
                            name: 'Лазерные',
                            weight: 302990,
                        },
                        {
                            code: 'led-lights',
                            full_name: 'Светодиодные фары',
                            is_popular: false,
                            name: 'Светодиодные',
                            weight: 302990,
                        },
                    ],
                },
            ],
            name: 'Обзор',
        },
        {
            groups: [
                {
                    code: 'steklo',
                    multiple_choice: false,
                    name: 'Стеклоподъемники',
                    options: [
                        {
                            code: 'steklo-auto',
                            full_name: 'Автоматические',
                            is_popular: true,
                            name: 'Автоматические',
                            weight: 302990,
                        },
                        {
                            code: 'steklo-manual',
                            full_name: 'Ручные',
                            is_popular: true,
                            name: 'Ручные',
                            weight: 302990,
                        },
                    ],
                },
            ],
            name: 'Комфорт',
        },
    ]);
});
