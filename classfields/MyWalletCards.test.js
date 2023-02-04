jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');
jest.mock('auto-core/react/dataDomain/user/actions/updateCardInfo');

const _ = require('lodash');
const React = require('react');
const MyWalletCards = require('./MyWalletCards');
const Button = require('auto-core/react/components/islands/Button');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userWithAuthMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const userWithCardsMock = require('auto-core/react/dataDomain/user/mocks/withTiedCards.mock');
const stateMock = require('autoru-frontend/mockData/state/state.mock');
const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const updateCardInfo = require('auto-core/react/dataDomain/user/actions/updateCardInfo').default;

let initialState;
let props;

let paymentModalParams;
paymentModalOpen.mockImplementation((params) => {
    paymentModalParams = params;
    return jest.fn();
});

updateCardInfo.mockImplementation(jest.fn(() => () => {}));

let context;
let vasLogParams;

beforeEach(() => {
    initialState = {
        state: {
            paymentModalResult: _.cloneDeep(stateMock.paymentModalResult),
        },
        user: _.cloneDeep(userWithAuthMock),
    };

    context = _.cloneDeep(contextMock);
    context.logVasEvent = jest.fn((params) => {
        vasLogParams = params;
    });
    context.link = jest.fn((route, params) => `link to "${ route }" with params: ${ JSON.stringify(params) }`);

    paymentModalOpen.mockClear();
    context.logVasEvent.mockClear();
});

it('правильно рисует компонент если нет привязанных карт', () => {
    const page = shallowRenderMyWalletCards();

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент если есть одна привязанная карта', () => {
    initialState.user = _.cloneDeep(userWithCardsMock);
    initialState.user.data.tied_cards = userWithCardsMock.data.tied_cards.slice(0, 1);
    const page = shallowRenderMyWalletCards();

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент если есть две привязанные карты или больше', () => {
    initialState.user = _.cloneDeep(userWithCardsMock);
    const page = shallowRenderMyWalletCards();

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('позволяет менять карты в селекте', () => {
    initialState.user = _.cloneDeep(userWithCardsMock);

    const page = shallowRenderMyWalletCards();
    const select = page.find('Select');
    const targetCardMask = initialState.user.data.tied_cards[2].card_mask;
    select.simulate('change', [ targetCardMask ]);

    const updatedSelect = page.find('Select');
    expect(updatedSelect.prop('value')).toBe(targetCardMask);
});

it('позволяет сделать карту основной', () => {
    initialState.user = _.cloneDeep(userWithCardsMock);

    const page = shallowRenderMyWalletCards();
    const select = page.find('Select');
    const targetCardMask = initialState.user.data.tied_cards[2].card_mask;
    select.simulate('change', [ targetCardMask ]);

    const checkbox = page.find('Checkbox');
    checkbox.simulate('check');

    expect(updateCardInfo).toHaveBeenCalledTimes(1);
    expect(updateCardInfo).toHaveBeenCalledWith({ card_id: '555555|4444', payment_system_id: 'yandexkassa_v3', preferred: true });
});

it('правильно логирует событие показа', () => {
    shallowRenderMyWalletCards();

    expect(context.logVasEvent).toHaveBeenCalledTimes(1);
    expect(vasLogParams).toMatchSnapshot();
});

describe('при клике на кнопку привязки карты', () => {
    beforeEach(() => {
        const page = shallowRenderMyWalletCards();
        const button = page.find(Button);
        button.simulate('click');
    });

    it('залогирует событие клика', () => {
        expect(context.logVasEvent).toHaveBeenCalledTimes(2);
        expect(vasLogParams).toMatchSnapshot();
    });

    it('откроет окно оплаты с правильными параметрами', () => {

        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalParams).toMatchSnapshot();
    });
});

function shallowRenderMyWalletCards() {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <MyWalletCards { ...props } store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
