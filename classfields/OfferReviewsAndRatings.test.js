const React = require('react');
const { shallow } = require('enzyme');

const reviewsSummary = require('auto-core/react/dataDomain/reviewsSummary/mocks/reviewsSummary.mock');
const reviewFeaturesMock = require('auto-core/react/dataDomain/reviewsFeatures/mocks/reviewsFeatures.mock');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const OfferReviewsAndRatings = require('./OfferReviewsAndRatings');

let resourceParams;
beforeEach(() => {
    resourceParams = {
        category: 'cars',
        mark: 'AUDI',
        model: 'A4',
    };
});

it('не должен рендерить, если нет features и summary', () => {
    const wrapper = shallow(
        <OfferReviewsAndRatings
            category="CARS"
            resourceParams={ resourceParams }
        />, { context: contextMock },
    );

    expect(wrapper.isEmptyRender()).toBe(true);
});

it('не должен рендерить, если нет summary', () => {
    const wrapper = shallow(
        <OfferReviewsAndRatings
            category="CARS"
            resourceParams={ resourceParams }
            features={ reviewFeaturesMock.data.features }
        />, { context: contextMock },
    );

    expect(wrapper.isEmptyRender()).toBe(true);
});

it('должен рендерить отзывы, если нет features', () => {
    const wrapper = shallow(
        <OfferReviewsAndRatings
            category="CARS"
            resourceParams={ resourceParams }
            summary={ reviewsSummary }
        />, { context: contextMock },
    );

    expect(wrapper.isEmptyRender()).toBe(false);
});

it('не должен рендерить отзывы, если hideReviews', () => {
    const wrapper = shallow(
        <OfferReviewsAndRatings
            category="CARS"
            hideReviews
            resourceParams={ resourceParams }
            summary={ reviewsSummary }
        />, { context: contextMock },
    );

    expect(wrapper.find('.CardReviews__list')).not.toExist();
});

it('должен рендерить подвал, если featuresCount=100', () => {
    const wrapper = shallow(
        <OfferReviewsAndRatings
            category="CARS"
            features={ reviewFeaturesMock.data.features }
            featuresCount={ 100 }
            resourceParams={ resourceParams }
            summary={ reviewsSummary }
        />, { context: contextMock },
    );

    expect(wrapper.find('.CardReviews__loadMore')).not.toExist();
});

it('не должен рендерить подвал, если featuresCount=1', () => {
    const features = { ...reviewFeaturesMock.data.features };
    features.positive = [ ...features.positive, ...features.positive ];
    const wrapper = shallow(
        <OfferReviewsAndRatings
            category="CARS"
            features={ features }
            featuresCount={ 1 }
            resourceParams={ resourceParams }
            summary={ reviewsSummary }
        />, { context: contextMock },
    );

    expect(wrapper.find('.CardReviews__loadMore')).toExist();
});
