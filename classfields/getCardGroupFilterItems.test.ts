import stateMock from '../mocks/state.mock';

import getCardGroupFilterItems from './getCardGroupFilterItems';

it('должен вернуть набор значений фильтров для карточки группы', () => {

    expect(getCardGroupFilterItems(stateMock)).toStrictEqual({
        additionalOptionsGroupedItems: [
            {
                groupName: 'Обзор',
                options: [
                    {
                        code: 'laser-lights',
                        group: 'Обзор',
                        name: 'Лазерные фары',
                    },
                    {
                        code: 'led-lights',
                        group: 'Обзор',
                        name: 'Светодиодные фары',
                    },
                ],
            },
        ],
        additionalOptionsFilterItems: [
            {
                code: 'laser-lights',
                group: 'Фары',
                name: 'Лазерные фары',
            },
            {
                code: 'led-lights',
                group: 'Фары',
                name: 'Светодиодные фары',
            },
        ],
        availableOptionsGroupedItems: [
            {
                groupName: 'Обзор',
                options: [
                    {
                        code: 'xenon',
                        group: 'Обзор',
                        name: 'Ксеноновые/Биксеноновые фары',
                    },
                ],
            },
        ],
        availableOptionsUniqueGroups: {},
        cardGroupSearchTags: [
            {
                code: 'medium',
                name: 'Средний размер',
                tag: {
                    applicability: 'ALL',
                    code: 'medium',
                    mapping: true,
                    name: 'Средний размер',
                    order: 20,
                    tag_group: {
                        code: 'guru',
                        name: '[Гуру]',
                    },
                },
            },
            {
                code: 'big',
                name: 'Большой',
                tag: {
                    applicability: 'ALL',
                    code: 'big',
                    mapping: true,
                    name: 'Большой',
                    order: 30,
                    tag_group: {
                        code: 'guru',
                        name: '[Гуру]',
                    },
                },
            },
        ],
        colorFilterItems: [
            {
                hex: '200204',
                value: '200204',
                title: 'коричневый',
                titleShort: 'коричн.',
                visibleHex: '926547',
            },
            {
                hex: '0000CC',
                value: '0000CC',
                title: 'синий',
                titleShort: 'синий',
                visibleHex: '334dff',
            },
        ],
        complectationFilterItems: [
            {
                title: 'Test',
                value: 'Test',
                optionsCount: 1,
                priceFrom: {
                    RUR: 1000000,
                },
                availableOptions: [
                    'xenon',
                ],
                uniqueAvailableOptions: [],
            },
            {
                title: 'Test1',
                value: 'Test1',
                optionsCount: 2,
                priceFrom: {
                    RUR: 1000000,
                },
                availableOptions: [
                    'xenon',
                    'led-lights',
                ],
                uniqueAvailableOptions: [],
            },
        ],
        engineFilterItems: [
            {
                acceleration: 11,
                displacement: 2200,
                engineType: 'DIESEL',
                fuel_rate: 5.1,
                id: '2.2 л / 150 л.с. / Дизель',
                power: 150,
                title: 'Дизель 2.2 л, 150 л.c.',
                value: [ '345' ],
            },
            {
                acceleration: 11,
                displacement: 1968,
                engineType: 'DIESEL',
                fuel_rate: 5.1,
                id: '2.0 л / 150 л.с. / Дизель',
                power: 150,
                title: 'Дизель 2.0 л, 150 л.c.',
                value: [ '346' ],
            },
            {
                acceleration: 7,
                displacement: 0,
                engineType: 'ELECTRO',
                fuel_rate: undefined,
                id: '400 л.с. / 294 кВт / Электро',
                power: 400,
                title: 'Электро / 400 л.c. / 294 кВт',
                value: [ '123', '12345' ],
            },
        ],
        gearTypeFilterItems: [
            {
                title: 'Передний',
                value: 'FORWARD_CONTROL',
            },
            {
                title: 'Полный',
                value: 'ALL_WHEEL_DRIVE',
            },
        ],
        transmissionFilterItems: [
            {
                title: 'Автоматическая',
                value: 'AUTOMATIC',
            },
            {
                title: 'Механическая',
                value: 'MECHANICAL',
            },
        ],
    });
});
