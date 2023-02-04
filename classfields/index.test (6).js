const dealers = require('./index');

const mockSeoParams = (params) => {
    const {
        category,
        is_oficial: isOfficial,
        mmmInfo,
        moto_category: motoCategory,
        offersCount,
        section,
        trucks_category: trucksCategory,
    } = params;
    const seoParams = {
        salonInfo: {
            is_oficial: isOfficial,
            name: 'Тестовый дилер',
            car_marks: [
                {
                    name: 'Tachanka',
                },
            ],
            place: {
                region_info: {
                    prepositional: 'Москве',
                },
            },
        },
        offersCount: offersCount,
        priceRange: {
            min: {
                rur_price: 123000,
            },
        },
        mmmInfo: mmmInfo || {},
        params: {
            category,
            section: section || 'all',
            moto_category: motoCategory || null,
            trucks_category: trucksCategory || null,
        },
    };

    return seoParams;
};

/* eslint-disable max-len */
const H1_TESTS = [
    // официальный дилер авто
    {
        name: 'Официальный дилер авто с фильтром "Все"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'all',
            offersCount: 999,
        }),
        result: 'Автомобили в наличии',
    },
    {
        name: 'Официальный дилер авто с фильтром "Новые"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'new',
            offersCount: 999,
        }),
        result: 'Новые автомобили в наличии',
    },
    {
        name: 'Официальный дилер авто с фильтром "С пробегом"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'used',
            offersCount: 999,
        }),
        result: 'Автомобили с пробегом в наличии',
    },
    // обычный дилер авто
    {
        name: 'Обычный дилер авто без выбора марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: 999,
        }),
        result: 'Автомобили с пробегом в наличии',
    },
    {
        name: 'Обычный дилер авто с выбором марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: 999,
            mmmInfo: {
                mark: {
                    name: 'BMW',
                },
            },
        }),
        result: 'Автомобили с пробегом BMW в наличии',
    },
    // официальный дилер мото
    {
        name: 'Официальный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'moto',
            moto_category: 'scooters',
            offersCount: 999,
        }),
        result: 'Скутеры в наличии',
    },
    // обычный дилер мото
    {
        name: 'Обычный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'moto',
            moto_category: 'scooters',
            offersCount: 999,
        }),
        result: 'Скутеры в наличии',
    },
    // официальный дилер комтс
    {
        name: 'Официальный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'trucks',
            moto_category: 'lcv',
            offersCount: 999,
        }),
        result: 'Лёгкий коммерческий транспорт в наличии',
    },
    // обычный дилер комтс
    {
        name: 'Обычный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'trucks',
            moto_category: 'lcv',
            offersCount: 999,
        }),
        result: 'Лёгкий коммерческий транспорт в наличии',
    },
];

const TITLE_TESTS = [
    // официальный дилер авто
    {
        name: 'Официальный дилер авто с фильтром "Все"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'all',
            offersCount: 999,
        }),
        result: 'Официальный дилер Тестовый дилер в Москве - 999 автомобилей в наличии у официального дилера Tachanka',
    },
    {
        name: 'Официальный дилер авто с фильтром "Новые"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'new',
            offersCount: 999,
        }),
        result: 'Официальный дилер Тестовый дилер в Москве - 999 новых автомобилей в наличии у официального дилера Tachanka',
    },
    {
        name: 'Официальный дилер авто с фильтром "С пробегом"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'used',
            offersCount: 999,
        }),
        result: 'Официальный дилер Тестовый дилер в Москве - 999 б/у автомобилей в наличии у официального дилера Tachanka',
    },
    // обычный дилер авто
    {
        name: 'Обычный дилер авто без выбора марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: 999,
        }),
        result: 'Дилер Тестовый дилер в Москве - 999 б/у автомобилей в наличии у дилера',
    },
    {
        name: 'Обычный дилер авто с выбором марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: 999,
            mmmInfo: {
                mark: {
                    name: 'BMW',
                },
            },
        }),
        result: 'Дилер Тестовый дилер в Москве - 999 б/у автомобилей BMW в наличии у дилера',
    },
    {
        name: 'Обычный дилер авто с выбором марки в фильтре и без офферов',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: undefined,
            mmmInfo: {
                mark: {
                    name: 'BMW',
                },
            },
        }),
        result: 'Дилер Тестовый дилер в Москве -  б/у автомобили BMW в наличии у дилера',
    },
    // официальный дилер мото
    {
        name: 'Официальный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'moto',
            moto_category: 'scooters',
            offersCount: 999,
        }),
        result: 'Официальный дилер Тестовый дилер в Москве - 999 скутеров в наличии у официального дилера Tachanka',
    },
    // обычный дилер мото
    {
        name: 'Обычный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'moto',
            moto_category: 'scooters',
            offersCount: 999,
        }),
        result: 'Дилер Тестовый дилер в Москве - 999 скутеров в наличии у дилера',
    },
    // официальный дилер комтс
    {
        name: 'Официальный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'trucks',
            moto_category: 'lcv',
            offersCount: 999,
        }),
        result: 'Официальный дилер Тестовый дилер в Москве - 999 предложений лёгкого коммерческого транспорта в наличии у официального дилера Tachanka',
    },
    // обычный дилер комтс
    {
        name: 'Обычный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'trucks',
            moto_category: 'lcv',
            offersCount: 999,
        }),
        result: 'Дилер Тестовый дилер в Москве - 999 предложений лёгкого коммерческого транспорта в наличии у дилера',
    },
];

const DESC_TESTS = [
    // официальный дилер авто
    {
        name: 'Официальный дилер авто с фильтром "Все"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'all',
        }),
        result: 'Акции, скидки и специальные предложения на автомобили у официального дилера Тестовый дилер. Купить автомобиль Tachanka в Москве',
    },
    {
        name: 'Официальный дилер авто с фильтром "Новые"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'new',
        }),
        result: 'Акции, скидки и специальные предложения на автомобили у официального дилера Тестовый дилер. Купить новый автомобиль Tachanka в Москве',
    },
    {
        name: 'Официальный дилер авто с фильтром "С пробегом"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'used',
        }),
        result: 'Акции, скидки и специальные предложения на автомобили у официального дилера Тестовый дилер. Купить подержанный автомобиль Tachanka в Москве',
    },
    // обычный дилер авто
    {
        name: 'Обычный дилер авто без выбора марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
        }),
        result: 'Акции, скидки и специальные предложения на автомобили у дилера Тестовый дилер. Купить подержанный автомобиль в Москве',
    },
    {
        name: 'Обычный дилер авто с выбором марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            mmmInfo: {
                mark: {
                    name: 'BMW',
                },
            },
        }),
        result: 'Акции, скидки и специальные предложения на автомобили у дилера Тестовый дилер. Купить подержанный автомобиль BMW в Москве',
    },
    // официальный дилер мото
    {
        name: 'Официальный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'moto',
            moto_category: 'scooters',
        }),
        result: 'Акции, скидки и специальные предложения на скутеры у официального дилера Тестовый дилер. Купить скутер в Москве',
    },
    // обычный дилер мото
    {
        name: 'Обычный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'moto',
            moto_category: 'scooters',
        }),
        result: 'Акции, скидки и специальные предложения на скутеры у дилера Тестовый дилер. Купить скутер в Москве',
    },
    // официальный дилер комтс
    {
        name: 'Официальный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'trucks',
            moto_category: 'lcv',
        }),
        result: 'Акции, скидки и уникальные предложения на лёгкий коммерческий транспорт у официального дилера Тестовый дилер в Москве',
    },
    // обычный дилер комтс
    {
        name: 'Обычный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'trucks',
            moto_category: 'lcv',
        }),
        result: 'Акции, скидки и уникальные предложения на лёгкий коммерческий транспорт у дилера Тестовый дилер в Москве',
    },
];

const SEO_TEXT_TESTS = [
    {
        name: 'Официальный дилер авто с фильтром "Все"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'all',
            offersCount: 999,
        }),
        result: 'Купить автомобиль у официального дилера Тестовый дилер в Москве - 999 автомобилей в наличии с уникальными акциями и специальными условиями. Выберите свой автомобиль Tachanka от 123 000 рублей.',
    },
    {
        name: 'Официальный дилер авто с фильтром "Новые"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'new',
            offersCount: 999,
        }),
        result: 'Купить новый автомобиль у официального дилера Тестовый дилер в Москве - 999 автомобилей в наличии с уникальными акциями и специальными условиями. Выберите свой автомобиль Tachanka от 123 000 рублей.',
    },
    {
        name: 'Официальный дилер авто с фильтром "С пробегом"',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'cars',
            section: 'used',
            offersCount: 999,
        }),
        result: 'Купить подержанный автомобиль у официального дилера Тестовый дилер в Москве - 999 автомобилей в наличии с уникальными акциями и специальными условиями. Выберите свой автомобиль Tachanka от 123 000 рублей.',
    },
    // обычный дилер авто
    {
        name: 'Обычный дилер авто без выбора марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: 999,
        }),
        result: 'Купить подержанный автомобиль у дилера Тестовый дилер в Москве - 999 автомобилей в наличии с уникальными акциями и специальными условиями. Выберите свой автомобиль от 123 000 рублей.',
    },
    {
        name: 'Обычный дилер авто с выбором марки в фильтре',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: 999,
            mmmInfo: {
                mark: {
                    name: 'BMW',
                },
            },
        }),
        result: 'Купить подержанный автомобиль у дилера Тестовый дилер в Москве - 999 автомобилей в наличии с уникальными акциями и специальными условиями. Выберите свой автомобиль BMW от 123 000 рублей.',
    },
    {
        name: 'Обычный дилер авто с выбором марки в фильтре и без офферов',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'cars',
            offersCount: undefined,
            mmmInfo: {
                mark: {
                    name: 'BMW',
                },
            },
        }),
        result: 'Купить подержанный автомобиль у дилера Тестовый дилер в Москве -  автомобили в наличии с уникальными акциями и специальными условиями. Выберите свой автомобиль BMW от 123 000 рублей.',
    },
    // официальный дилер мото
    {
        name: 'Официальный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'moto',
            moto_category: 'scooters',
            offersCount: 999,
        }),
        result: 'Купить скутеры у официального дилера Тестовый дилер в Москве - 999 скутеров в наличии с уникальными акциями и специальными условиями.',
    },
    // обычный дилер мото
    {
        name: 'Обычный дилер мото',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'moto',
            moto_category: 'scooters',
            offersCount: 999,
        }),
        result: 'Купить скутеры у дилера Тестовый дилер в Москве - 999 скутеров в наличии с уникальными акциями и специальными условиями.',
    },
    // официальный дилер комтс
    {
        name: 'Официальный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: true,
            category: 'trucks',
            moto_category: 'lcv',
            offersCount: 999,
        }),
        result: 'Купить лёгкий коммерческий транспорт у официального дилера Тестовый дилер в Москве - 999 предложений лёгкого коммерческого транспорта в наличии с уникальными акциями и специальными условиями.',
    },
    // обычный дилер комтс
    {
        name: 'Обычный дилер комтс',
        seoParams: mockSeoParams({
            is_oficial: false,
            category: 'trucks',
            moto_category: 'lcv',
            offersCount: 999,
        }),
        result: 'Купить лёгкий коммерческий транспорт у дилера Тестовый дилер в Москве - 999 предложений лёгкого коммерческого транспорта в наличии с уникальными акциями и специальными условиями.',
    },
];

describe('getH1', () => {
    H1_TESTS.forEach((test) => it(test.name, () => {
        expect(dealers.card.getH1(test.seoParams)).toEqual(test.result);
    }));
});

describe('getTitle', () => {
    TITLE_TESTS.forEach((test) => it(test.name, () => {
        expect(dealers.card.getTitle(test.seoParams)).toEqual(test.result);
    }));
});

describe('getDescription', () => {
    DESC_TESTS.forEach((test) => it(test.name, () => {
        expect(dealers.card.getDescription(test.seoParams)).toEqual(test.result);
    }));
});

describe('getSeoText', () => {
    SEO_TEXT_TESTS.forEach((test) => it(test.name, () => {
        expect(dealers.card.getSeoText(test.seoParams)).toEqual(test.result);
    }));
});

describe('getCanonical', () => {
    it('Официальный дилер авто', () => {
        const store = {
            salonInfo: {
                data: {
                    name: 'Регион-Тыва LADA',
                    is_oficial: true,
                    code: 'region_tiva_lada',
                },
            },
            router: {
                current: {
                    params: {
                        category: 'cars',
                        section: 'all',
                        dealer_code: 'region_tiva_lada',
                    },
                },
            },
        };
        expect(dealers.card.getCanonical(store)).toEqual('/diler-oficialniy/cars/all/region_tiva_lada/');
    });

    it('Неофициальный дилер авто', () => {
        const store = {
            salonInfo: {
                data: {
                    name: 'Регион-Тыва LADA',
                    is_oficial: false,
                    code: 'region_tiva_lada',
                },
            },
            router: {
                current: {
                    params: {
                        category: 'cars',
                        section: 'all',
                        dealer_code: 'region_tiva_lada',
                    },
                },
            },
        };
        expect(dealers.card.getCanonical(store)).toEqual('/diler/cars/all/region_tiva_lada/');
    });
});
