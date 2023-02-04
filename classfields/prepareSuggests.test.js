const prepareSuggests = require('./prepareSuggests');

const mockedDesc = {
    transmission: {
        label: 'Коробка',
        key: 'transmission',
    },
    body_type_group: {
        label: 'Тип кузова',
        key: 'body_type_group',
    },
    owners_count_group: {
        label: 'Кол-во владельцев',
        val: 'Не более 2',
        key: 'owners_count_group',
    },
    exchange_group: {
        label: 'Обмен',
        val: '',
        key: 'exchange_group',
    },
    only_nds: {
        label: 'Продажа с НДС',
        val: '',
        key: 'only_nds',
    },
};

const mockedEqDesc = {
    ptf: {
        label: 'Противотуманные фары',
        key: 'ptf',
    },
    'light-sensor': {
        label: 'Датчик света',
        key: 'light-sensor',
    },
};

const mockedTagsDesc = {
    certificate_manufacturer: {
        label: 'Проверено производителем',
        key: 'certificate_manufacturer',
    },
    history_discount: {
        label: 'Сниженная цена',
        key: 'history_discount',
    },
    'wide-back-seats': {
        label: 'Просторный задний ряд',
        key: 'wide-back-seats',
    },
};

describe('обработка значений фильтров', () => {
    it('если нечем обогащать, вернёт в том же виде', () => {
        const suggestQueue = [
            {
                resetKey: 'some_search_param1',
                resetValue: '42',
                suggestText: '42',
            },
            {
                resetKey: 'catalog_equipment',
                resetValue: '42',
                suggestText: '42',
            },
        ];

        expect(prepareSuggests(suggestQueue)).toEqual(suggestQueue);
    });

    it('отображение, если выбрана группа коробок или кузовов', () => {
        const suggests = prepareSuggests([
            {
                resetKey: 'transmission',
                resetValue: [ 'AUTO', 'AUTOMATIC', 'ROBOT', 'VARIATOR' ],
            },
            {
                resetKey: 'body_type_group',
                resetValue: [ 'HATCHBACK', 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ],
            },
        ],
        mockedDesc,
        {},
        );
        expect(suggests).toEqual([
            {
                resetKey: 'transmission',
                resetLabel: 'Коробка',
                resetValue: [ 'AUTOMATIC', 'ROBOT', 'VARIATOR' ],
                suggestText: 'Коробка: выбранные 3',
            },
            {
                resetKey: 'body_type_group',
                resetLabel: 'Тип кузова',
                resetValue: [ 'HATCHBACK_3_DOORS', 'HATCHBACK_5_DOORS', 'LIFTBACK' ],
                suggestText: 'Тип кузова: выбранные 3',
            },
        ]);
    });

    it('отображение количества владельцев', () => {
        const suggests = prepareSuggests([
            {
                resetKey: 'owners_count_group',
                resetValue: 'LESS_THAN_TWO',
            },
        ],
        mockedDesc,
        {},
        );
        expect(suggests).toEqual([
            {
                resetKey: 'owners_count_group',
                resetLabel: 'Кол-во владельцев',
                resetValue: 'LESS_THAN_TWO',
                suggestText: 'Не более 2 владельцев',
            },
        ]);
    });

    it('отображение фильтра Обмен', () => {
        const suggests = prepareSuggests([
            {
                resetKey: 'exchange_group',
                resetValue: 'POSSIBLE',
            },
        ],
        mockedDesc,
        {},
        );
        expect(suggests).toEqual([
            {
                resetKey: 'exchange_group',
                resetLabel: 'Обмен',
                resetValue: 'POSSIBLE',
                suggestText: 'Обмен',
            },
        ]);
    });

    it('отображение фильтра "Продажа с НДС"', () => {
        const suggests = prepareSuggests([
            {
                resetKey: 'only_nds',
                resetValue: true,
            },
        ],
        mockedDesc,
        {},
        );
        expect(suggests).toEqual([
            {
                resetKey: 'only_nds',
                resetLabel: 'Продажа с НДС',
                resetValue: true,
                suggestText: 'Продажа с НДС',
            },
        ]);
    });

    it('отображение комплектующих', () => {
        const suggests = prepareSuggests([
            {
                resetKey: 'catalog_equipment',
                resetValue: 'light-sensor',
            },
            {
                resetKey: 'catalog_equipment',
                resetValue: 'ptf',
            },
        ],
        mockedDesc,
        mockedEqDesc,
        );
        expect(suggests).toEqual([
            {
                resetKey: 'catalog_equipment',
                resetValue: 'light-sensor',
                resetLabel: 'light-sensor',
                suggestText: 'Датчик света',
            },
            {
                resetKey: 'catalog_equipment',
                resetValue: 'ptf',
                resetLabel: 'ptf',
                suggestText: 'Противотуманные фары',
            },
        ]);
    });

    it('search_tag', () => {
        const suggestQueue = [
            {
                resetKey: 'search_tag',
                resetValue: 'certificate_manufacturer',
            },
            {
                resetKey: 'search_tag',
                resetValue: 'history_discount',
            },
            {
                resetKey: 'search_tag',
                resetValue: 'wide-back-seats',
            },
        ];
        const suggests = prepareSuggests(suggestQueue, mockedDesc, mockedEqDesc, mockedTagsDesc);

        expect(suggests).toEqual([
            {
                resetKey: 'search_tag',
                resetValue: 'certificate_manufacturer',
                reselLabel: 'certificate_manufacturer',
                suggestText: 'Проверено производителем',
            },
            {
                resetKey: 'search_tag',
                resetValue: 'history_discount',
                reselLabel: 'history_discount',
                suggestText: 'Сниженная цена',
            },
            {
                resetKey: 'search_tag',
                resetValue: 'wide-back-seats',
                reselLabel: 'wide-back-seats',
                suggestText: 'Просторный задний ряд',
            },
        ]);
    });
});
