const commonCases = require('../susanin.common.testcases');

module.exports = [
    ...commonCases,
    {
        url: '/',
        route: {
            routeName: 'index',
            routeParams: {
                category: 'cars',
            },
        },
    },
    {
        url: '/atv/',
        route: {
            routeName: 'index',
            routeParams: {
                category: 'moto',
                moto_category: 'atv',
            },
        },
    },
    {
        url: '/artic/',
        route: {
            routeName: 'index',
            routeParams: {
                category: 'trucks',
                trucks_category: 'artic',
            },
        },
    },
    {
        url: '/cars/used/sale/audi/a4/123-ac4/456-678/',
        route: {
            routeName: 'card',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a4',
                section: 'used',
                sale_id: '123',
                sale_hash: 'ac4',
                groupID: '456-678',
            },
        },
    },
    {
        url: '/cars/all/do-100000/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                'do': '100000',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/all/body-liftback/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                body_type_sef: 'body-liftback',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/all/engine-benzin/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                engine_type_sef: 'engine-benzin',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/all/drive-front_wheel/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                drive_sef: 'drive-front_wheel',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/audi/all/color-chernyj/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                section: 'all',
                color_sef: 'color-chernyj',
            },
        },
    },
    {
        url: '/cars/audi/a3/123/456/789/all/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                section: 'all',
                configuration_id: '456',
                model: 'a3',
                super_gen: '123',
                tech_param_id: '789',
            },
        },
    },
    {
        url: '/atv/all/',
        route: {
            routeName: 'moto-listing',
            routeParams: {
                category: 'moto',
                moto_category: 'atv',
                section: 'all',
            },
        },
    },
    {
        url: '/atv/audi/all/',
        route: {
            routeName: 'moto-listing',
            routeParams: {
                category: 'moto',
                moto_category: 'atv',
                mark: 'audi',
                section: 'all',
            },
        },
    },
    {
        url: '/atv/audi/a3/all/',
        route: {
            routeName: 'moto-listing',
            routeParams: {
                category: 'moto',
                moto_category: 'atv',
                mark: 'audi',
                model: 'a3',
                section: 'all',
            },
        },
    },
    {
        url: '/atv/2020-year/all/',
        route: {
            routeName: 'moto-listing',
            routeParams: {
                category: 'moto',
                moto_category: 'atv',
                year: '2020-year',
                section: 'all',
            },
        },
    },
    {
        url: '/bus/all/',
        route: {
            routeName: 'commercial-listing',
            routeParams: {
                category: 'trucks',
                trucks_category: 'bus',
                section: 'all',
            },
        },
    },
    {
        url: '/bus/audi/all/',
        route: {
            routeName: 'commercial-listing',
            routeParams: {
                category: 'trucks',
                trucks_category: 'bus',
                mark: 'audi',
                section: 'all',
            },
        },
    },
    {
        url: '/bus/audi/a3/all/',
        route: {
            routeName: 'commercial-listing',
            routeParams: {
                category: 'trucks',
                trucks_category: 'bus',
                mark: 'audi',
                model: 'a3',
                section: 'all',
            },
        },
    },
    {
        url: '/bus/2020-year/all/',
        route: {
            routeName: 'commercial-listing',
            routeParams: {
                category: 'trucks',
                trucks_category: 'bus',
                year: '2020-year',
                section: 'all',
            },
        },
    },
    {
        url: '/stats/cars/audi/a3/20785010/20785079/20785079__20794251/',
        route: {
            routeName: 'stats',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a3',
                super_gen: '20785010',
                configuration_id: '20785079',
                complectation_id: '20785079__20794251',
            },
        },
    },
    {
        url: '/garage/promo/?promo=yandex_market',
        route: {
            routeName: 'garage-promo-redirect',
            routeParams: {
                promo: 'yandex_market',
            },
        },
    },
];
