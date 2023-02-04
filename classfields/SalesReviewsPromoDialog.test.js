const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const querystring = require('querystring');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const SalesReviewsPromoDialog = require('./SalesReviewsPromoDialog');

jest.useFakeTimers();

it('должен правильно отрендерить промо отзывов', () => {
    const store = mockStore({
        state: {
            reviewPromoActive: true,
            reviewsPromoParams: { offer: cardMock },
            reloadPageAfterClose: true,
        },
    });
    const context = {
        ...contextMock,
        link: (url, params) => `${ url }-${ querystring.stringify(params) }`,
        store,
    };
    const wrapper = shallow(<SalesReviewsPromoDialog campaign="lk"/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен скрывать промо отзывов по reviewPromoActive=false', () => {
    const store = mockStore({
        state: {
            reviewPromoActive: false,
            reviewsPromoParams: { offer: cardMock },
            reloadPageAfterClose: true,
        },
    });
    const context = {
        ...contextMock,
        link: (url, params) => `${ url }-${ querystring.stringify(params) }`,
        store,
    };
    const wrapper = shallow(<SalesReviewsPromoDialog campaign="lk"/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен скрывать промо отзывов по кнопке "скрыть"', () => {
    const store = mockStore({
        state: {
            reviewPromoActive: true,
            reviewsPromoParams: { offer: cardMock },
            reloadPageAfterClose: false,
        },
    });
    const context = {
        ...contextMock,
        link: (url, params) => `${ url }-${ querystring.stringify(params) }`,
        store,
    };
    const wrapper = shallow(<SalesReviewsPromoDialog campaign="lk"/>, { context: context }).dive();
    wrapper.find('Button[children="Не оставлять"]').simulate('click');
    jest.runAllTimers();
    expect(store.getActions()).toEqual([ { type: 'CLOSE_REVIEWS_PROMO_MODAL' } ]);
});

it('должен скрывать промо отзывов по кнопке "оставить отзыв"', () => {
    const store = mockStore({
        state: {
            reviewPromoActive: true,
            reviewsPromoParams: { offer: cardMock },
            reloadPageAfterClose: false,
        },
    });
    const context = {
        ...contextMock,
        link: (url, params) => `${ url }-${ querystring.stringify(params) }`,
        store,
    };
    const wrapper = shallow(<SalesReviewsPromoDialog campaign="lk"/>, { context: context }).dive();
    wrapper.find('Button[children="Оставить отзыв"]').simulate('click');
    jest.runAllTimers();
    expect(store.getActions()).toEqual([ { type: 'CLOSE_REVIEWS_PROMO_MODAL' } ]);
});

it('должен перезагрузить страницу по reloadPageAfterClose=true', () => {
    const store = mockStore({
        state: {
            reviewPromoActive: true,
            reviewsPromoParams: { offer: cardMock },
            reloadPageAfterClose: true,
        },
    });
    const context = {
        ...contextMock,
        link: (url, params) => `${ url }-${ querystring.stringify(params) }`,
        store,
    };
    const wrapper = shallow(<SalesReviewsPromoDialog campaign="lk"/>, { context: context }).dive();
    wrapper.find('Button[children="Оставить отзыв"]').simulate('click');
    jest.runAllTimers();
    expect(store.getActions()).toEqual([ { type: 'CLOSE_REVIEWS_PROMO_MODAL' } ]);
});
