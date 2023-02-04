const { shallow } = require('enzyme');
const React = require('react');
const { Provider } = require('react-redux');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.2.mock.ts').default;
const geoMock = require('auto-core/react/dataDomain/geo/mocks/geo.mock');

const CardDealerSubscriptionButton = require('./CardDealerSubscriptionButton');

let Context;
let store;

beforeEach(() => {
    Context = createContextProvider(contextMock);
    store = mockStore({
        geo: geoMock,
        card: cardMock,
        listing: { data: { search_parameters: { category: 'cars', section: 'used' } } },
    });
});

it('должен добавить id дилера в параметры подписки', () => {
    const wrapper = renderComponent();
    expect(wrapper.props().searchParams).toEqual({ category: 'cars', section: 'used', dealer_id: '20867192' });
});

function renderComponent() {
    return shallow(
        <Context>
            <Provider store={ store }>
                <CardDealerSubscriptionButton/>
            </Provider>
        </Context>,
    ).dive().dive();
}
