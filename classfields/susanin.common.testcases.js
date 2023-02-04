module.exports = [
    {
        url: '/cars/used/sale/audi/a4/123-ac4/',
        route: {
            routeName: 'card',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a4',
                section: 'used',
                sale_id: '123',
                sale_hash: 'ac4',
            },
        },
    },
    {
        url: '/cars/used/sale/audi/a4/123-ac4/history/',
        route: {
            routeName: 'card',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a4',
                section: 'used',
                sale_id: '123',
                sale_hash: 'ac4',
                history: 'history',
            },
        },
    },
    {
        url: '/cars/used/sale/123-ac4/',
        route: {
            routeName: 'card-old',
            routeParams: {
                category: 'cars',
                section: 'used',
                sale_id: '123',
                sale_hash: 'ac4',
            },
        },
    },
    {
        url: '/lcv/used/sale/ford/transit/123-ac4/',
        route: {
            routeName: 'card',
            routeParams: {
                category: 'lcv',
                mark: 'ford',
                model: 'transit',
                section: 'used',
                sale_id: '123',
                sale_hash: 'ac4',
            },
        },
    },
    {
        url: '/catalog/cars/datsun/mi_do/20227455/',
        route: {
            routeName: 'catalog-generation-listing',
            routeParams: {
                category: 'cars',
                mark: 'datsun',
                model: 'mi_do',
                super_gen: '20227455',
            },
        },
    },
    {
        url: '/catalog/cars/toyota/rav_4/2309591/2309592/specifications/2309592__2309594/',
        route: {
            routeName: 'catalog-card-specifications',
            routeParams: {
                category: 'cars',
                mark: 'toyota',
                model: 'rav_4',
                super_gen: '2309591',
                configuration_id: '2309592',
                complectation_id: '2309592__2309594',
            },
        },
    },
    {
        url: '/cars/all/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/audi/all/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/audi/a3/all/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'audi',
                model: 'a3',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/2020-year/all/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                year: '2020-year',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/2020-year/all/do-100000/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                year: '2020-year',
                'do': '100000',
                section: 'all',
            },
        },
    },
    {
        url: '/cars/all/do-1/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                'do': '1',
                section: 'all',
            },
        },
    },
    {
        url: '/history/a111aa11/',
        route: {
            routeName: 'proauto-report',
            routeParams: {
                history_entity_id: 'a111aa11',
            },
        },
    },
    {
        url: '/cars/ford/ecosport/all/on-credit/',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'ford',
                model: 'ecosport',
                section: 'all',
                on_credit: 'true',
            },
        },
    },
    {
        url: '/cars/ford/ecosport/20104320/all/?on_credit=true',
        route: {
            routeName: 'listing',
            routeParams: {
                category: 'cars',
                mark: 'ford',
                model: 'ecosport',
                super_gen: '20104320',
                section: 'all',
                on_credit: 'true',
            },
        },
    },
];
