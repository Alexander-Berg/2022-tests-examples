const getReviewsMeta = require('./reviews');

const review = require('autoru-frontend/mockData/state/review.mock');
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mock').default;
const configMock = require('auto-core/react/dataDomain/config/mock').default;
const geoMock = require('auto-core/react/dataDomain/geo/mocks/geo.mock');

it('должен генерить мету для индекса отзывов', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.value(),
        config: configMock.withPageType('reviews-index').value(),
        geo: geoMock,
        reviews: {
            params: {
                category: 'CARS',
            },
        },
    };
    expect(getReviewsMeta(state)).toMatchSnapshot();
});

it('должен генерить мету для индекса отзывов мото', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.value(),
        config: configMock.withPageType('reviews-index').value(),
        geo: geoMock,
        reviews: {
            params: {
                category: 'MOTO',
            },
        },
    };
    expect(getReviewsMeta(state)).toMatchSnapshot();
});

it('должен генерить мету для карточки отзыва', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.value(),
        config: configMock.withPageType('review-card-cars').value(),
        geo: geoMock,
        review: {
            data: review,
        },
        reviews: {
            params: {
                mark: 'FORD',
                model: 'ECOSPORT',
                super_gen: '20104320',
                reviewId: '4114858725813482709',
                category: 'CARS',
                catalog_filter: [ {
                    mark: 'FORD',
                    model: 'ECOSPORT',
                    generation: '20104320',
                } ],
            },
        },
    };
    expect(getReviewsMeta(state)).toMatchSnapshot();
});

it('должен генерить мету для листинга отзывов', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.value(),
        config: configMock.withPageType('reviews-listing-cars').value(),
        geo: geoMock,
        reviews: {
            params: {
                mark: 'FORD',
                model: 'ECOSPORT',
                super_gen: [ '20104320', '21126901' ],
                category: 'CARS',
                catalog_filter: [
                    { mark: 'FORD', model: 'ECOSPORT', generation: '20104320' },
                    { mark: 'FORD', model: 'ECOSPORT', generation: '21126901' },
                ],
            },
        },
    };
    expect(getReviewsMeta(state)).toMatchSnapshot();
});

it('должен генерить мету для листинга отзывов мото', () => {
    const state = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.withCategoryBreadcrumbs({
            category: 'moto',
            sub_category: 'atv',
        }).value(),
        config: configMock.withPageType('reviews-listing').value(),
        geo: geoMock,
        reviews: {
            params: {
                mark: 'BRP',
                model: 'COMMANDER_1000',
                category: 'MOTO',
                sub_category: 'ATV',
                catalog_filter: [
                    { mark: 'BRP', model: 'COMMANDER_1000' },
                ],
            },
        },
    };
    expect(getReviewsMeta(state)).toMatchSnapshot();
});
