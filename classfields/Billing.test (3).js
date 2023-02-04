/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/billing/actions/autoProlongation.js', () => ({
    toggleAutoProlongationToTransaction: jest.fn(),
    addAutoProlongationToOffer: jest.fn(),
    setAutoBoostSchedule: jest.fn(),
}));

let paymentInitParams;
const mockProcessPayment = jest.fn(() => Promise.resolve());

jest.mock('auto-core/lib/billing/payment', () => {
    return jest.fn((...args) => {
        paymentInitParams = args;
        return {
            processPayment: mockProcessPayment,
            options: args[2],
        };
    });
});

const logVasEventPromise = Promise.resolve();
const mockLogVasEvent = jest.fn(() => logVasEventPromise);
jest.mock('auto-core/lib/util/vas/logger', () => {
    return {
        'default': jest.fn(() => ({
            logVasEvent: mockLogVasEvent,
        })),
    };
});

jest.mock('auto-core/react/dataDomain/billing/actions/setActiveTransaction');
jest.mock('auto-core/react/dataDomain/billing/actions/initTransaction');

const mockCreateApplePay = jest.fn();
jest.mock('auto-core/lib/billing/createApplePaySession', () => {
    return { 'default': mockCreateApplePay };
});

require('autoru-frontend/mocks/applePaySessionMock');

const _ = require('lodash');
const React = require('react');
const Billing = require('./Billing');
const BillingFrameSberbank = require('auto-core/react/components/common/Billing/BillingFrameSberbank');
const BillingPaymentStatus = require('auto-core/react/components/common/Billing/BillingPaymentStatus');

const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const billingStateMock = require('auto-core/react/dataDomain/billing/mocks/billing');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const autoProlongationActions = require('auto-core/react/dataDomain/billing/actions/autoProlongation');
const setActiveTransaction = require('auto-core/react/dataDomain/billing/actions/setActiveTransaction');
const initTransaction = require('auto-core/react/dataDomain/billing/actions/initTransaction');

const Payment = require('auto-core/lib/billing/payment');
const { ERRORS, WALLET_METHOD } = require('auto-core/lib/billing/utils');
const { formatTiedCardInfo, methods } = require('autoru-frontend/mockData/responses/billing/paymentMethods.mock');
const { cards, serviceMock } = require('auto-core/react/components/common/Billing/Billing.mock');
const VasLogger = require('auto-core/lib/util/vas/logger').default;

const RETURN_URL = 'http://example.com';
const PAYMENT_TOKEN = 'very-secret-string';

let initialState;
let props;
let store;
let context;
const eventMap = {};
let originalWindowAddEventListener;
let originalWindowPostMessage;

// Добавляем Apple Pay в моки тут, потому что он нигде больше не нужен
// (и ломает тесты десктопа)
billingStateMock.paymentInfo[0].payment_methods.push({
    ps_id: 'YANDEXKASSA_V3',
    id: 'apple_pay',
    name: 'Apple Pay',
});

const postMessageMock = jest.fn();

setActiveTransaction.mockImplementation(() => () => {});

beforeEach(() => {
    const config = configStateMock.value();

    config.data.pageParams.from = 'new-lk-tab';
    config.data.pageParams.returnUrl = decodeURIComponent(JSON.stringify(RETURN_URL));
    config.data.pageParams.offerId = '1234567-abcdef';
    config.data.pageParams.product = decodeURIComponent(JSON.stringify([ { name: 'package_turbo', count: 1 } ]));

    initialState = {
        billing: _.cloneDeep(billingStateMock),
        config: config,
        appName: 'af-mobile',
    };

    props = {
        params: {},
    };

    context = _.cloneDeep(contextMock);

    originalWindowPostMessage = global.postMessage;

    global.addEventListener = jest.fn((event, cb) => {
        eventMap[event] = cb;
    });
    global.postMessage = postMessageMock;

    Payment.mockClear();
    VasLogger.mockClear();
    mockProcessPayment.mockClear();
    postMessageMock.mockClear();
});

afterEach(() => {
    global.addEventListener = originalWindowAddEventListener;
    global.postMessage = originalWindowPostMessage;
});

describe('при оплате новой картой', () => {
    let page;
    let newCardFrame;
    let billingBlock;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
        // Здесь важно сымитировать реальный расклад - новая карта может быть не в начале.
        // Обычно в начале привязанная карта
        putPaymentMethodToFirstPosition(formatTiedCardInfo(methods.newApiTiedCard2));
        page = shallowRenderBilling();

        billingBlock = page.dive();
        const paymentMethodsBlock = billingBlock.find('BillingPaymentMethodsMobile');
        paymentMethodsBlock.simulate('addCardButtonClick');
        billingBlock.update();
        newCardFrame = billingBlock.find('.BillingMobile__cardFrame');
    });

    it('покажет фрейм "оплата новой картой"', () => {
        expect(newCardFrame).toHaveLength(1);
        expect(shallowToJson(newCardFrame)).toMatchSnapshot();
    });

    it('по-умолчанию поставит чекбокс "запомнить карту"', () => {
        const checkbox = billingBlock.find('Checkbox');
        expect(checkbox.prop('checked')).toBe(true);
    });

    it('правильно инициирует платеж при получении сообщения от фрейма', () => {
        eventMap.message({ data: { source: 'card_frame', type: 'token', payload: PAYMENT_TOKEN } });

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });

    it('правильно инициирует платеж при получении сообщения от фрейма если пользователь не хочет запоминать карту', () => {
        const checkbox = billingBlock.find('Checkbox');
        checkbox.simulate('check', false);

        eventMap.message({ data: { source: 'card_frame', type: 'token', payload: PAYMENT_TOKEN } });

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });

    describe('если произошла ошибка в работе библиотеки checkout.js', () => {
        beforeEach(() => {
            eventMap.message({ data: { source: 'card_frame', type: 'error', payload: ERRORS.ya_checkout_fail } });
        });

        it('покажет фрейм с ошибкой', () => {
            const paymentStatusFrame = page.dive().find('BillingPaymentStatus');
            expect(paymentStatusFrame.prop('error')).toEqual({ type: ERRORS.ya_checkout_fail, error: ERRORS.ya_checkout_fail });
        });

        it('залогирует ошибку', () => {
            expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
            expect(mockLogVasEvent.mock.calls[0][0]).toMatchSnapshot();
        });
    });
});

describe('если доступна оплата кошельком', () => {
    let button;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
        initialState.billing.paymentInfo[0].account_balance = 2000;
        putPaymentMethodToFirstPosition(WALLET_METHOD);

        const page = shallowRenderBilling();
        const billingBlock = page.dive();

        const paymentMethodsBlock = billingBlock.find('BillingPaymentMethodsMobile');
        paymentMethodsBlock.simulate('methodChange', 'wallet');

        button = paymentMethodsBlock.dive().find('Button');
    });

    it('инициирует платеж при клике на кнопку', () => {
        button.simulate('click');

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });
});

it('если не пришли методы оплаты, покажет ошибку', () => {
    initialState.billing.paymentInfo = [];
    initialState.billing.initError = { type: 'init_fail' };
    const page = shallowRenderBilling();

    const billingStatusFrame = page.dive().find(BillingPaymentStatus);

    expect(billingStatusFrame.exists()).toBe(true);
    expect(billingStatusFrame.prop('error')).toEqual({ type: 'init_fail' });
});

describe('если есть привязанная карта', () => {
    let page;
    let paymentMethodsBlock;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
        putPaymentMethodToFirstPosition(methods.bankCard);
        initialState.billing.paymentInfo[0].payment_methods.unshift(cards.api_v3);

        page = shallowRenderBilling();
        paymentMethodsBlock = page.dive().find('BillingPaymentMethodsMobile');
    });

    it('то предложит оплатить ей', () => {
        expect(page.dive().state().preSelectedMethod).toEqual(cards.api_v3);
    });

    it('если пользователь выберет новую карту то покажет фрейм новой карты', () => {
        const billingBlock = page.dive();
        billingBlock.find('BillingPaymentMethodsMobile').simulate('addCardButtonClick');
        const newCardFrame = billingBlock.find('.BillingMobile__cardFrame');
        expect(newCardFrame).toHaveLength(1);
    });

    it('правильно инициирует платеж при клике на кнопку', () => {
        paymentMethodsBlock.dive().find('Button').simulate('click');
        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });
});

describe('при оплате сбербанком', () => {
    let sberbankFrame;
    let paymentMethodsBlock;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());

        const page = shallowRenderBilling();
        const billingBlock = page.dive();
        paymentMethodsBlock = billingBlock.find('BillingPaymentMethodsMobile');
        paymentMethodsBlock.simulate('methodChange', methods.sberbank.id);
        billingBlock.setProps({ selectedMethodId: methods.sberbank.id });
        sberbankFrame = billingBlock.find(BillingFrameSberbank);
    });

    it('покажет фрейм "оплата сбербанком" после выбора метода оплаты', () => {
        expect(sberbankFrame).toHaveLength(1);
    });

    it('правильно инициирует платеж при клике на кнопку', () => {
        sberbankFrame.simulate('payButtonClick', '79771234567');

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });
});

it('при оплате сторонним способом при выборе метода сразу инициирует платеж', () => {
    mockProcessPayment.mockImplementation(() => Promise.resolve());
    const page = shallowRenderBilling();
    const paymentMethodsBlock = page.dive().find('BillingPaymentMethodsMobile');
    paymentMethodsBlock.simulate('methodChange', methods.yandexMoney.id);

    expect(paymentInitParams).toMatchSnapshot();
    expect(mockProcessPayment).toHaveBeenCalledTimes(1);
});

describe('при оплате Apple Pay', () => {
    let page;

    beforeEach(() => {
        mockCreateApplePay.mockClear();
    });

    const manualBeforeEach = () => {
        page = shallowRenderBilling();
        const paymentMethodsBlock = page.dive().find('BillingPaymentMethodsMobile');
        paymentMethodsBlock.simulate('methodChange', methods.applePay.id);
    };

    it('при выборе метода сразу инициирует ApplePaySession', () => {
        manualBeforeEach();
        expect(mockCreateApplePay.mock.calls).toMatchSnapshot();
    });

    it('при успешной авторизации платежа со стороны Apple Pay инициирует наш платеж', () => {
        mockCreateApplePay.mockImplementationOnce(({ onAuthorizationComplete }) => {
            onAuthorizationComplete('APPLE_PAY_TOKEN');
        });
        manualBeforeEach();

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });

    it('при ошибке валидации мерчанта, покажем ошибку', () => {
        mockCreateApplePay.mockImplementationOnce(({ onValidationError }) => {
            onValidationError('APPLE_PAY_MERCHANT_VALIDATION_ERROR');
        });
        manualBeforeEach();

        const billingStatusFrame = page.dive().find(BillingPaymentStatus);
        expect(shallowToJson(billingStatusFrame)).toMatchSnapshot();
    });
});

it('при привязке карты чекбокс "запомнить карту" будет задизейблен', () => {
    initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.bindCard ];
    initialState.billing.paymentInfo[0].cost = 1;

    const page = shallowRenderBilling();
    const billingBlock = page.dive();
    const paymentMethodsBlock = billingBlock.find('BillingPaymentMethodsMobile');
    paymentMethodsBlock.simulate('addCardButtonClick');
    const checkbox = billingBlock.find('Checkbox');

    expect(checkbox.prop('disabled')).toBe(true);
});

describe('блок автопродления до оплаты', () => {
    it(`нарисуется если оплачивается один сервис, он автопродляемый,
        это не "Поднятие в поиске", у пользователя есть привязанные карты, это не форма добавления объявления`, () => {
        addAutoProlongationConditionsToState();

        const page = shallowRenderBilling();
        expect(page.dive().find('BillingAutoProlongation')).toHaveLength(1);
    });

    it(`нарисуется если оплачивается активация, она автопродляемая, при этом все остальные условия не важны`, () => {
        initialState.billing.paymentInfo[0].detailed_product_infos = [ { ...serviceMock.activationService }, { ...serviceMock.turboPackageService } ];
        initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = true;
        const methods = initialState.billing.paymentInfo[0].payment_methods;
        initialState.billing.paymentInfo[0].payment_methods = methods.filter(m => !(m.id === 'bank_card' && m.mask));
        props.params.from = 'add_form';

        const page = shallowRenderBilling();
        expect(page.dive().find('BillingAutoProlongation')).toHaveLength(1);
    });

    describe('не нарисуется если хотя бы одно условие не выполнено', () => {
        it('если оплачивается не один сервис', () => {
            addAutoProlongationConditionsToState();
            initialState.billing.paymentInfo[0].detailed_product_infos.push(serviceMock.boostService);

            const page = shallowRenderBilling();
            expect(page.dive().find('BillingAutoProlongation')).toHaveLength(0);
        });

        it('если сервис не автопродляемый', () => {
            addAutoProlongationConditionsToState();
            initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = false;

            const page = shallowRenderBilling();
            expect(page.dive().find('BillingAutoProlongation')).toHaveLength(0);
        });

        it('если у пользователя нет привязанных карт', () => {
            addAutoProlongationConditionsToState();
            const methods = initialState.billing.paymentInfo[0].payment_methods;
            initialState.billing.paymentInfo[0].payment_methods = methods.filter(m => !(m.id === 'bank_card' && m.mask));

            const page = shallowRenderBilling();
            expect(page.dive().find('BillingAutoProlongation')).toHaveLength(0);
        });

        it('если оплата происходит на форме добавления', () => {
            addAutoProlongationConditionsToState();
            props.params.from = 'add_form';

            const page = shallowRenderBilling();
            expect(page.dive().find('BillingAutoProlongation')).toHaveLength(0);
        });
    });

    describe('если в бэка придет флаг "prolongation_forced"', () => {
        let page;
        let spy;

        beforeEach(() => {
            addAutoProlongationConditionsToState();
            initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_forced = true;
            spy = jest.fn(() => () => {});
            autoProlongationActions.toggleAutoProlongationToTransaction.mockImplementationOnce(spy);

            page = shallowRenderBilling();
        });

        it('взведет чекбокс', () => {
            const checkboxValue = page.prop('autoProlongationChecked');
            expect(checkboxValue).toBe(true);
        });

        it('вызовет коллбэк с корректным параметром', () => {
            expect(spy).toHaveBeenCalledTimes(1);
            expect(spy).toHaveBeenCalledWith(true);
        });
    });
});

//TODO Я поправлю эти тесты - здесь стейт не успевает изменится до вызова expect
/* eslint-disable jest/no-disabled-tests */
describe.skip('при неудачной оплате', () => {
    it('покажет окно ошибки', () => {
        const paymentProcessPromise = Promise.reject({ type: 'foo' });
        mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);

        const page = simulateTiedCardPay();

        return paymentProcessPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                const billingStatusFrame = page.dive().find(BillingPaymentStatus);
                expect(shallowToJson(billingStatusFrame)).toMatchSnapshot();
            },
        );
    });

    it('если бэк вернул текст ошибки, отобразит её', () => {
        const paymentProcessPromise = Promise.reject({ body: { description_ru: 'foo' } });
        mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);

        const page = simulateTiedCardPay();

        return paymentProcessPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                const billingStatusFrame = page.dive().find(BillingPaymentStatus);
                expect(billingStatusFrame.prop('error')).toEqual({ description_ru: 'foo', type: ERRORS.payment_fail });
            },
        );
    });
});

describe('при размещении объявы перекупами с 7мью днями', () => {
    let paymentProcessPromise;
    const autoProlongationSuccessPromise = Promise.resolve({ status: 'SUCCESS' });

    beforeEach(() => {
        initialState.billing.paymentInfo[0].detailed_product_infos = [
            _.cloneDeep(serviceMock.activationService),
            _.cloneDeep(serviceMock.turboPackageService),
        ];
        initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_forced_not_togglable = true;
        const methods = initialState.billing.paymentInfo[0].payment_methods;
        initialState.billing.paymentInfo[0].payment_methods = methods.filter(m => !(m.id === 'bank_card' && m.mask));
        props.params.from = 'add_form';

        paymentProcessPromise = Promise.resolve();
        mockProcessPayment.mockImplementation(() => paymentProcessPromise);
    });

    it('не нарисует блок автопродления до оплаты', () => {
        const page = shallowRenderBilling();
        expect(page.dive().find('BillingAutoProlongation')).toHaveLength(0);
    });

    it('не нарисует блок автопродления после оплаты', () => {
        autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(jest.fn(() => () => autoProlongationSuccessPromise));
        const page = simulateWalletPay();

        return paymentProcessPromise
            .then(() => { })
            .then(() => {
                const paymentStatusFrame = page.dive().find(BillingPaymentStatus);
                expect(paymentStatusFrame.prop('autoProlongableService')).toBeUndefined();
            });
    });

    describe('если есть вип', () => {

        beforeEach(() => {
            initialState.billing.paymentInfo[0].detailed_product_infos[1] = serviceMock.vipPackageService;
            autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(jest.fn(() => () => autoProlongationSuccessPromise));
        });

        it('не будет показывать чекбокс автопродления до оплаты', () => {
            const page = shallowRenderBilling();
            expect(page.dive().find('BillingAutoProlongation')).toHaveLength(0);
        });

        it('не будет показывать экран автопродления после оплаты', () => {
            const page = simulateWalletPay();
            const paymentStatusFrame = page.dive().find(BillingPaymentStatus);

            return paymentProcessPromise
                .then(() => { })
                .then(() => {
                    expect(paymentStatusFrame.prop('autoProlongableService')).toBeUndefined();
                });
        });

        it('не будет подключать автопродление для размещения', () => {
            simulateWalletPay();
            return paymentProcessPromise
                .then(() => { })
                .then(() => {
                    expect(autoProlongationActions.addAutoProlongationToOffer).not.toHaveBeenCalled();
                });
        });

    });
});

describe('пакеты "отчёт по вину"', () => {
    it('нарисует плашку если у пользователя нет подписок и оплачивается сервис "отчёт по вину" и передаст в нее инфо о пакетах', () => {
        initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
        initialState.billing.reportsSubscription = undefined;

        const page = shallowRenderBilling();

        expect(shallowToJson(page.dive().find('BillingVinReportSelector'))).toMatchSnapshot();
    });

    it('нарисует, если forceShowBundleSelector и инициировали с пакетом', () => {
        initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
        initialState.config.data.pageParams.forceShowBundleSelector = true;
        initialState.billing.reportsBundles[2].ticketId = initialState.billing.reportsBundles[0].ticketId;
        delete initialState.billing.reportsSubscription;
        delete initialState.billing.reportsBundles[0].ticketId;

        const page = shallowRenderBilling();
        expect(page.dive().find('BillingVinReportSelector').exists()).toBe(true);
        expect(page.dive().find('BillingHeader').prop('subtitleData')).toBeUndefined();
    });

    describe('не нарисует плашку', () => {
        it('если нет forceShowBundleSelector, но инициировали с пакетом', () => {
            initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
            initialState.config.data.pageParams.forceShowBundleSelector = false;
            initialState.billing.reportsBundles[2].ticketId = initialState.billing.reportsBundles[0].ticketId;
            delete initialState.billing.reportsBundles[0].ticketId;
            delete initialState.billing.reportsSubscription;

            const page = shallowRenderBilling();
            expect(page.dive().find('BillingVinReportSelector').exists()).toBe(false);
            expect(page.dive().find('BillingHeader').prop('subtitleData')).toMatchSnapshot();
        });

        it('если оплачивается не "отчёт по вину"', () => {
            initialState.billing.reportsSubscription = undefined;

            const page = shallowRenderBilling();
            expect(page.dive().find('BillingVinReportSelector')).toHaveLength(0);
        });

        it('если нет активных бандлов', () => {
            initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
            initialState.billing.reportsSubscription = undefined;
            initialState.billing.reportsBundles = [];

            const page = shallowRenderBilling();
            expect(page.dive().find('BillingVinReportSelector')).toHaveLength(0);
        });
    });

    describe('при клике на таб', () => {
        const initTransactionPromise = Promise.resolve();
        let bundleCount;
        let page;

        beforeEach(() => {
            initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
            initialState.billing.reportsSubscription = undefined;
            bundleCount = initialState.billing.reportsBundles[1].counter;

            initTransaction.mockImplementation(() => () => initTransactionPromise);

            page = shallowRenderBilling();
            const vinReportSelector = page.dive().find('BillingVinReportSelector');
            vinReportSelector.simulate('tabChange', bundleCount);
        });

        it('выберет его', () => {
            expect(page.dive().find('BillingVinReportSelector').prop('selectedBundle')).toBe(bundleCount);
        });

        it('отправит метрику bundle_tab_change', () => {
            expect(context.metrika.sendPageEvent).toHaveBeenCalled();
            expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'bundle_tab_change', 'bundle_5' ]);
        });

        it('если транзакция не была инициализирована отправит запрос на инициализацию', () => {
            expect(initTransaction).toHaveBeenCalledTimes(1);
            expect(initTransaction.mock.calls[0]).toMatchSnapshot();
        });

        it('если транзакция была инициализирована ранее ничего не будет делать', () => {
            const vinReportSelector = page.dive().find('BillingVinReportSelector');
            vinReportSelector.simulate('tabChange', '1');

            expect(initTransaction).toHaveBeenCalledTimes(1);

            vinReportSelector.simulate('tabChange', bundleCount);

            expect(initTransaction).toHaveBeenCalledTimes(1);
        });
    });

    it('если при инициализации транзакции произошла ошибка покажет лоадер а потом сообщение о ней', () => {
        const bundleCount = initialState.billing.reportsBundles[1].counter;
        initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
        initialState.billing.reportsSubscription = undefined;
        const initTransactionPromise = Promise.reject();
        initTransaction.mockImplementation(() => () => initTransactionPromise);

        const page = shallowRenderBilling();
        const vinReportSelector = page.dive().find('BillingVinReportSelector');
        vinReportSelector.simulate('tabChange', bundleCount);

        return initTransactionPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                expect(page.dive().find('.BillingMobile__contentLoader')).toHaveLength(1);
                expect(page.dive().find('BillingPaymentMethodsMobile')).toHaveLength(0);
                expect(page.dive().find(BillingPaymentStatus)).toHaveLength(0);
            },
        )
            .then(() => {
                expect(page.dive().find(BillingPaymentStatus)).toHaveLength(1);
            });
    });
});

function shallowRenderBilling() {
    store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <Billing { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return wrapper.dive().dive().dive();
}

function addAutoProlongationConditionsToState() {
    initialState.billing.paymentInfo[0].detailed_product_infos = [ _.cloneDeep(serviceMock.turboPackageService) ];
    initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = true;
    initialState.billing.paymentInfo[0].payment_methods.push(cards.api_v3);
    props.params.from = 'new-lk-tab';
}

function putPaymentMethodToFirstPosition(method) {
    const newMethods = initialState.billing.paymentInfo[0].payment_methods.filter(({ id }) => id !== method.id);
    initialState.billing.paymentInfo[0].payment_methods = [ method, ...newMethods ];
}

function simulateTiedCardPay() {
    initialState.billing.paymentInfo[0].payment_methods.push(cards.api_v3);
    const page = shallowRenderBilling();

    const paymentMethodsBlock = page.dive().find('BillingPaymentMethodsMobile');
    paymentMethodsBlock.simulate('methodChange', 'bank_card');

    paymentMethodsBlock.simulate('payButtonClick', cards.api_v3);

    return page;
}

function simulateWalletPay() {
    initialState.billing.paymentInfo[0].account_balance = 20000;
    putPaymentMethodToFirstPosition(WALLET_METHOD);

    const page = shallowRenderBilling();

    // Выбираем кошелек чтобы показался его фрейм
    const paymentMethodsBlock = page.dive().find('BillingPaymentMethodsMobile').dive();
    paymentMethodsBlock.simulate('methodChange', WALLET_METHOD.id);

    // Тыкаем в кнопку "оплатить"
    paymentMethodsBlock.find('Button').simulate('click');

    return page;
}
