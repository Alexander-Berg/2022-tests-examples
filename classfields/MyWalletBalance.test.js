jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

const _ = require('lodash');
const React = require('react');
const MyWalletBalance = require('./MyWalletBalance');
const Button = require('auto-core/react/components/islands/Button');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const stateMock = require('autoru-frontend/mockData/state/state.mock');
const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

let initialState;
let props;

let paymentModalParams;
paymentModalOpen.mockImplementation((params) => {
    paymentModalParams = params;
    return jest.fn();
});

let context;
let vasLogParams;

beforeEach(() => {
    initialState = {
        state: {
            paymentModalResult: _.cloneDeep(stateMock.paymentModalResult),
        },
        user: _.cloneDeep(userMock),
    };

    context = _.cloneDeep(contextMock);
    context.logVasEvent = jest.fn((params) => {
        vasLogParams = params;
    });
    context.link = jest.fn((route, params) => `link to "${ route }" with params: ${ JSON.stringify(params) }`);

    paymentModalOpen.mockClear();
    context.logVasEvent.mockClear();
});

it('правильно рисует компонент', () => {
    const page = shallowRenderMyWalletBalance();

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно логирует событие показа', () => {
    shallowRenderMyWalletBalance();

    expect(context.logVasEvent).toHaveBeenCalledTimes(1);
    expect(vasLogParams).toMatchSnapshot();
});

describe('при клике на кнопку пополнения баланса', () => {
    beforeEach(() => {
        const page = shallowRenderMyWalletBalance();
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

function shallowRenderMyWalletBalance() {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <MyWalletBalance { ...props } store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
