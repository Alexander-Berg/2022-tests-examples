const Update = require('immutability-helper');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const MockDate = require('mockdate');

const { getAll } = require('./index');

jest.mock('auto-core/lib/core/isDesktopApp', () => () => true);
jest.mock('auto-core/lib/core/isAmpApp', () => ({
    'default': () => false,
}));

const BASE_COMPLECTATION_MOCK = {
    data: {
        search_parameters: {
            tech_param_id: '20536620',
            complectation_id: '20940975',
        },
        complectations: [
            {
                tech_info: {
                    mark_info: {
                        code: 'AUDI',
                        name: 'Audi',
                        ru_name: 'Ауди',
                    },
                    model_info: {
                        code: 'Q3',
                        name: 'Q3',
                        ru_name: 'Ку3',
                    },
                    super_gen: {
                        id: '20293979',
                        name: 'I Рестайлинг',
                        ru_name: '1 Рестайлинг',
                    },
                    configuration: {
                        id: '20294010',
                        body_type: 'ALLROAD_5_DOORS',
                        human_name: 'Внедорожник 5 дв.',
                        body_type_group: 'ALLROAD_5_DOORS',
                        main_photo: {
                            sizes: {
                                cattouch: '//avatars.mds.yandex.net/get-verba/787013/2a0000016095f2130bbdf48f5215b874d157/cattouch',
                            },
                        },
                    },
                    tech_param: {
                        id: '20536620',
                        human_name: '2.0 AMT (180 л.с.) 4WD',
                        engine_type: 'GASOLINE',
                        transmission: 'MECHANICAL',
                    },
                    complectation: {
                        vendor_colors: [
                            {
                                photos: [
                                    {
                                        name: '34-front',
                                        sizes: {
                                            cattouch: '//avatars.mds.yandex.net/get-verba/937147/2a0000016dcf95343f30126207cd576076e2/cattouch',
                                        },
                                    },
                                ],
                            },
                        ],
                    },
                },
                complectation_id: '20940975',
                complectation_name: 'Sport',
            },
        ],
    },
};

const OFFER_MOCK = {
    category: '',
    state: {
        image_urls: [
            {
                sizes: {
                    '1200x900': '//images.mds-proxy.test.avto.ru/get-autoru-vos/2071343/df0f08c6c9f772cf4486964f4c00e2f6/1200x900',
                },
            },
        ],
    },
    vehicle_info: {},
};

describe('Группы карточек', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('должен вернуть правильные поля для группы c офферами', () => {
        const store = mockStore({
            cardGroupComplectations: Update(BASE_COMPLECTATION_MOCK, {}),
            config: {
                data: { baseDomain: 'auto.ru' },
            },
            listing: {
                data: {
                    offers: [
                        OFFER_MOCK,
                    ],
                    pagination: {},
                },
            },
            geo: {
                geoAlias: 'moskva',
                gids: [ 213 ],
                gidsInfo: [ {
                    linguistics: {
                        dative: 'Москве',
                    },
                } ],
            },
        });
        expect(getAll(store.getState(), { section: 'new', category: 'cars' })).toMatchSnapshot();
    });

    it('должен вернуть правильные поля для группы без офферов', () => {
        const store = mockStore({
            cardGroupComplectations: Update(BASE_COMPLECTATION_MOCK, {}),
            config: {
                data: { baseDomain: 'auto.ru' },
            },
            geo: {
                geoAlias: 'moskva',
                gids: [ 213 ],
                gidsInfo: [ {
                    linguistics: {
                        dative: 'Москве',
                    },
                } ],
            },
            listing: {
                data: {
                    pagination: {},
                },
            },
        });
        expect(getAll(store.getState(), { section: 'new', category: 'cars' })).toMatchSnapshot();
    });

    it('должен вернуть правильные поля для группы без офферов и без рендеров', () => {
        const store = mockStore({
            cardGroupComplectations: Update(BASE_COMPLECTATION_MOCK, {
                data: {
                    complectations: {
                        [0]: {
                            tech_info: {
                                complectation: {
                                    $unset: [ 'vendor_colors' ],
                                },
                            },
                        },
                    },
                },
            }),
            config: {
                data: { baseDomain: 'auto.ru' },
            },
            geo: {
                geoAlias: 'moskva',
                gids: [ 213 ],
                gidsInfo: [ {
                    linguistics: {
                        dative: 'Москве',
                    },
                } ],
            },
            listing: {
                data: {
                    pagination: {},
                },
            },
        });
        expect(getAll(store.getState(), { section: 'new', category: 'cars' })).toMatchSnapshot();
    });

    it('не упадет если в конфигурации пришел не поянтный body_type', () => {
        const store = mockStore({
            cardGroupComplectations: Update(BASE_COMPLECTATION_MOCK, {
                data: {
                    complectations: {
                        '0': {
                            tech_info: {
                                configuration: {
                                    body_type: { $set: 'хуй' },
                                },
                            },
                        },
                    },
                },
            }),
            config: {
                data: { baseDomain: 'auto.ru' },
            },
            geo: {
                geoAlias: 'moskva',
                gids: [ 213 ],
                gidsInfo: [ {
                    linguistics: {
                        dative: 'Москве',
                    },
                } ],
            },
            listing: {
                data: {
                    pagination: {},
                },
            },
        });
        const result = getAll(store.getState(), { section: 'new', category: 'cars' });

        expect(result.title).not.toBeUndefined();
    });

    it('должен вернуть правильный каноникл, для страницы комплектация/сравнение', () => {
        const canonical = 'https://autoru_frontend.base_domain/cars/new/group/audi/q3/20293979-20294010/options/compare/';
        const store = mockStore({
            cardGroupComplectations: Update(BASE_COMPLECTATION_MOCK, {}),
            config: {
                data: { baseDomain: 'auto.ru' },
            },
            listing: {
                data: {
                    pagination: {},
                },
            },
            router: {
                current: {
                    name: 'card-group-options',
                    params: { tab_id: 'compare' },
                },
            },
        });
        expect(getAll(store.getState(), { section: 'new', category: 'cars' }).canonical).toEqual(canonical);
    });

    it('должен вернуть правильный каноникл для страницы комплектация/список', () => {
        const canonical = 'https://autoru_frontend.base_domain/cars/new/group/audi/q3/20293979-20294010/options/';
        const store = mockStore({
            cardGroupComplectations: Update(BASE_COMPLECTATION_MOCK, {}),
            config: {
                data: { baseDomain: 'auto.ru' },
            },
            listing: {
                data: {
                    pagination: {},
                },
            },
            router: {
                current: {
                    name: 'card-group-options',
                },
            },
        });
        expect(getAll(store.getState(), { section: 'new', category: 'cars' }).canonical).toEqual(canonical);
    });

    it('должен вернуть правильный каноникл для страницы групп', () => {
        const canonical = 'https://autoru_frontend.base_domain/moskva/cars/new/group/audi/q3/20293979-20294010/';
        const store = mockStore({
            cardGroupComplectations: Update(BASE_COMPLECTATION_MOCK, {}),
            config: {
                data: {
                    baseDomain: 'auto.ru',
                    pageParams: {
                        category: 'cars',
                        section: 'new',
                    },
                },
            },
            listing: {
                data: {
                    pagination: {},
                },
            },
            router: {
                current: {
                    name: 'card-group',
                },
            },
            geo: {
                geoAlias: 'moskva',
                gids: [ 213 ],
                gidsInfo: [ {
                    linguistics: {
                        dative: 'Москве',
                    },
                } ],
            },
        });
        expect(getAll(store.getState(), {}).canonical).toEqual(canonical);
    });
});
