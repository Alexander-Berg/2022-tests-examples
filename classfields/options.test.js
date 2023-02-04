const prepareOptions = require('./options');

const OPTIONS = [
    '12-inch-wheels',
    '12v-socket',
];

const EQUIPMENT_DICTIONARY = {
    '12-inch-wheels': {
        code: '12-inch-wheels',
        name: 'Легкосплавные диски 12"',
        group: 'Элементы экстерьера',
    },
    '12v-socket': {
        code: '12v-socket',
        name: 'Розетка 12V',
        group: 'Мультимедиа',
    },
    '13-inch-wheels': {
        code: '13-inch-wheels',
        name: 'Легкосплавные диски 13"',
        group: 'Элементы экстерьера',
    },
    '14-inch-wheels': {
        code: '14-inch-wheels',
        name: 'Легкосплавные диски 14"',
        group: 'Элементы экстерьера',
    },
};

it('должен вернуть список объектов с информацией об опциях', () => {
    expect(
        prepareOptions(OPTIONS, EQUIPMENT_DICTIONARY),
    ).toEqual([
        {
            code: '12-inch-wheels',
            name: 'Легкосплавные диски 12"',
            group: 'Элементы экстерьера',
        },
        {
            code: '12v-socket',
            name: 'Розетка 12V',
            group: 'Мультимедиа',
        },
    ]);
});

it('должен корректно сработать, даже если некоторых опций нет в словаре', () => {
    expect(
        prepareOptions([
            '13-inch-wheels',
            'third-rear-headrest',
            'windscreen-heat',
        ], EQUIPMENT_DICTIONARY),
    ).toEqual([
        {
            code: '13-inch-wheels',
            name: 'Легкосплавные диски 13"',
            group: 'Элементы экстерьера',
        },
    ]);
});
