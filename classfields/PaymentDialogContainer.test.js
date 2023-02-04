/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const _ = require('lodash');
const PaymentDialogContainer = require('./PaymentDialogContainer');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const { PAYMENT_MESSAGE_TYPES } = require('auto-core/lib/billing/utils');
const { SECOND } = require('auto-core/lib/consts');

const paymentParamsMock = {
    category: 'cars',
    geoId: 213,
    from: 'new-lk-tab',
    offerId: '1234567890-foo',
    orderId: '123-bar',
    product: [ { name: 'package_turbo', count: 1 } ],
    purchaseCount: 1,
    stoId: 'my-sto-it-the-best',
    section: 'used',
    salesmanDomain: 'autoru',
    returnUrl: 'http://pornhub.com',
};

const DEFAULT_PROPS = {
    paymentParams: paymentParamsMock,
    onPayAutorenewChange: () => { },
    onPayAutoProlongChange: () => { },
    closeDialog: jest.fn(),
    onAfterPayment: () => { },
    isMobile: false,
};

let props;
let context;
let originalWindowAddEventListener;
let originalWindowVertisChat;
const eventMap = {};

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
    context = {
        link: () => {},
    };
    originalWindowAddEventListener = global.addEventListener;
    originalWindowVertisChat = global.vertis_chat;
    global.addEventListener = jest.fn((event, cb) => {
        eventMap[event] = cb;
    });
    global.vertis_chat = {};

    jest.useFakeTimers('legacy');
});

afterEach(() => {
    global.addEventListener = originalWindowAddEventListener;
    global.vertis_chat = originalWindowVertisChat;
    jest.useRealTimers();
});

it('у айфрейма будет статус loading до момента пока фрейм не ответит что он готов', () => {
    const page = shallowRenderPaymentDialogContainer();

    const frameBefore = page.find('.PaymentDialogContainer__frame');
    expect(frameBefore.hasClass('PaymentDialogContainer__frame_loading')).toBe(true);

    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.frame_loaded } });

    const frameAfter = page.find('.PaymentDialogContainer__frame');
    expect(frameAfter.hasClass('PaymentDialogContainer__frame_loading')).toBe(false);
});

describe('при ошибке загрузки', () => {
    it('закроет себя сразу без подтверждения', () => {
        const framePostMessageMock = jest.fn();
        const page = shallowRenderPaymentDialogContainer({ framePostMessageMock });

        expect(page.state('isLoading')).toBe(true);

        jest.advanceTimersByTime(15 * SECOND);

        expect(page.state('isLoading')).toBe(false);
        expect(page.state('isError')).toBe(true);

        const instance = page.instance();
        instance.handleCloseRequest();

        expect(props.closeDialog).toHaveBeenCalledTimes(1);
        expect(framePostMessageMock).toHaveBeenCalledTimes(0);
    });

    it('при ретрае запросит страницу заново', () => {
        const framePostMessageMock = jest.fn();
        const page = shallowRenderPaymentDialogContainer({ framePostMessageMock });

        jest.advanceTimersByTime(15 * SECOND);

        const frame = page.find('.PaymentDialogContainer__frame');
        const error = page.find('.PaymentDialogContainer__error');
        expect(frame.isEmptyRender()).toBe(true);
        expect(error.isEmptyRender()).toBe(false);

        const retryButton = page.find('.PaymentDialogContainer__errorButton');
        retryButton.simulate('click');

        const frame2 = page.find('.PaymentDialogContainer__frame');
        const error2 = page.find('.PaymentDialogContainer__error');
        expect(frame2.isEmptyRender()).toBe(false);
        expect(error2.isEmptyRender()).toBe(true);

        // проверяем, что при ретрае в случае неудачи мы тоже покажем ошибки и не покажем фрейм
        jest.advanceTimersByTime(15 * SECOND);

        const frame3 = page.find('.PaymentDialogContainer__frame');
        const error3 = page.find('.PaymentDialogContainer__error');
        expect(frame3.isEmptyRender()).toBe(true);
        expect(error3.isEmptyRender()).toBe(false);
    });
});

it('в процессе загрузки, если не получили сообщение от фрейма о готовности, закроет себя сразу без подтверждения', () => {
    const framePostMessageMock = jest.fn();
    const page = shallowRenderPaymentDialogContainer({ framePostMessageMock });

    expect(page.state('isLoading')).toBe(true);

    const instance = page.instance();
    instance.handleCloseRequest();

    expect(props.closeDialog).toHaveBeenCalledTimes(1);
    expect(framePostMessageMock).toHaveBeenCalledTimes(0);
});

it('если фрейм загрузился, то при закрытии запросит подтверждение у фрейма', () => {
    const framePostMessageMock = jest.fn();
    const page = shallowRenderPaymentDialogContainer({ framePostMessageMock });

    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.frame_loaded } });

    expect(page.state('isLoading')).toBe(false);

    const instance = page.instance();
    instance.handleCloseRequest();

    expect(props.closeDialog).toHaveBeenCalledTimes(0);
    expect(framePostMessageMock).toHaveBeenCalledTimes(1);
    expect(framePostMessageMock).toHaveBeenCalledWith({ source: 'parent', type: 'close_request' }, '*');
});

it('правильно рисуется на десктопе', () => {
    const page = shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.frame_loaded } });

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисуется на мобилке', () => {
    props.isMobile = true;
    const page = shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.frame_loaded } });

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно формирует ссылку для айфрейма', () => {
    let routeName;
    let pageParams;
    context.link = jest.fn((name, params) => {
        routeName = name;
        pageParams = params;
        return 'link-to-billing';
    });
    shallowRenderPaymentDialogContainer();

    expect(context.link).toHaveBeenCalledTimes(1);
    expect(routeName).toBe('billing');
    expect(pageParams).toMatchSnapshot();
});

//TODO удалить этот тест после того как выкатим траст
it('изменяет только высоту фрейма когда необходимо', () => {
    const newHeight = 100;
    const page = shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.resize, payload: newHeight } });
    expect(page.instance().frame.height).toBe(`${ newHeight }px`);
});

it('изменяет высоту и ширину фрейма когда необходимо', () => {
    const newHeight = 100;
    const newWidth = 200;
    const page = shallowRenderPaymentDialogContainer();
    eventMap.message({
        data: {
            source: 'billing_frame',
            type: PAYMENT_MESSAGE_TYPES.resize,
            payload: { height: newHeight, width: newWidth },
        },
    });

    expect(page.instance().frame.width).toBe(`${ newWidth }px`);
    expect(page.instance().frame.height).toBe(`${ newHeight }px`);
});

it('закрывает себя при получение сообщения от фрейма', () => {
    props.closeDialog = jest.fn();
    shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.close_request } });
    expect(props.closeDialog).toHaveBeenCalledTimes(1);
});

it('вызывает коллбэк при успешной оплате', () => {
    props.onAfterPayment = jest.fn();
    shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.payment_success, payload: { purchaseCount: 1 } } });
    expect(props.onAfterPayment).toHaveBeenCalledTimes(1);
    expect(props.onAfterPayment).toHaveBeenCalledWith(props.paymentParams);
});

it('вызывает коллбэк при подключение автобуста', () => {
    const boostPayload = { status: 'SUCCESS', time: '09:00' };
    props.onPayAutorenewChange = jest.fn();
    shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.auto_boost_change, payload: boostPayload } });
    expect(props.onPayAutorenewChange).toHaveBeenCalledTimes(1);
    expect(props.onPayAutorenewChange).toHaveBeenCalledWith(boostPayload);
});

it('вызывает коллбэк при подключение автопродления', () => {
    const autoProlongationPayload = { status: 'SUCCESS' };
    props.onPayAutoProlongChange = jest.fn();
    shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.auto_prolongation_change, payload: autoProlongationPayload } });
    expect(props.onPayAutoProlongChange).toHaveBeenCalledTimes(1);
    expect(props.onPayAutoProlongChange).toHaveBeenCalledWith(autoProlongationPayload);
});

it('откроет чат с тех поддержкой при необходимости', () => {
    global.vertis_chat.open_tech_support_chat = jest.fn();
    shallowRenderPaymentDialogContainer();
    eventMap.message({ data: { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.open_tech_support_chat } });
    expect(global.vertis_chat.open_tech_support_chat).toHaveBeenCalledTimes(1);
});

function shallowRenderPaymentDialogContainer({ framePostMessageMock } = { framePostMessageMock: jest.fn() }) {
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <PaymentDialogContainer { ...props }/>
        </ContextProvider>,
    );

    const page = wrapper.dive();
    const instance = page.instance();
    instance.frame = {
        height: '',
        contentWindow: {
            postMessage: framePostMessageMock,
        },
    };

    return page;
}
