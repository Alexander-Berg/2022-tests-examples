/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('../../actions/draft', () => {
    return {
        publishOfferDraft: jest.fn(),
    };
});
const { publishOfferDraft: publishOfferDraftMock } = require('../../actions/draft');

const React = require('react');
const _ = require('lodash');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const OfferPhotosAddForm = require('./OfferPhotosAddForm');
const Context = createContextProvider(contextMock);

const defaultStore = mockStore({
    draft: {
        offer: _.cloneDeep(mockOffer),
    },
});

it('должен вычислить fingerprint на componentDidMount', () => {
    const wrapper = renderShallowWrapper();

    expect(wrapper.state('fingerprint')).toEqual('fingerprint2_mock_value');
});

it('должен отправить метрику на didMount', () => {
    renderShallowWrapper();
    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'cars', 'card_no_photo', 'open_page' ]);
});

it('должен отправить метрику после успешной загрузки', () => {
    const publishOfferDraftResolve = Promise.resolve({ status: 'SUCCESS' });

    publishOfferDraftMock.mockImplementation(() => () => publishOfferDraftResolve);

    const store = mockStore({
        draft: {
            offer: mockOffer,
        },
    });
    const wrapper = renderShallowWrapper(store);
    wrapper.find('Button').simulate('click');

    return publishOfferDraftResolve.then(() => {
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendParams.mock.calls[1][0]).toEqual([ 'cars', 'card_no_photo', 'add_photo', 'success' ]);
    });
});

function renderShallowWrapper() {
    return shallow(
        <Provider store={ defaultStore }>
            <Context>
                <OfferPhotosAddForm params={{}}/>
            </Context>
        </Provider>,
    ).dive().dive().dive();
}
