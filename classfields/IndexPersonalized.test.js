/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const React = require('react');
const { Provider } = require('react-redux');
const { mount, shallow } = require('enzyme');

const IndexPersonalized = require('./IndexPersonalized');

const sendParams = jest.fn();
const ContextProvider = createContextProvider({
    metrika: {
        sendParams,
    },
});
const store = mockStore({
    recommendedOffers: {
        offers: [ { category: 'cars', id: 1 }, { category: 'cars', id: 2 } ],
    },
});

it('должен отправить метрику, когда блок нарисован', function() {
    mount(
        <ContextProvider>
            <Provider store={ store }>
                <IndexPersonalized
                    searchID="searchID"
                />
            </Provider>
        </ContextProvider>,
    );

    expect(sendParams).toHaveBeenCalledWith([ 'cars', 'index', `personalized`, 'shows' ]);
});

it('должен отдать props from для метрики', function() {
    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <IndexPersonalized
                    searchID="searchID"
                />
            </Provider>
        </ContextProvider>,
    );
    expect(tree.dive().dive().dive().find('CarouselOffers').props().offerProps.from).toBe('personalized');
});
