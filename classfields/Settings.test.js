const React = require('react');
const { shallow } = require('enzyme');

const { Provider } = require('react-redux');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const defaultTariffsMock = require('./mocks/defaultTariffs.mock');
const onlyCallTariffsMock = require('./mocks/onlyCallTariffs.mock');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const Settings = require('./Settings');

const contextObject = {
    context: { linkCabinet: jest.fn() },
};

it('не должен нарисовать вкладку с чатами, ' +
    'если у пользователя подключен только звонковый тариф', () => {
    const state = {
        promoPopup: { tariffs: onlyCallTariffsMock },
        bunker: getBunkerMock([ 'cabinet/settings_crm' ]),
    };
    const tree = shallowRenderComponent(state);

    expect(
        tree
            .find('Connect(ServiceNavigation)')
            .props().navigationItems
            .find(item => item.name === 'chats'),
    ).toBeUndefined();

});

it('должен нарисовать вкладку с чатами, если у пользователя подключен тариф SINGLE_WITH_CALLS', () => {
    const state = {
        promoPopup: { tariffs: defaultTariffsMock },
        bunker: getBunkerMock([ 'cabinet/settings_crm' ]),
    };
    const tree = shallowRenderComponent(state);

    expect(
        tree
            .find('Connect(ServiceNavigation)')
            .props().navigationItems
            .find(item => item.name === 'chats'),
    ).toBeDefined();

});

const defaultProps = {};

function shallowRenderComponent(state, props = defaultProps) {
    const store = mockStore(state);
    const wrapper = shallow(
        <Provider store={ store }>
            <Settings { ...props }/>
        </Provider>,
        contextObject,
    );
    return wrapper.dive().dive();
}
