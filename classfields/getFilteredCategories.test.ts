import filtersMock from 'auto-core/models/equipment/mock';

import getFilteredCategories from './getFilteredCategories';

it('правильно фильтрует категории', () => {
    const result = getFilteredCategories(filtersMock, 'под');

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
            ],
            name: 'Безопасность',
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
        {
            groups: [
                {
                    code: undefined,
                    multiple_choice: false,
                    name: undefined,
                    options: [
                        {
                            code: 'front-centre-armrest',
                            full_name: 'Центральный подлокотник',
                            is_popular: false,
                            name: 'Подлокотник',
                            weight: 302990,
                        },
                    ],
                },
            ],
            name: 'Салон',
        },
    ]);
});
