const Update = require('immutability-helper');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const { getAll } = require('./index');

jest.mock('auto-core/lib/core/isDesktopApp', () => () => true);

it('должен вернуть пустой объект, если нет карточки и нет информации про комплектации', () => {
    const store = mockStore({});
    expect(getAll(store.getState(), {})).toMatchSnapshot();
});

it('не добавит цену в заголовок и описание, если оффер не активен', () => {
    const card = cloneOfferWithHelpers(cardMock).withStatus('INACTIVE').value();
    const store = mockStore({
        card: card,
        cardGroupComplectations: { data: {} },
        config: {
            data: { baseDomain: 'auto.ru' },
        },
    });
    const result = getAll(store.getState(), {});

    expect(result.title).not.toMatch(/855\s000/);
    expect(result.ogTitle).not.toMatch(/855\s000/);
    expect(result.description).not.toMatch(/855\s000/);
    expect(result.ogDescription).not.toMatch(/855\s000/);
});

describe('card cars', () => {
    describe('б/у частник', () => {
        const BASE_CARD_MOCK = {
            category: 'cars',
            color_hex: '040001',
            documents: {
                year: 2017,
            },
            id: '12345',
            hash: 'abde',
            price_info: {
                RUR: 2050000,
            },
            section: 'used',
            state: {
                mileage: 12345,
            },
            vehicle_info: {
                mark_info: {
                    code: 'AUDI',
                    name: 'Audi',
                    ru_name: 'Ауди',
                },
                model_info: {
                    code: 'ALLROAD',
                    name: 'A6 allroad',
                    ru_name: 'А6 олроуд',
                },
                super_gen: {
                    id: '1',
                    name: 'III (C7)',
                },
                configuration: {
                    body_type: 'WAGON_5_DOORS',
                },
                tech_param: {
                    human_name: '3.0d AMT (245 л.с.) 4WD',
                    engine_type: 'GASOLINE',
                    transmission: 'MECHANICAL',
                    power: 100,
                },
            },
            status: 'ACTIVE',
        };

        it('должен вернуть правильные поля для карточки без фото', () => {
            const store = mockStore({
                card: Update(BASE_CARD_MOCK, {}),
                cardGroupComplectations: { data: {} },
                config: {
                    data: { baseDomain: 'auto.ru' },
                },
            });
            expect(getAll(store.getState(), {})).toMatchSnapshot();
        });

        it('должен вернуть правильные поля для карточки с фото', () => {
            const store = mockStore({
                card: Update(BASE_CARD_MOCK, {
                    state: {
                        image_urls: {
                            $set: [
                                { sizes: { '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1338862/7fc99683f9de210678aa1dd19ae9288f/1200x900' } },
                                { sizes: { '1200x900': '//avatars.mds.yandex.net/get-autoru-all/901772/8450ff0ef3589157bbb883988b81d89e/1200x900' } },
                            ],
                        },
                    },
                }),
                cardGroupComplectations: { data: {} },
                config: {
                    data: { baseDomain: 'auto.ru' },
                },
            });
            expect(getAll(store.getState(), {})).toMatchSnapshot();
        });

        it('должен вернуть только canonical, с сылкой на листинг', () => {
            const store = mockStore({
                card: Update(BASE_CARD_MOCK, {
                    status: {
                        $set: 'BANNED',
                    },
                }),
                cardGroupComplectations: { data: {} },
            });

            expect(getAll(store.getState(), {})).toMatchSnapshot();
        });
    });

    describe('новые, дилер', () => {
        const BASE_CARD_MOCK = {
            category: 'cars',
            color_hex: '007F00',
            description: 'Дополнительные опции: Многофункциональный спортивный кожаный руль, дизайн',
            documents: {
                year: 2018,
            },
            id: '12345',
            hash: 'abde',
            price_info: {
                RUR: 5129000,
            },
            section: 'new',
            seller: {
                name: 'АЦ Беляево',
                location: {
                    address: 'Севастопольский проспект, 56 А.',
                    region_info: {
                        id: '213',
                        name: 'Москва',
                        prepositional: 'Москве',
                        preposition: 'в',
                    },
                },
            },
            seller_type: 'COMMERCIAL',
            state: {
                image_urls: [
                    { sizes: { '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1338862/7fc99683f9de210678aa1dd19ae9288f/1200x900' } },
                ],
                mileage: 0,
            },
            vehicle_info: {
                mark_info: {
                    code: 'AUDI',
                    name: 'Audi',
                    ru_name: 'Ауди',
                },
                model_info: {
                    code: 'A7',
                    name: 'A7',
                    ru_name: 'А7',
                },
                super_gen: {
                    id: '112451223',
                    name: 'II',
                },
                configuration: {
                    id: '123123512123',
                    body_type: 'LIFTBACK',
                },
                complectation: {
                    id: '20940975',
                },
                tech_param: {
                    id: '20536620',
                    nameplate: '55 TFSI',
                    human_name: '55 TFSI 3.0 AMT (340 л.с.) 4WD',
                    engine_type: 'GASOLINE',
                    transmission: 'MECHANICAL',
                    power: 122,
                },
            },
            status: 'ACTIVE',
        };

        it('должен вернуть правильные поля для карточки c фото', () => {
            const store = mockStore({
                card: Update(BASE_CARD_MOCK, {}),
                cardGroupComplectations: { data: {} },
                config: {
                    data: { baseDomain: 'auto.ru' },
                },
            });
            expect(getAll(store.getState(), {})).toMatchSnapshot();
        });

        it('должен вернуть правильные поля для проданной карточки c фото', () => {
            const store = mockStore({
                card: Update(BASE_CARD_MOCK, {
                    status: {
                        $set: 'BANNED',
                    },
                }),
                cardGroupComplectations: { data: {} },
            });
            expect(getAll(store.getState(), {})).toMatchSnapshot();
        });
    });
});

describe('card moto', () => {
    describe('б/у частник', () => {
        const BASE_CARD_MOCK = {
            category: 'moto',
            color_hex: '040001',
            documents: {
                year: 2017,
            },
            id: '12345',
            hash: 'abde',
            price_info: {
                RUR: 2050000,
            },
            section: 'used',
            state: {
                mileage: 12345,
            },
            vehicle_info: {
                moto_category: 'atv',
                mark_info: {
                    code: 'STELS',
                    name: 'Stels',
                },
                model_info: {
                    code: 'ATV_500',
                    name: 'ATV_500',
                },
                displacement: 500,
                engine: 'GASOLINE_CARBURETOR',
                transmission: 'VARIATOR',
                horse_power: 122,
            },
            status: 'ACTIVE',
        };

        it('должен вернуть правильные поля для карточки с фото', () => {
            const store = mockStore({
                card: Update(BASE_CARD_MOCK, {
                    state: {
                        image_urls: {
                            $set: [
                                { sizes: { '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1338862/7fc99683f9de210678aa1dd19ae9288f/1200x900' } },
                                { sizes: { '1200x900': '//avatars.mds.yandex.net/get-autoru-all/901772/8450ff0ef3589157bbb883988b81d89e/1200x900' } },
                            ],
                        },
                    },
                }),
                cardGroupComplectations: { data: {} },
                config: {
                    data: { baseDomain: 'auto.ru' },
                },
            });
            expect(getAll(store.getState(), {})).toMatchSnapshot();
        });
    });

    describe('новые, дилер', () => {
        const BASE_CARD_MOCK = {
            category: 'moto',
            color_hex: '007F00',
            description: 'Дополнительные опции: Многофункциональный спортивный кожаный руль, дизайн',
            documents: {
                year: 2018,
            },
            id: '12345',
            hash: 'abde',
            price_info: {
                RUR: 5129000,
            },
            section: 'new',
            seller: {
                name: 'АЦ Беляево',
                location: {
                    address: 'Севастопольский проспект, 56 А.',
                    region_info: {
                        id: '213',
                        name: 'Москва',
                        prepositional: 'Москве',
                        preposition: 'в',
                    },
                },
            },
            seller_type: 'COMMERCIAL',
            state: {
                image_urls: [
                    { sizes: { '1200x900': '//avatars.mds.yandex.net/get-autoru-all/1338862/7fc99683f9de210678aa1dd19ae9288f/1200x900' } },
                ],
                mileage: 0,
            },
            vehicle_info: {
                moto_category: 'atv',
                mark_info: {
                    code: 'STELS',
                    name: 'Stels',
                },
                model_info: {
                    code: 'ATV_500',
                    name: 'ATV_500',
                },
                displacement: 500,
                engine: 'GASOLINE_CARBURETOR',
                transmission: 'VARIATOR',
                horse_power: 122,
            },
            status: 'ACTIVE',
        };

        it('должен вернуть правильные поля для карточки c фото', () => {
            const store = mockStore({
                card: Update(BASE_CARD_MOCK, {}),
                cardGroupComplectations: { data: {} },
                config: {
                    data: { baseDomain: 'auto.ru' },
                },
            });
            expect(getAll(store.getState(), {})).toMatchSnapshot();
        });
    });
});
