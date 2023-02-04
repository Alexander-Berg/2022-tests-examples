const seoTags = require('auto-core/data/dicts/seoTags').map(tag => tag.name);
const commonCases = require('../susanin.common.testcases');

module.exports = [
    ...commonCases,
    {
        url: '/cars/audi/2020-year/all/do-100000/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                'do': '100000',
                year: '2020-year',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/audi/a3/2020-year/all/do-100000/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a3',
                'do': '100000',
                year: '2020-year',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/audi/a3-e_tron/2020-year/all/do-100000/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a3',
                nameplate_name: 'e_tron',
                'do': '100000',
                year: '2020-year',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/audi/a3-e_tron/2020-year/all/do-100000/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a3',
                nameplate_name: 'e_tron',
                'do': '100000',
                year: '2020-year',
                section: 'all',
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
        url: '/cars/all/tag/' + seoTags[0] + '/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                section: 'all',
                search_tag: [ seoTags[0] ],
            },
        },
    },
    {
        url: '/cars/audi/all/?search_tag=' + seoTags[0],
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                section: 'all',
                mark: 'audi',
                search_tag: [ seoTags[0] ],
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
        url: '/atv/2020-year/all/do-100000/',
        route: {
            routeName: 'moto-listing',
            routeParams: {
                category: 'moto',
                moto_category: 'atv',
                year: '2020-year',
                'do': '100000',
                section: 'all',
            },
        },
    },
    {
        url: '/atv/all/do-100000/',
        route: {
            routeName: 'moto-listing',
            routeParams: {
                category: 'moto',
                moto_category: 'atv',
                'do': '100000',
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
        url: '/bus/2020-year/all/do-100000/',
        route: {
            routeName: 'commercial-listing',
            routeParams: {
                category: 'trucks',
                trucks_category: 'bus',
                'do': '100000',
                year: '2020-year',
                section: 'all',
            },
        },
    },
    {
        url: '/bus/all/do-100000/',
        route: {
            routeName: 'commercial-listing',
            routeParams: {
                category: 'trucks',
                trucks_category: 'bus',
                'do': '100000',
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
        url: '/reviews/',
        route: {
            routeName: 'reviews-index',
            routeParams: {
                parent_category: 'cars',
            },
        },
    },
    {
        url: '/reviews/cars/all/',
        route: {
            routeName: 'reviews-listing-all',
            routeParams: {
                parent_category: 'cars',
            },
        },
    },
    {
        url: '/reviews/cars/audi/',
        route: {
            routeName: 'reviews-listing-cars',
            routeParams: {
                parent_category: 'cars',
                mark: 'audi',
            },
        },
    },
    {
        url: '/reviews/moto/',
        route: {
            routeName: 'reviews-index',
            routeParams: {
                parent_category: 'moto',
            },
        },
    },
    {
        url: '/reviews/moto/all/',
        route: {
            routeName: 'reviews-listing-all',
            routeParams: {
                parent_category: 'moto',
            },
        },
    },
    {
        url: '/reviews/moto/atv/',
        route: {
            routeName: 'reviews-listing',
            routeParams: {
                category: 'atv',
                parent_category: 'moto',
            },
        },
    },
    {
        url: '/reviews/trucks/',
        route: {
            routeName: 'reviews-index',
            routeParams: {
                parent_category: 'trucks',
            },
        },
    },
    {
        url: '/reviews/trucks/all/',
        route: {
            routeName: 'reviews-listing-all',
            routeParams: {
                parent_category: 'trucks',
            },
        },
    },
    {
        url: '/reviews/trucks/artic/',
        route: {
            routeName: 'reviews-listing',
            routeParams: {
                category: 'artic',
                parent_category: 'trucks',
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
