const reviewsMock = require('./mocks/reviews.mock');

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
const getResource = require('auto-core/react/lib/gateApi').getResource;

const React = require('react');
const querystring = require('querystring');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const IndexReviews = require('./IndexReviews');

const context = {
    link: (routeName, routeParams) => `${ routeName }?${ querystring.stringify(routeParams) }`,
};

it('должен отрендерить провязку с отзывами', () => {
    const mockResponse = Promise.resolve(reviewsMock);
    getResource.mockImplementation(() => mockResponse);

    const wrapper = shallow(
        <IndexReviews/>,
        { context: context });

    return mockResponse.then(() => {
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
});

it('провязка отзывов на главной: не должен отрендерить отзыв без рейтинга', () => {
    const mockResponse = Promise.resolve(reviewsMock);
    getResource.mockImplementation(() => mockResponse);

    const wrapper = shallow(
        <IndexReviews/>,
        { context: context });

    return mockResponse.then(() => {
        const reviewIds = wrapper.find('.IndexReviews__review').map(n => n.key());

        expect(reviewIds).not.toContain('8859172958711294932');
    });
});
