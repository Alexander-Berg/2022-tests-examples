const getSearchParametersCounters = require('./getSearchParametersCounters');

const TESTS = [
    // power_from учитывается для NEW в главных фильтрах
    {
        test: { category: 'cars', currency: 'RUR', section: 'new', sort: 'fresh_relevance_1-desc', power_from: 1000 },
        result: {
            mmm: 0,
            main: 1,
            extended: 0,
            panel: 0,
        },
    },

    // power_to учитывается для USED|ALL в расширенных фильтрах
    {
        test: { category: 'cars', currency: 'RUR', section: 'used', sort: 'fresh_relevance_1-desc', power_from: 1000 },
        result: {
            mmm: 0,
            main: 0,
            extended: 1,
            panel: 0,
        },
    },

    // ?special=true
    {
        test: { category: 'cars', currency: 'RUR', section: 'used', sort: 'fresh_relevance_1-desc', special: true },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },
    // ?pinned_offer_id=XXX-XXX
    {
        test: { category: 'cars', currency: 'RUR', section: 'used', sort: 'fresh_relevance_1-desc', pinned_offer_id: '123-abc' },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },

    // configuration_id не считаем
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            catalog_filter: [
                { mark: 'AUDI', model: 'A4', generatiom: '123' },
            ],
            configuration_id: '1234',
        },
        result: {
            mmm: 1,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },

    // tech_param_id не считаем
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            catalog_filter: [
                { mark: 'AUDI', model: 'A4', generatiom: '123' },
            ],
            configuration_id: '1234',
            tech_param_id: '456',
        },
        result: {
            mmm: 1,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },

    // пустой объект в catalog_filter не считаем
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            catalog_filter: [ {} ],
            configuration_id: '1234',
            tech_param_id: '456',
        },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },

    // тег certificate_manufacturer считаем
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            search_tag: [ 'certificate_manufacturer' ],
        },
        result: {
            mmm: 0,
            main: 0,
            extended: 1,
            panel: 0,
        },
    },

    // тег history_discount не считаем
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            search_tag: [ 'history_discount' ],
        },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },

    // тег history_discount не считаем
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            search_tag: [ 'history_discount' ],
        },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },

    // тег может быть и массивом и строкой ¯\_(ツ)_/¯
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            search_tag: 'certificate_manufacturer',
        },
        result: {
            mmm: 0,
            main: 0,
            extended: 1,
            panel: 0,
        },
    },

    // считаем кол-во тегов
    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            search_tag: [ 'some_tag1', 'some_tag2' ],
        },
        result: {
            mmm: 0,
            main: 0,
            extended: 2,
            panel: 0,
        },
    },

    {
        test: {
            category: 'cars',
            currency: 'RUR',
            section: 'all',
            search_tag: 'some_tag',
        },
        result: {
            mmm: 0,
            main: 0,
            extended: 1,
            panel: 0,
        },
    },

    {
        test: { category: 'cars', currency: 'RUR', section: 'all', online_view: true },
        result: {
            mmm: 0,
            main: 0,
            extended: 1,
            panel: 0,
        },
    },

    {
        test: { category: 'moto', currency: 'RUR', section: 'all', moto_category: 'motorsycle' },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 0,
        },
    },

    {
        test: { category: 'moto', currency: 'RUR', section: 'all', moto_category: 'motorcycle', km_age_to: 1000 },
        result: {
            mmm: 0,
            main: 0,
            extended: 1,
            panel: 0,
        },
    },

    {
        test: { category: 'moto', currency: 'RUR', section: 'all', moto_category: 'motorcycle', displacement_from: 1000 },
        result: {
            mmm: 0,
            main: 1,
            extended: 0,
            panel: 0,
        },
    },

    {
        test: { category: 'trucks', currency: 'RUR', section: 'all', trucks_category: 'lcv', displacement_from: 1000 },
        result: {
            mmm: 0,
            main: 0,
            extended: 1,
            panel: 0,
        },
    },

    {
        test: { category: 'trucks', currency: 'RUR', section: 'all', trucks_category: 'lcv', only_nds: true, top_days: 1 },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 2,
        },
    },

    {
        test: { category: 'cars', currency: 'RUR', section: 'all', on_credit: true, top_days: 1, with_autoru_expert: 'ONLY' },
        result: {
            mmm: 0,
            main: 1,
            extended: 0,
            panel: 2,
        },
    },

    {
        test: { category: 'cars', currency: 'RUR', section: 'used', sort: 'fresh_relevance_1-desc', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' },
        result: {
            mmm: 0,
            main: 0,
            extended: 2,
            panel: 0,
        },
    },

    {
        test: { category: 'cars', currency: 'RUR', section: 'used', sort: 'fresh_relevance_1-desc', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' },
        result: {
            mmm: 0,
            main: 0,
            extended: 0,
            panel: 0,
        },
        experiments: { ABT_VS_678_PESSIMIZATION_BEATEN: true },
    },

    {
        test: { category: 'cars', currency: 'RUR', section: 'new', sort: 'fresh_relevance_1-desc', damage_group: 'ANY', customs_state_group: 'DOESNT_MATTER' },
        result: {
            mmm: 0,
            main: 0,
            extended: 2,
            panel: 0,
        },
        experiments: { ABT_VS_678_PESSIMIZATION_BEATEN: true },
    },

];

TESTS.forEach((testCase) => {
    const experimentsText = testCase.experiments ? ` с экспами ${ Object.keys(testCase.experiments).join() }` : '';

    it(`должен вернуть ${ JSON.stringify(testCase.result) } для ${ JSON.stringify(testCase.test) } ${ experimentsText }`, () => {
        const mockStore = {
            listing: {
                data: {
                    search_parameters: testCase.test,
                },
            },
            ...testCase.experiments && {
                config: { data: { experimentsData: { experiments: testCase.experiments } } },
            },
        };
        expect(getSearchParametersCounters(mockStore)).toEqual(testCase.result);
    });
});
