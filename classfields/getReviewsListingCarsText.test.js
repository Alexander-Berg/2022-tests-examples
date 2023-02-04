const getReviewsListingCarsText = require('./getReviewsListingCarsText');
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

it('Должен сгенерировать текст с маркой и моделью и окрулить рейтинг до десятых', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        reviews: {
            params: {
                mark: 'FORD',
                model: 'ECOSPORT',
                category: 'CARS',
            },
            pagination: {
                total_offers_count: 1,
            },
        },
        reviewsSummary: {
            data: {
                rating: {
                    ratings: [
                        {
                            name: 'total',
                            value: 4.67,
                        },
                    ],
                },
            },
        },
    };

    const snippet = getReviewsListingCarsText(state);

    expect(snippet).toEqual('Отзывы владельцев об автомобилях Ford EcoSport. Общая оценка Форд ЭкоСпорт - 4.7');
});

it('Должен сгенерировать текст только с маркой и окрулить рейтинг до десятых', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        reviews: {
            params: {
                mark: 'FORD',
                category: 'CARS',
            },
            pagination: {
                total_offers_count: 1,
            },
        },
        reviewsSummary: {
            data: {
                rating: {
                    ratings: [
                        {
                            name: 'total',
                            value: 4.632317,
                        },
                    ],
                },
            },
        },
    };

    const snippet = getReviewsListingCarsText(state);

    expect(snippet).toEqual('Отзывы владельцев об автомобилях Ford. Общая оценка Форд - 4.6');
});

it('Должен сгенерировать текст со всеми параметрами и окрулить рейтинг до десятых', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        reviews: {
            params: {
                mark: 'FORD',
                model: 'ECOSPORT',
                super_gen: '21126901',
                transmission: 'ROBOT',
                year_from: '2018',
                year_to: '2018',
                category: 'CARS',
            },
            pagination: {
                total_offers_count: 1,
            },
        },
        reviewsSummary: {
            data: {
                rating: {
                    ratings: [
                        {
                            name: 'total',
                            value: 3,
                        },
                    ],
                },
            },
        },
        reviewsFeatures: {
            data: {
                features: {
                    positive: [ {}, {}, {}, {} ],
                    negative: [ {}, {} ],
                },
            },
        },
    };

    const snippet = getReviewsListingCarsText(state);
    const res = 'Отзывы владельцев об автомобилях Ford EcoSport I Рестайлинг 2018. ' +
                'Общая оценка Форд ЭкоСпорт I Рестайлинг - 3.0, 4 плюса и 2 минуса среди самых актуальных отзывов';
    expect(snippet).toEqual(res);
});

it('Ничего не должен вернуть, если это не CARS', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        reviews: {
            params: {
                mark: 'BMW',
                category: 'MOTO',
            },
            pagination: {
                total_offers_count: 1,
            },
        },
    };

    const snippet = getReviewsListingCarsText(state);

    expect(snippet).toEqual('');
});

it('Ничего не должен вернуть, если нет отзывов', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        reviews: {
            params: {
                mark: 'BMW',
                category: 'CARS',
            },
            pagination: {
                total_offers_count: 0,
            },
        },
    };

    const snippet = getReviewsListingCarsText(state);

    expect(snippet).toEqual('');
});
