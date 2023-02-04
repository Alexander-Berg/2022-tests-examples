const React = require('react');
const { Provider } = require('react-redux');
const _ = require('lodash');
const { render } = require('@testing-library/react');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const review = require('autoru-frontend/mockData/state/review.mock');

const Review = require('./Review');
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mock').default;
const reviewsFeatures = require('auto-core/react/dataDomain/reviewsFeatures/mocks/reviewsFeatures.mock');

require('@testing-library/jest-dom');

const Context = createContextProvider(contextMock);

const storeMock = {
    breadcrumbsPublicApi: breadcrumbsPublicApiMock.value(),
    reviewsSummary: {
        data: {
            count: 123,
            rating: {
                ratings: [],
            },
        },
    },
    reviewComments: { pagination: {} },
    reviewsFeatures: reviewsFeatures,
    config: {
        data: {},
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
    user: {},
    bunker: {
        'banners/index-marketing-banners': {
            electro: {
                inUse: true,
            },
        },
    },
};

it('должен отрендерить баннер электромобилей, если отзыв на электротачку', async() => {
    const store = mockStore(storeMock);
    const newReview = _.cloneDeep(review);

    newReview.item.auto.engine_type = 'ELECTRO';

    render(
        <Context>
            <Provider store={ store }>
                <Review
                    review={ newReview }
                    comments={{ pagination: {} }}
                    config={{}}
                    listingParams={{}}
                />
            </Provider>
        </Context>,
    );

    expect(document.querySelector('.ElectroBannerDesktop')).not.toBeNull();
});
