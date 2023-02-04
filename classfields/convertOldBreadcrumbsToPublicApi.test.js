const convertOldBreadcrumbsToPublicApi = require('./convertOldBreadcrumbsToPublicApi');

it('должен сконвертировать старые марки в новый формат', () => {
    const breadcrumbs = {
        AS: {
            'cyrillic-name': 'АС',
            id: 'AS',
            name: 'АС',
        },
        FORD: {
            'cyrillic-name': 'Ford',
            id: 'FORD',
            name: 'Ford',
        },
    };

    const breadcrumbsPublicApi = [
        {
            entities: [
                { cyrillic_name: 'АС', id: 'AS', name: 'АС' },
                { cyrillic_name: 'Ford', id: 'FORD', name: 'Ford' },
            ],
            level: 'MARK_LEVEL',
            meta_level: 'MARK_LEVEL',
            levelFilterParams: {},
        },
    ];

    expect(convertOldBreadcrumbsToPublicApi(breadcrumbs)).toEqual(breadcrumbsPublicApi);
});

it('должен сконвертировать старые марки+модели в новый формат', () => {
    const breadcrumbs = {
        AS: {
            'cyrillic-name': 'АС',
            id: 'AS',
            name: 'АС',
        },
        FORD: {
            'cyrillic-name': 'Ford',
            id: 'FORD',
            name: 'Ford',
            models: {
                TRANSIT_CONNECT: {
                    id: 'TRANSIT_CONNECT',
                    name: 'Transit Connect',
                    count: 9,
                    'cyrillic-name': 'Transit Connect',
                },
                TRANSIT_CUSTOM: {
                    id: 'TRANSIT_CUSTOM',
                    name: 'Transit Custom',
                    count: 0,
                    'cyrillic-name': 'Transit Custom',
                },
            },
        },
    };

    const breadcrumbsPublicApi = [
        {
            entities: [
                { cyrillic_name: 'Transit Connect', id: 'TRANSIT_CONNECT', name: 'Transit Connect', count: 9 },
                { cyrillic_name: 'Transit Custom', id: 'TRANSIT_CUSTOM', name: 'Transit Custom', count: 0 },
            ],
            level: 'MODEL_LEVEL',
            meta_level: 'MODEL_LEVEL',
            levelFilterParams: { mark: 'FORD' },
        },
        {
            entities: [
                { cyrillic_name: 'АС', id: 'AS', name: 'АС' },
                { cyrillic_name: 'Ford', id: 'FORD', name: 'Ford' },
            ],
            level: 'MARK_LEVEL',
            meta_level: 'MARK_LEVEL',
            levelFilterParams: {},
        },
    ];

    expect(convertOldBreadcrumbsToPublicApi(breadcrumbs)).toEqual(breadcrumbsPublicApi);
});
