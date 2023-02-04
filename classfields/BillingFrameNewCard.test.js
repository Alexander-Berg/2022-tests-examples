/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { Provider } = require('react-redux');
const BillingFrameNewCard = require('./BillingFrameNewCard');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const PAYMENT_TOKEN = 'very-secret-string';

const configStateMock = require('auto-core/react/dataDomain/config/mock').default;

let amount;
let initialState;
let store;
let originalWindowParent;

const YooMoneyCheckoutUIEventMap = {};
const YooMoneyCheckoutUIMockOpen = jest.fn();
const YooMoneyCheckoutUIMock = jest.fn().mockImplementation(() => {
    return {
        open: YooMoneyCheckoutUIMockOpen,
        on: jest.fn((event, cb) => {
            YooMoneyCheckoutUIEventMap[event] = cb;
        }),
    };
});
const postMessageMock = jest.fn();

beforeEach(() => {
    amount = 997;
    initialState = {
        config: configStateMock.value(),
    };
    initialState.config.data.pageParams = { amount };

    YooMoneyCheckoutUIMock.mockClear();
    YooMoneyCheckoutUIMockOpen.mockClear();
    postMessageMock.mockClear();

    originalWindowParent = global.parent;
    global.YooMoneyCheckoutUI = YooMoneyCheckoutUIMock;
    global.parent.postMessage = postMessageMock;
});

afterEach(() => {
    global.parent = originalWindowParent;
    global.YooMoneyCheckoutUI = undefined;
});

it('правильно рисует компонент для десктопа', () => {
    const page = shallowRenderComponent();
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент для мобилки', () => {
    const page = shallowRenderComponent({ isMobile: true });
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('корректно инициализирует библиотеку YooMoneyCheckoutUI.js', () => {
    shallowRenderComponent();

    expect(YooMoneyCheckoutUIMock).toHaveBeenCalledTimes(1);
    expect(YooMoneyCheckoutUIMock).toHaveBeenCalledWith('autoru_frontend.yandex_kassa_shop_id', { amount });
    expect(YooMoneyCheckoutUIMockOpen).toHaveBeenCalledTimes(1);
});

it('если не удалось инициализировать библиотеку YooMoneyCheckoutUI.js, отправит родителю ошибку', () => {
    global.YooMoneyCheckoutUI = undefined;
    const page = shallowRenderComponent();

    expect(postMessageMock).toHaveBeenCalledTimes(1);
    expect(postMessageMock).toHaveBeenCalledWith(
        { source: 'card_frame', type: 'error', payload: 'ya_checkout_fail' },
        page.instance().props.origin,
    );
});

describe('при наступление события "yc_success"', () => {
    let page;

    beforeEach(() => {
        page = shallowRenderComponent();
    });

    it('если токен получен, отправит его родителю', () => {
        YooMoneyCheckoutUIEventMap.yc_success({ data: { response: { paymentToken: PAYMENT_TOKEN } } });

        expect(postMessageMock).toHaveBeenCalledTimes(1);
        expect(postMessageMock).toHaveBeenCalledWith(
            { payload: PAYMENT_TOKEN, source: 'card_frame', type: 'token' },
            page.instance().props.origin,
        );
    });

    it('если токена нет, отправит родителю ошибку', () => {
        YooMoneyCheckoutUIEventMap.yc_success();

        expect(postMessageMock).toHaveBeenCalledTimes(1);
        expect(postMessageMock).toHaveBeenCalledWith(
            { source: 'card_frame', type: 'error', payload: 'ya_checkout_fail' },
            page.instance().props.origin,
        );
    });
});

function shallowRenderComponent(props = {}) {
    store = mockStore(initialState);

    const wrapper = shallow(
        <Provider store={ store }>
            <BillingFrameNewCard { ...props }/>
        </Provider>,
    );
    return wrapper.dive().dive();
}
