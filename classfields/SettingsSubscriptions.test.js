jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
const getResource = require('auto-core/react/lib/gateApi').getResource;

const React = require('react');
const { shallow } = require('enzyme');

const { Provider } = require('react-redux');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const SettingsSubscriptions = require('./SettingsSubscriptions').default;
const SUBSCRIPTION_CATEGORIES = require('www-cabinet/data/subscription/subscription-categories');

const defaultState = {
    user: { data: { emails: [ { email: 'user@ya.ru' } ] } },
    settingsSubscriptions: {
        'application-credit': [
            { emailAddress: 'ac-user1@ya.ru', id: 1 },
            { emailAddress: 'ac-user2@ya.ru', id: 2 },
        ],
        booking: [
            { emailAddress: 'b-user1@ya.ru', id: 3 },
            { emailAddress: 'b-user2@ya.ru', id: 4 },
        ],
    },
    settings: { allow_photo_reorder: 1 },
};

const defaultProps = {};

it('не должен показать настройку кредитных заявок, если юзер не менеджер', () => {
    const tree = shallowRenderComponent();

    expect(tree.findWhere(node => node.key() === SUBSCRIPTION_CATEGORIES.APPLICATION_CREDIT)).not.toExist();
});

it('не отправит запрос на обновление адреса подписки, если введен некорректный email', () => {
    const tree = shallowRenderComponent();

    const emailInput = tree.find('.SettingsSubscription__email').at(0);
    emailInput.simulate('focusChange', false, { name: 'booking', value: 'финифть', subId: 3, index: 0 });

    expect(getResource).not.toHaveBeenCalled();
});

it('обрежет пробелы в начале и конце email при потере фокуса у инпута', () => {
    const tree = shallowRenderComponent();
    const fofudja = 'фофудья';
    const fofudjaWithSpaces = ` ${ fofudja }  `;
    const fofudjaId = 3;

    const emailInput = tree.find('.SettingsSubscription__email').at(0);
    emailInput.simulate('focusChange', false, { name: 'booking', value: fofudjaWithSpaces, subId: fofudjaId, index: 0 });

    expect(tree.state().booking.find(email => email.id === fofudjaId).emailAddress).toEqual(fofudja);
});

function shallowRenderComponent(state = defaultState, props = defaultProps) {
    const store = mockStore(state);
    const wrapper = shallow(
        <Provider store={ store }>
            <SettingsSubscriptions { ...props }/>
        </Provider>,
    );
    return wrapper.dive().dive();
}
