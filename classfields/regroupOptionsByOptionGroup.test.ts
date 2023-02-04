import equipmentDictionary from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';

import regroupOptionsByOptionGroup from './regroupOptionsByOptionGroup';

it('должен вернуть опции, сгруппированные по имени группы опций, из опций, сгруппированных по модификации', () => {
    expect(regroupOptionsByOptionGroup(
        [
            {
                modification: '1.4 AT',
                options: [ '12v-socket', '14-inch-wheels' ],
                prefix: 'Только для модификации',
            },
            {
                modification: '1.4 MT',
                options: [ '17-inch-wheels' ],
                prefix: 'Только для модификации',
            },
        ],
        equipmentDictionary.data,
    )).toStrictEqual({
        'Элементы экстерьера': [
            {
                modification: '1.4 AT',
                options: [
                    {
                        code: '14-inch-wheels',
                        name: 'Легкосплавные диски 14"',
                        group: 'Элементы экстерьера',
                    },
                ],
                prefix: 'Только для модификации',
            },
            {
                modification: '1.4 MT',
                options: [
                    {
                        code: '17-inch-wheels',
                        name: 'Легкосплавные диски 17"',
                        group: 'Элементы экстерьера',
                    },
                ],
                prefix: 'Только для модификации',
            },
        ],
        Мультимедиа: [
            {
                modification: '1.4 AT',
                options: [
                    {
                        code: '12v-socket',
                        name: 'Розетка 12V',
                        group: 'Мультимедиа',
                    },
                ],
                prefix: 'Только для модификации',
            },
        ],
    });
});

it('должен вернуть пустой объект, если опций нет', () => {
    expect(regroupOptionsByOptionGroup([], equipmentDictionary.data)).toStrictEqual({});
});
