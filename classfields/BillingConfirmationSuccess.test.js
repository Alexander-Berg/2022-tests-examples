/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/billing/actions/autoProlongation.js', () => ({
    toggleAutoProlongationToTransaction: jest.fn(),
    addAutoProlongationToOffer: jest.fn(),
    setAutoBoostSchedule: jest.fn(),
}));

const React = require('react');
const { Provider } = require('react-redux');
const BillingConfirmationSuccess = require('./BillingConfirmationSuccess');
const BillingPaymentStatus = require('../BillingPaymentStatus');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const autoProlongationActions = require('auto-core/react/dataDomain/billing/actions/autoProlongation');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const { PAYMENT_MESSAGE_TYPES } = require('auto-core/lib/billing/utils');
const FRAME_ORIGIN = 'https://auto.ru';

let initialState;
let store;
let originalWindowParent;

beforeEach(() => {
    const config = configStateMock.withPageParams({
        rus_name: 'Турбо-продажа',
        cost: 997,
        name: 'package_turbo',
        days: 3,
        base_price: 1999,
    }).value();

    initialState = {
        config: config,
    };
    originalWindowParent = global.parent;
    global.parent.postMessage = () => {};
});

afterEach(() => {
    global.parent = originalWindowParent;
});

it('правильно рисует компонент для десктопа', () => {
    const page = shallowRenderComponent();
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент для мобилки', () => {
    const page = shallowRenderComponent({ isMobile: true });
    expect(shallowToJson(page)).toMatchSnapshot();
});

describe('при клике по кнопке автопродления', () => {
    let spy;
    let service;
    let statusBlock;
    let autoProlongationPromise;

    beforeEach(() => {
        global.parent.postMessage = jest.fn();
        autoProlongationPromise = Promise.resolve({ status: 'SUCCESS' });
        spy = jest.fn(() => () => autoProlongationPromise);
        service = initialState.config.data.pageParams.name;
        autoProlongationActions.addAutoProlongationToOffer.mockImplementation(spy);

        const page = shallowRenderComponent();
        statusBlock = page.find(BillingPaymentStatus);
    });

    it('вызовет пропс с корректными аргументами', () => {
        statusBlock.simulate('autoProlongationButtonClick', service);

        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith(service);
    });

    it('сообщит родительскому окну об удачном ответе', () => {
        statusBlock.simulate('autoProlongationButtonClick', service);

        return autoProlongationPromise
            .then(() => {})
            .then(() => {
                expect(global.parent.postMessage).toHaveBeenCalledTimes(1);
                expect(global.parent.postMessage).toHaveBeenCalledWith(
                    { payload: { status: 'SUCCESS' }, source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.auto_prolongation_change },
                    FRAME_ORIGIN,
                );
            });
    });
});

describe('при клике по кнопке автоподнятия', () => {
    let spy;
    let statusBlock;
    let autoBoostPromise;
    const time = '10:00';

    beforeEach(() => {
        global.parent.postMessage = jest.fn();
        autoBoostPromise = Promise.resolve({ status: 'SUCCESS' });
        spy = jest.fn(() => () => autoBoostPromise);
        initialState.config.data.pageParams = {
            rus_name: 'Поднятие в поиске',
            name: 'all_sale_fresh',
            days: 1,
        };
        autoProlongationActions.setAutoBoostSchedule.mockImplementation(spy);

        const page = shallowRenderComponent();
        statusBlock = page.find(BillingPaymentStatus);
    });

    it('вызовет пропс с корректными аргументами', () => {
        statusBlock.simulate('autoBoostButtonClick', time);

        expect(spy).toHaveBeenCalledTimes(1);
        expect(spy).toHaveBeenCalledWith(time);
    });

    it('сообщит родительскому окну об удачном ответе', () => {
        statusBlock.simulate('autoBoostButtonClick', time);

        return autoBoostPromise
            .then(() => { })
            .then(() => {
                expect(global.parent.postMessage).toHaveBeenCalledTimes(1);
                expect(global.parent.postMessage).toHaveBeenCalledWith(
                    { payload: { status: 'SUCCESS' }, source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.auto_boost_change },
                    FRAME_ORIGIN,
                );
            });
    });
});

it('если не удалось подключить автоподнятие сообщит об этом родительскому окну', () => {
    global.parent.postMessage = jest.fn();
    const autoProlongationPromise = Promise.reject({ status: 'ERROR' });
    const spy = jest.fn(() => () => autoProlongationPromise);
    const service = initialState.config.data.pageParams.name;
    autoProlongationActions.addAutoProlongationToOffer.mockImplementation(spy);

    const page = shallowRenderComponent();
    const statusBlock = page.find(BillingPaymentStatus);

    statusBlock.simulate('autoProlongationButtonClick', service);

    return autoProlongationPromise
        .then(() => new Promise((resolve) => process.nextTick(resolve)))
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                expect(global.parent.postMessage).toHaveBeenCalledTimes(1);
                expect(global.parent.postMessage).toHaveBeenCalledWith(
                    { payload: { status: 'ERROR' }, source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.auto_prolongation_change },
                    FRAME_ORIGIN,
                );
            },
        );
});

function shallowRenderComponent(props = {}) {
    store = mockStore(initialState);

    const wrapper = shallow(
        <Provider store={ store }>
            <BillingConfirmationSuccess { ...props }/>
        </Provider>,
    );
    return wrapper.dive().dive();
}
