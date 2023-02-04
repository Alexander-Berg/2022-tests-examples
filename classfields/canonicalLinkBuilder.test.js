jest.mock('auto-core/configs/base-domain', () => {
    return 'auto.ru';
});

const canonicalLinkBuilder = require('./canonicalLinkBuilder');

const DATA = [
    [
        'listing',
        {
            category: 'cars',
            section: 'all',
            mark: 'audi',
            model: 'a3',
            super_gen: 20785010,
            configuration_id: 20785541,
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'listing',
        {
            isMobile: true,
            category: 'cars',
            section: 'all',
            mark: 'audi',
            model: 'a3',
            super_gen: 20785010,
            configuration_id: 20785541,
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'card-group',
        {
            category: 'cars',
            section: 'new',
            mark: 'audi',
            model: 'a3',
            configuration_id: '20785541',
            super_gen: '20785010',
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'card',
        {
            category: 'cars',
            section: 'used',
            mark: 'audi',
            model: 'a3',
            sale_id: '123',
            sale_hash: '321',
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'listing',
        {
            category: 'atv',
            section: 'all',
            mark: 'apollo',
            model: 'elite',
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'listing',
        {
            category: 'lcv',
            section: 'all',
            mark: 'lada',
            model: 'largus',
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
        {
            noDomain: true,
        },
    ],
    [
        'listing',
        {
            category: 'cars',
            section: 'all',
            catalog_filter: [
                {
                    vendor: 'VENDOR1',
                },
            ],
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'catalog',
        {
            category: 'cars',
            mark: 'audi',
            model: 'a3',
            super_gen: 20785010,
            configuration_id: 20785541,
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'incorrect',
        {
            category: 'cars',
            mark: 'audi',
            model: 'a3',
            super_gen: 20785010,
            configuration_id: 20785541,
        },
        {
            geo: {
                baseDomain: 'auto.ru',
                geoAlias: '',
                geoIds: [],
                geoOverride: false,
            },
        },
    ],
    [
        'index',
    ],
    [
        'versus',
        {
            first_mark: 'ford',
            first_model: 'focus',
            first_nameplate: 'active',
            second_mark: 'kia',
            second_model: 'rio',
            second_nameplate: 'x_line',
        },
    ],
];

const RESULTS = [
    'https://auto.ru/cars/audi/a3/20785010/20785541/all/',
    'https://auto.ru/cars/audi/a3/20785010/20785541/all/',
    'https://auto.ru/cars/new/group/audi/a3/20785010-20785541/',
    'https://auto.ru/cars/used/sale/audi/a3/123-321/',
    'https://auto.ru/atv/apollo/elite/all/',
    '/lcv/lada/largus/all/',
    'https://auto.ru/cars/vendor-domestic/all/',
    'https://auto.ru/catalog/cars/audi/a3/20785010/20785541/',
    '',
    'https://auto.ru/',
    'https://auto.ru/compare-cars/ford-focus-active-vs-kia-rio-x_line/',
];

it('возвращает правильные адреса', () => {
    return new Promise((done) => {
        const result = DATA.map((params) => canonicalLinkBuilder(...params));

        expect(result).toEqual(RESULTS);

        done();
    });
});
