jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(),
    };
});

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const review = require('autoru-frontend/mockData/state/review.mock');

const gateApi = require('auto-core/react/lib/gateApi');
const ReviewSnippetDesktop = require('./ReviewSnippetDesktop');

let reviewMock;
let store;
beforeEach(() => {
    store = mockStore({});
    reviewMock = _.cloneDeep(review);
});

describe('like/dislike', () => {
    it('должен вызвать экшен лайка после клика', () => {
        reviewMock.dislike_num = 1;
        reviewMock.like_num = 2;

        const mockResponse = Promise.resolve({});
        gateApi.getResourcePublicApi.mockImplementation(() => mockResponse);

        const wrapper = shallow(
            <ReviewSnippetDesktop review={ reviewMock }/>,
            { context: { ...contextMock, store } },
        ).dive();

        const likeButton = wrapper.find('.ReviewSnippetDesktop__statusBlockItem_type_like');
        likeButton.simulate('click', {}, likeButton.props());

        return mockResponse.then(() => {
            expect(store.getActions()).toEqual([
                { type: 'REVIEW_RATE_PENDING' },
                {
                    type: 'REVIEW_RATE_RESOLVED',
                    payload: {
                        opinion: 'LIKE',
                        rates: {
                            dislike: 1,
                            like: 3,
                        },
                    },
                },
            ]);
        });
    });

    it('должен вызвать экшен дизлайка после клика', () => {
        reviewMock.dislike_num = 1;
        reviewMock.like_num = 2;

        const mockResponse = Promise.resolve({});
        gateApi.getResourcePublicApi.mockImplementation(() => mockResponse);

        const wrapper = shallow(
            <ReviewSnippetDesktop review={ reviewMock }/>,
            { context: { ...contextMock, store } },
        ).dive();

        const likeButton = wrapper.find('.ReviewSnippetDesktop__statusBlockItem_type_dislike');
        likeButton.simulate('click', {}, likeButton.props());

        return mockResponse.then(() => {
            expect(store.getActions()).toEqual([
                { type: 'REVIEW_RATE_PENDING' },
                {
                    type: 'REVIEW_RATE_RESOLVED',
                    payload: {
                        opinion: 'DISLIKE',
                        rates: {
                            dislike: 2,
                            like: 2,
                        },
                    },
                },
            ]);
        });
    });
});
