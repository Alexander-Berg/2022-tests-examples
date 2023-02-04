/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/billing/actions/autoProlongation.js', () => ({
    toggleAutoProlongationToTransaction: jest.fn(() => () => Promise.resolve()),
    addAutoProlongationToOffer: jest.fn(() => () => Promise.resolve()),
    setAutoBoostSchedule: jest.fn(() => () => Promise.resolve()),
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

const _ = require('lodash');
const React = require('react');
const Billing = require('./Billing');
const BillingFrameTiedCards = require('./BillingFrameTiedCards');
const BillingFrameWallet = require('./BillingFrameWallet');
const BillingFrameSberbank = require('./BillingFrameSberbank');
const BillingFooter = require('./BillingFooter');
const BillingPaymentMethods = require('./BillingPaymentMethods');
const BillingPaymentStatus = require('./BillingPaymentStatus');
const Button = require('auto-core/react/components/islands/Button');

const MockDate = require('mockdate');
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
const { ERRORS, PAYMENT_MESSAGE_TYPES, WALLET_METHOD } = require('auto-core/lib/billing/utils');
const { TBillingFrom, TBillingPlatform } = require('auto-core/types/TBilling');

const { methods } = require('autoru-frontend/mockData/responses/billing/paymentMethods.mock');
const { cards, serviceMock } = require('./Billing.mock');
const VasLogger = require('auto-core/lib/util/vas/logger').default;

const RETURN_URL = 'link/billing_return_page/?';
const PAYMENT_TOKEN = 'very-secret-string';
const FRAME_ORIGIN = 'https://auto.ru';
const TICKET_ID = 'my-ticket-id';

let initialState;
let props;
let store;
let context;
const eventMap = {};
let originalWindowAddEventListener;
let originalWindowPostMessage;

const postMessageMock = jest.fn();

setActiveTransaction.mockImplementation(() => () => {});

beforeEach(() => {
    const config = configStateMock.withPageParams({
        from: 'new-lk-tab',
        returnUrl: decodeURIComponent(JSON.stringify(RETURN_URL)),
        offerId: '1234567-abcdef',
        product: decodeURIComponent(JSON.stringify([ { name: 'package_turbo', count: 1 } ])),
    }).value();

    initialState = {
        billing: _.cloneDeep(billingStateMock),
        config: config,
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

    // initTransaction.mockReset();
    MockDate.reset();
});

it('правильно рисует компонент BillingDumb', () => {
    const page = shallowRenderBilling();
    expect(shallowToJson(page.dive())).toMatchSnapshot();
});

it('при маунте уведомит родительский фрейм о том что все готово', () => {
    shallowRenderBilling();
    expect(postMessageMock).toHaveBeenCalledTimes(1);
    expect(postMessageMock).toHaveBeenCalledWith(
        { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.frame_loaded },
        FRAME_ORIGIN,
    );
});

it('в мобильном поффере origin в postMessage будет без префикса m, то есть такой же как на десктопе', () => {
    initialState.config.from = TBillingFrom.FORM_BETA_ADD;
    initialState.config.data.browser.isMobile = true;
    initialState.config.data.host = 'm.auto.ru';

    props.params.from = TBillingFrom.FORM_BETA_ADD;
    props.params.platform = TBillingPlatform.MOBILE;
    props.params.host = 'auto.ru';

    shallowRenderBilling();
    expect(postMessageMock).toHaveBeenCalledTimes(1);
    expect(postMessageMock).toHaveBeenCalledWith(
        { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.frame_loaded },
        FRAME_ORIGIN,
    );
});

it('правильно инициализирует вас логгер', () => {
    initialState.config.data.pageParams.category = 'trucks';

    shallowRenderBilling();
    expect(VasLogger.mock.calls[0][1]).toMatchSnapshot();
});

it('если при инициализации бэк вернул ошибку залогирует её', () => {
    initialState.billing.paymentInfo = undefined;
    initialState.billing.initError = { type: ERRORS.init_fail, error: 'BAD_REQUEST' };
    shallowRenderBilling();

    expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
    expect(mockLogVasEvent.mock.calls[0][0]).toMatchSnapshot();
});

describe('при оплате новой картой', () => {
    let page;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
        putPaymentMethodToFirstPosition(methods.bankCard);
        page = shallowRenderBilling();
    });

    it('покажет фрейм "оплата новой картой"', () => {
        const newCardFrame = page.dive().find('.Billing__cardFrame');
        expect(newCardFrame).toHaveLength(1);
        expect(shallowToJson(newCardFrame)).toMatchSnapshot();
    });

    it('по-умолчанию поставит чекбокс "запомнить карту"', () => {
        const checkboxChecked = page.dive().find(BillingFooter).prop('checkboxChecked');
        expect(checkboxChecked).toBe(true);
    });

    it('правильно инициирует платеж при получении сообщения от фрейма', () => {
        eventMap.message({ data: { source: 'card_frame', type: 'token', payload: PAYMENT_TOKEN } });

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });

    it('правильно инициирует платеж при получении сообщения от фрейма если пользователь не хочет запоминать карту', () => {
        const footer = page.dive().find(BillingFooter);
        footer.simulate('rememberCardToggle', false);

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
    let walletFrame;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
        initialState.billing.paymentInfo[0].account_balance = 2000;
        putPaymentMethodToFirstPosition(WALLET_METHOD);

        const page = shallowRenderBilling();
        walletFrame = page.dive().find(BillingFrameWallet);
    });

    it('покажет фрейм "оплата кошельком"', () => {
        expect(walletFrame).toHaveLength(1);
    });

    it('правильно инициирует платеж при клике на кнопку', () => {
        walletFrame.simulate('payButtonClick');

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });
});

describe('если есть привязанная карта', () => {
    let page;
    let tiedCardsFrame;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
        putPaymentMethodToFirstPosition(methods.bankCard);
        initialState.billing.paymentInfo[0].payment_methods.push(cards.api_v3);

        page = shallowRenderBilling();
        tiedCardsFrame = page.dive().find('BillingFrameTiedCards');
    });

    it('то предложит оплатить ей', () => {
        expect(tiedCardsFrame).toHaveLength(1);
    });

    it('если пользователь выберет новую карту то покажет фрейм новой карты', () => {
        const billing = page.dive();
        tiedCardsFrame = billing.find('BillingFrameTiedCards');
        tiedCardsFrame.simulate('addCardButtonClick');

        const newCardFrame = billing.find('.Billing__cardFrame');
        expect(newCardFrame).toHaveLength(1);
    });

    it('правильно инициирует платеж при клике на кнопку', () => {
        tiedCardsFrame.simulate('payButtonClick', cards.api_v3);

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });
});

describe('при оплате сбербанком', () => {
    let sberbankFrame;

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());

        const page = shallowRenderBilling();
        const paymentMethods = page.dive().find(BillingPaymentMethods);
        paymentMethods.simulate('methodChange', methods.sberbank.id);
        sberbankFrame = page.dive().find(BillingFrameSberbank);
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
    const paymentMethods = page.dive().find(BillingPaymentMethods);
    paymentMethods.simulate('methodChange', methods.yandexMoney.id);

    expect(paymentInitParams).toMatchSnapshot();
    expect(mockProcessPayment).toHaveBeenCalledTimes(1);
});

it('при привязке карты чекбокс "запомнить карту" будет задизейблен', () => {
    initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.bindCard ];
    initialState.billing.paymentInfo[0].cost = 1;

    const page = shallowRenderBilling();
    const checkboxDisabled = page.dive().find(BillingFooter).prop('checkboxDisabled');

    expect(checkboxDisabled).toBe(true);
});

describe('блок автопродления до оплаты', () => {
    it('нарисуется если оплачивается один сервис, он автопродляемый, это не "Поднятие в поиске"' +
        ', у пользователя есть привязанные карты, это не форма добавления объявления', () => {
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

    describe('для "Поднятия в поиске"', () => {
        beforeEach(() => {
            addAutoProlongationConditionsToState();
            initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.boostService ];
            initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = true;
        });

        it('взведет чекбокс по-умолчанию', () => {
            const page = shallowRenderBilling();
            const checkbox = page.dive().find('BillingAutoProlongation');

            expect(checkbox).toHaveLength(1);
            expect(checkbox.prop('checked')).toBe(true);
        });

        it('не будет дергать ручку продления при маунте', () => {
            shallowRenderBilling();
            expect(autoProlongationActions.toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(0);
        });

        it('не будет дергать ручку продления при клике на чекбокс', () => {
            const page = shallowRenderBilling();
            const checkbox = page.dive().find('BillingAutoProlongation');
            checkbox.simulate('check', false);
            expect(autoProlongationActions.toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(0);
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
            const checkbox = page.dive().find('BillingAutoProlongation').dive().find('.BillingAutoProlongation__checkbox');
            expect(checkbox.prop('checked')).toBe(true);
        });

        it('вызовет коллбэк с корректным параметром', () => {
            expect(spy).toHaveBeenCalledTimes(1);
            expect(spy).toHaveBeenCalledWith(true);
        });
    });
});

describe('правильно инициирует платеж', () => {
    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
    });

    it('при пополнение баланса кошелька', () => {
        initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.accountRefill ];
        initialState.billing.paymentInfo[0].payment_methods.push(cards.api_v3);

        const page = shallowRenderBilling();
        const tiedCardsFrame = page.dive().find(BillingFrameTiedCards);

        tiedCardsFrame.simulate('payButtonClick', cards.api_v3);

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });

    it('при привязке карты', () => {
        initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.bindCard ];
        initialState.billing.paymentInfo[0].cost = 1;

        shallowRenderBilling();
        eventMap.message({ data: { source: 'card_frame', type: 'token', payload: PAYMENT_TOKEN } });

        expect(paymentInitParams).toMatchSnapshot();
        expect(mockProcessPayment).toHaveBeenCalledTimes(1);
    });
});

describe('при инициализации платежа', () => {
    const salesmanDomainMock = 'autoservices';

    beforeEach(() => {
        mockProcessPayment.mockImplementation(() => Promise.resolve());
        initialState.billing.paymentInfo[0].salesman_domain = salesmanDomainMock;
        initialState.billing.paymentInfo[0].ticket_id = TICKET_ID;
        initialState.billing.selectedTicketId = TICKET_ID;
    });

    it('передаст ticketId из ответа ручки /billing/init', () => {
        simulateWalletPay();
        const [ ticketId ] = paymentInitParams;
        expect(ticketId).toBe(TICKET_ID);
    });

    it('передаст выбранный метод оплаты', () => {
        simulateWalletPay();
        const [ , paymentMethod ] = paymentInitParams;
        expect(paymentMethod).toBe(WALLET_METHOD);
    });

    it('передаст salesmanDomain из ответа ручки /billing/init', () => {
        simulateWalletPay();
        const [ , , options ] = paymentInitParams;
        expect(options.salesmanDomain).toBe(salesmanDomainMock);
    });

    describe('передаст корректный урл для возврата', () => {
        const passedReturnUrl = encodeURIComponent(JSON.stringify(RETURN_URL));
        beforeEach(() => {
            initialState.config.data.pageParams.returnUrl = passedReturnUrl;
        });

        it('если оплата не картой то урл не будет содержать параметров', () => {
            simulateWalletPay();
            const [ , , options ] = paymentInitParams;
            expect(options.returnUrl).toBe(RETURN_URL);
        });

        it('если оплата картой и нет автопродляемого сервиса то урл будет содержать только параметр для уведомления', () => {
            addAutoProlongationConditionsToState();
            initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = false;

            simulateTiedCardPay();
            const [ , , options ] = paymentInitParams;
            expect(options.returnUrl).toMatchSnapshot();
        });

        it('если оплата картой и оплачивается вин-истоиря то урл будет содержать параметр для уведомления и подскролла', () => {
            initialState.billing.paymentInfo[0].detailed_product_infos = [ { service: 'offers-history-reports', name: '' } ];

            simulateTiedCardPay();
            const [ , , options ] = paymentInitParams;
            expect(options.returnUrl).toMatchSnapshot();
        });

        it('если оплата картой и есть автопродляемый сервис то урл будет содержать параметры для окна оплаты', () => {
            addAutoProlongationConditionsToState();

            simulateTiedCardPay();
            const [ , , options ] = paymentInitParams;
            expect(options.returnUrl).toMatchSnapshot();
        });
    });

    describe('при оплате картой с цвц-подтверждением', () => {
        const CVC_TOKEN = 'token';
        const cardWithCvcConfirmation = {
            ...cards.api_v3,
            verification_required: true,
        };
        let paymentMethod;
        let options;

        beforeEach(() => {
            initialState.billing.paymentInfo[0].payment_methods.push(cardWithCvcConfirmation);
            const page = shallowRenderBilling();

            const tiedCardsFrame = page.dive().find(BillingFrameTiedCards);
            tiedCardsFrame.simulate('payButtonClick', { ...cardWithCvcConfirmation, cvcToken: CVC_TOKEN });

            ([ , paymentMethod, options ] = paymentInitParams);
        });

        it('добавит в paymentMethod цвц-токен', () => {
            expect(paymentMethod.cvcToken).toBe(CVC_TOKEN);
        });

        it('правильно сформирует урл возврата', () => {
            expect(options.returnUrl).toMatchSnapshot();
        });
    });
});

describe('после успешной оплаты', () => {
    let paymentProcessPromise;

    beforeEach(() => {
        paymentProcessPromise = Promise.resolve();
        mockProcessPayment.mockImplementation(() => paymentProcessPromise);
    });

    it('уведомит родительское окно', () => {
        simulateTiedCardPay();
        return paymentProcessPromise
            .then(() => {})
            .then(() => {
                expect(postMessageMock).toHaveBeenCalledTimes(2);
                expect(postMessageMock).toHaveBeenCalledWith(
                    { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.payment_success, payload: { purchaseCount: 1 } },
                    FRAME_ORIGIN,
                );
            });
    });

    it('правильно залогирует событие успеха', () => {
        simulateTiedCardPay();
        return paymentProcessPromise
            .then(() => { })
            .then(() => {
                expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
                expect(mockLogVasEvent.mock.calls[0][0]).toMatchSnapshot();
            });
    });

    it('если нет автопродляемого сервиса покажет обычный экран', () => {
        addAutoProlongationConditionsToState();
        initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = false;

        const page = simulateTiedCardPay().dive();
        return paymentProcessPromise
            .then(() => { })
            .then(() => {
                // Стейт не успевает обновиться
                setTimeout(() => expect(shallowToJson(page)).toMatchSnapshot(), 0);
            });
    });

    describe('если есть автопродляемый сервис', () => {
        let page;
        let paymentStatusFrame;
        const autoProlongationPromise = Promise.resolve({ status: 'SUCCESS' });
        const autoProlongationSpy = jest.fn(() => () => autoProlongationPromise);
        const service = 'package_turbo';

        beforeEach(() => {
            autoProlongationSpy.mockClear();
            autoProlongationActions.addAutoProlongationToOffer.mockClear();
            autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(autoProlongationSpy);
            addAutoProlongationConditionsToState();

            page = simulateTiedCardPay();
            paymentStatusFrame = page.dive().find(BillingPaymentStatus);
        });

        it('покажет экран с автопродлением', () => {
            const updatedPaymentStatusFrame = page.dive().find(BillingPaymentStatus);

            return paymentProcessPromise
                .then(() => { })
                .then(() => {
                    expect(shallowToJson(updatedPaymentStatusFrame)).toMatchSnapshot();
                });
        });

        it('позволит подключить автопродление', () => {
            paymentStatusFrame.simulate('autoProlongationButtonClick', service);

            expect(autoProlongationSpy).toHaveBeenCalledTimes(1);
            expect(autoProlongationSpy).toHaveBeenCalledWith(service);
        });

        it('уведомит родительское окно об успешном подключении автопродления', () => {
            paymentStatusFrame.simulate('autoProlongationButtonClick', service);

            return autoProlongationPromise
                .then(() => {
                    expect(postMessageMock).toHaveBeenCalledTimes(3);
                    expect(postMessageMock.mock.calls[2]).toMatchSnapshot();
                });
        });
    });

    it('если галку с автоподнятие до оплаты не отжали, подключит расписание на начало текущего часа', () => {
        const autoBoostPromise = Promise.resolve({ status: 'SUCCESS' });
        const autoBoostSpy = jest.fn(() => () => autoBoostPromise);
        autoProlongationActions.setAutoBoostSchedule.mockImplementationOnce(autoBoostSpy);
        MockDate.set('2019-02-26T13:13:13.000+03:00');

        initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.boostService ];

        simulateTiedCardPay();

        return paymentProcessPromise
            .then(() => { })
            .then(() => {
                expect(autoBoostSpy).toHaveBeenCalledTimes(1);
                expect(autoBoostSpy).toHaveBeenCalledWith('13:00');
            });
    });

    describe('если есть автоподнятие', () => {
        let page;
        let paymentStatusFrame;
        const autoBoostPromise = Promise.resolve({ status: 'SUCCESS' });
        const autoBoostSpy = jest.fn(() => () => autoBoostPromise);
        const time = '10:00';

        beforeEach(() => {
            autoBoostSpy.mockClear();
            autoProlongationActions.setAutoBoostSchedule.mockImplementationOnce(autoBoostSpy);
            initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.boostService ];
            initialState.billing.paymentInfo[0].payment_methods.push(cards.api_v3);
            props.params.from = 'new-lk-tab';
            page = shallowRenderBilling();

            // чекбокс взведен по-умолчанию, поэтому отжимаем его
            const checkbox = page.dive().find('BillingAutoProlongation');
            checkbox.simulate('check', false);

            const tiedCardsFrame = page.dive().find(BillingFrameTiedCards);
            tiedCardsFrame.simulate('payButtonClick', cards.api_v3);

            paymentStatusFrame = page.dive().find(BillingPaymentStatus);
        });

        it('покажет экран с автоподнятием', () => {
            const updatedPaymentStatusFrame = page.dive().find(BillingPaymentStatus);

            return paymentProcessPromise
                .then(() => { })
                .then(() => {
                    expect(shallowToJson(updatedPaymentStatusFrame)).toMatchSnapshot();
                });
        });

        it('позволит подключить автоподнятие', () => {
            paymentStatusFrame.simulate('autoBoostButtonClick', time);

            expect(autoBoostSpy).toHaveBeenCalledTimes(1);
            expect(autoBoostSpy).toHaveBeenCalledWith(time);
        });

        it('уведомит родительское окно об успешном подключении автоподнятия', () => {
            paymentStatusFrame.simulate('autoBoostButtonClick', time);

            return autoBoostPromise
                .then(() => {
                    expect(postMessageMock).toHaveBeenCalledTimes(3);
                    expect(postMessageMock.mock.calls[2]).toMatchSnapshot();
                });
        });
    });

    it('если не удалось подключить автопродление после оплаты уведомит родительское окно об этом', () => {
        const autoProlongationPromise = Promise.reject({ status: 'ERROR' });
        const autoProlongationSpy = jest.fn(() => () => autoProlongationPromise);
        autoProlongationActions.addAutoProlongationToOffer.mockReset();
        autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(autoProlongationSpy);

        addAutoProlongationConditionsToState();
        const page = simulateTiedCardPay();

        return paymentProcessPromise.then(() => {
            const paymentStatusFrame = page.dive().find(BillingPaymentStatus);
            paymentStatusFrame.simulate('autoProlongationButtonClick', 'package_turbo');

            return autoProlongationPromise.then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                async() => {
                    await new Promise((resolve) => process.nextTick(resolve));
                    expect(postMessageMock).toHaveBeenCalledTimes(3);
                    expect(postMessageMock.mock.calls[2]).toMatchSnapshot();
                },
            );
        });
    });
});

describe('при неудачной оплате', () => {
    it('покажет окно ошибки', () => {
        const paymentProcessPromise = Promise.reject({ type: 'foo' });
        mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);

        const page = simulateTiedCardPay();

        return paymentProcessPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => process.nextTick(resolve));
                const billingStatusFrame = page.dive().find(BillingPaymentStatus);
                expect(shallowToJson(billingStatusFrame)).toMatchSnapshot();
            });
    });

    it('если произошла ошибка платежа залогирует событие ошибки', () => {
        const paymentProcessPromise = Promise.reject({ body: { error: 'FRAUD_SUSPECTED' } });
        mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);
        initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.vinReportService ];
        initialState.config.data.pageParams.purchaseCount = 10;

        simulateTiedCardPay();

        return paymentProcessPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => process.nextTick(resolve));
                expect(mockLogVasEvent).toHaveBeenCalledTimes(4);
                expect(mockLogVasEvent.mock.calls[3][0]).toMatchSnapshot();
            });
    });

    it('если пользователь закрыл окно внешнего платежа залогирует событие отмены и покажет первоначальный экран', () => {
        const paymentProcessPromise = Promise.reject({ type: 'window_closed' });
        mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);
        initialState.billing.paymentInfo[0].detailed_product_infos = [ serviceMock.vinReportService ];
        initialState.config.data.pageParams.purchaseCount = 10;

        const page = shallowRenderBilling();
        const paymentMethods = page.dive().find(BillingPaymentMethods);
        paymentMethods.simulate('methodChange', methods.yandexMoney.id);

        return paymentProcessPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => process.nextTick(resolve));
                const paymentMethods = page.dive().find(BillingPaymentMethods);

                expect(paymentMethods.prop('selectedMethodId')).toBe('bank_card');
                expect(mockLogVasEvent).toHaveBeenCalledTimes(4);
                expect(mockLogVasEvent.mock.calls[3][0]).toMatchSnapshot();
            });
    });

    it('если бэк вернул текст ошибки, отобразит её', () => {
        const paymentProcessPromise = Promise.reject({ body: { description_ru: 'foo' } });
        mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);

        const page = simulateTiedCardPay();

        return paymentProcessPromise.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => process.nextTick(resolve));
                const billingStatusFrame = page.dive().find(BillingPaymentStatus);
                expect(billingStatusFrame.prop('error')).toEqual({ description_ru: 'foo', type: ERRORS.payment_fail });
            });
    });
});

describe('при попытке закрыть окно оплаты', () => {
    let page;

    describe('если платеж не завершен', () => {
        let closeConfirmBlock;

        beforeEach(() => {
            page = shallowRenderBilling();
            eventMap.message({ data: { source: 'parent', type: PAYMENT_MESSAGE_TYPES.close_request } });
            closeConfirmBlock = page.dive().find('BillingCloseConfirm');
        });

        it('покажет диалог подтверждения закрытия', () => {
            expect(closeConfirmBlock).toHaveLength(1);
        });

        describe('при клике на кнопку "да"', () => {
            beforeEach(() => {
                const yesButton = closeConfirmBlock.dive().find(Button).at(1);
                yesButton.simulate('click');
            });

            it('отправит сообщение родителю на закрытие модала', () => {
                return logVasEventPromise
                    .then(() => {})
                    .then(() => {
                        expect(postMessageMock).toHaveBeenCalledTimes(2);
                        expect(postMessageMock).toHaveBeenCalledWith(
                            { source: 'billing_frame', type: PAYMENT_MESSAGE_TYPES.close_request },
                            FRAME_ORIGIN,
                        );
                    });
            });

            it('залогирует событие отказа оплаты', () => {
                expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
                expect(mockLogVasEvent.mock.calls[0][0]).toMatchSnapshot();
            });
        });

        it('при клике на кнопку "нет" скроет диалог подтверждения', () => {
            const noButton = closeConfirmBlock.dive().find(Button).at(0);
            noButton.simulate('click');

            const updatedCloseConfirmBlock = page.find('.Billing__closeConfirm');
            expect(updatedCloseConfirmBlock).toHaveLength(0);
        });
    });

    describe('если платеж завершен успешно', () => {
        let paymentProcessPromise;

        beforeEach(() => {
            paymentProcessPromise = Promise.resolve();
            mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);

            simulateTiedCardPay();
        });

        it('сразу отправит сообщение родителю на закрытие модала', () => {

            return paymentProcessPromise
                .then(() => { })
                .then(() => {
                    eventMap.message({ data: { source: 'parent', type: PAYMENT_MESSAGE_TYPES.close_request } });

                    expect(postMessageMock).toHaveBeenCalledTimes(3);
                    expect(postMessageMock.mock.calls[2]).toMatchSnapshot();
                });
        });

        it('не залогирует это как отказ', () => {
            // 1 подразумевает логирование успеха; кроме этого ничего не должно слаться
            expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
            expect(mockLogVasEvent.mock.calls[0][0].event).toBe('VAS_PURCHASE');
        });
    });

    describe('если платеж завершен с ошибкой', () => {
        let paymentProcessPromise;

        beforeEach(() => {
            paymentProcessPromise = Promise.reject({ type: 'foo' });
            mockProcessPayment.mockImplementationOnce(() => paymentProcessPromise);

            simulateTiedCardPay();
        });

        it('сразу отправит сообщение родителю на закрытие модала', () => {
            return paymentProcessPromise
                .then(() => { })
                .catch(() => {
                    eventMap.message({ data: { source: 'parent', type: PAYMENT_MESSAGE_TYPES.close_request } });
                })
                .then(() => { })
                .then(() => {
                    expect(postMessageMock).toHaveBeenCalledTimes(2);
                    expect(postMessageMock.mock.calls[1]).toMatchSnapshot();
                });
        });

        it('залогирует события отмены оплаты', () => {
            return paymentProcessPromise.then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                async() => {
                    await new Promise((resolve) => process.nextTick(resolve));
                    eventMap.message({ data: { source: 'parent', type: PAYMENT_MESSAGE_TYPES.close_request } });

                    expect(mockLogVasEvent).toHaveBeenCalledTimes(2);
                    expect(mockLogVasEvent.mock.calls[1][0]).toMatchSnapshot();
                });
        });
    });
});

describe('при размещении объявы перекупами с 7мью днями', () => {
    let paymentProcessPromise;
    const autoProlongationSuccessPromise = Promise.resolve({ status: 'SUCCESS' });
    const autoProlongationFailedPromise = Promise.reject({ error: 'PRODUCT_NOT_FOUND' });

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

    describe('если оплачивается более одного сервиса', () => {
        it('отправит экшен на подключение автопродления после успешной оплаты', () => {
            autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(jest.fn(() => () => autoProlongationSuccessPromise));
            simulateWalletPay();

            return paymentProcessPromise
                .then(() => { })
                .then(() => {
                    expect(autoProlongationActions.addAutoProlongationToOffer).toHaveBeenCalledTimes(1);
                    expect(autoProlongationActions.addAutoProlongationToOffer).toHaveBeenCalledWith('all_sale_activate');
                });
        });

        it('отправит метрику после успешной оплаты', () => {
            autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(jest.fn(() => () => autoProlongationSuccessPromise));
            simulateWalletPay();

            return autoProlongationSuccessPromise
                .then(() => { })
                .then(() => { })
                .then(() => {
                    expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
                    expect(context.metrika.sendParams.mock.calls[0][0]).toEqual([ '7days-placement', 'prolongation-turn-on', 'after-payment' ]);
                });
        });

        it('отправит метрику об ошибке если не удалось подключить продление после оплаты', () => {
            autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(jest.fn(() => () => autoProlongationFailedPromise));

            simulateWalletPay();
            return autoProlongationFailedPromise.then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                async() => {
                    await new Promise((resolve) => process.nextTick(resolve));
                    expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
                    expect(context.metrika.sendParams.mock.calls[0][0]).toEqual([ '7days-placement', 'prolongation-errors' ]);
                });
        });
    });

    describe('если оплачивается только активация', () => {
        beforeEach(() => {
            initialState.billing.paymentInfo[0].detailed_product_infos = [ _.cloneDeep(serviceMock.activationService) ];
            initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_forced_not_togglable = true;
        });

        it('отправит экшен на подключение автопродления до оплаты', () => {
            autoProlongationActions.toggleAutoProlongationToTransaction.mockImplementationOnce(jest.fn(() => () => autoProlongationSuccessPromise));
            shallowRenderBilling();

            expect(autoProlongationActions.toggleAutoProlongationToTransaction).toHaveBeenCalledTimes(1);
            expect(autoProlongationActions.toggleAutoProlongationToTransaction).toHaveBeenCalledWith(true);
        });

        it('отправит метрику до оплаты', () => {
            autoProlongationActions.toggleAutoProlongationToTransaction.mockImplementationOnce(jest.fn(() => () => autoProlongationSuccessPromise));
            shallowRenderBilling();

            return autoProlongationSuccessPromise
                .then(() => { })
                .then(() => {
                    expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
                    expect(context.metrika.sendParams.mock.calls[0][0]).toEqual([ '7days-placement', 'prolongation-turn-on', 'with-payment' ]);
                });
        });

        it('отправит метрику об ошибке если не удалось подключить продление до оплаты', () => {
            autoProlongationActions.toggleAutoProlongationToTransaction.mockImplementationOnce(jest.fn(() => () => autoProlongationFailedPromise));
            shallowRenderBilling();

            return autoProlongationFailedPromise.then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                async() => {
                    await new Promise((resolve) => process.nextTick(resolve));
                    expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
                    expect(context.metrika.sendParams.mock.calls[0][0]).toEqual([ '7days-placement', 'prolongation-errors' ]);
                });
        });
    });

    describe('если есть вип', () => {

        beforeEach(() => {
            initialState.billing.paymentInfo[0].detailed_product_infos[1] = serviceMock.vipPackageService;
            autoProlongationActions.addAutoProlongationToOffer.mockImplementationOnce(jest.fn(() => () => autoProlongationSuccessPromise));
        });

        it('не будет показывать чекбокс автопродления до оплаты', () => {
            const page = shallowRenderBilling();
            expect(page.find('.Billing__autoProlongation')).toHaveLength(0);
        });

        it('не будет показывать экран автопродления после оплаты', () => {
            const page = simulateWalletPay();

            return paymentProcessPromise
                .then(() => { })
                .then(() => {
                    const paymentStatusFrame = page.dive().find(BillingPaymentStatus);
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
    it('при маунте отправит метрику для пакетов', () => {
        initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
        initialState.billing.reportsSubscription = undefined;

        shallowRenderBilling();

        expect(mockLogVasEvent).toHaveBeenCalledTimes(3);
        expect(mockLogVasEvent).toHaveBeenNthCalledWith(2, {
            originalPrice: 388,
            price: 388,
            serviceId: 'offers-history-reports-5',
            errorCode: undefined,
            event: 'VAS_SHOW',
        });
        expect(mockLogVasEvent).toHaveBeenNthCalledWith(3, {
            originalPrice: 777,
            price: 777,
            serviceId: 'offers-history-reports-10',
            errorCode: undefined,
            event: 'VAS_SHOW',
        });
    });

    it('отправит метрику только выбранного пакета, когда скрывается bundle selector', () => {
        initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
        initialState.billing.reportsSubscription = undefined;
        initialState.billing.reportsBundles[2].ticketId = initialState.billing.reportsBundles[0].ticketId;
        delete initialState.billing.reportsBundles[0].ticketId;

        shallowRenderBilling();

        expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
        expect(mockLogVasEvent).toHaveBeenNthCalledWith(1, {
            originalPrice: 777,
            price: 777,
            serviceId: 'offers-history-reports-10',
            errorCode: undefined,
            event: 'VAS_SHOW',
        });
    });

    it('нарисует плашку если у пользователя нет подписок и оплачивается сервис "отчёт по вину" и передаст в нее инфо о пакетах', () => {
        initialState.billing.paymentInfo[0].detailed_product_infos[0].service = 'offers-history-reports';
        initialState.billing.reportsSubscription = undefined;

        const page = shallowRenderBilling();

        expect(shallowToJson(page.dive().find('BillingVinReportSelector'))).toMatchSnapshot();
    });

    describe('не нарисует плашку', () => {
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
            // сбрасываем счетчики от componentDidMount
            mockLogVasEvent.mockClear();
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

        it('отправит метрику клика если это пакет', () => {
            expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
            expect(mockLogVasEvent.mock.calls[0]).toMatchSnapshot();
        });

        it('отправит метрику клика если это не пакет', () => {
            const vinReportSelector = page.dive().find('BillingVinReportSelector');
            vinReportSelector.simulate('tabChange', '1');

            expect(mockLogVasEvent).toHaveBeenCalledTimes(2);
            expect(mockLogVasEvent.mock.calls[1]).toMatchSnapshot();
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
                expect(page.dive().find('.Billing__contentLoader')).toHaveLength(1);
                expect(page.dive().find('BillingPaymentMethods')).toHaveLength(0);
                expect(page.dive().find('.Billing__frameContainer')).toHaveLength(0);
            })
            .then(() => {
                expect(page.dive().find('BillingPaymentMethods')).toHaveLength(1);
                expect(page.dive().find('.Billing__frameContainer')).toHaveLength(1);
            });
    });
});

it('правильный заголовок если оплачивается 2 и более сервиса', () => {
    initialState.billing.paymentInfo[0].detailed_product_infos.push({ name: 'Поднятие в поиске', service: 'all_sale_fresh' });
    const page = shallowRenderBilling();
    expect(page.prop('title')).toBe('Наборный пакет');
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

    const tiedCardsFrame = page.dive().find(BillingFrameTiedCards);
    tiedCardsFrame.simulate('payButtonClick', cards.api_v3);

    return page;
}

function simulateWalletPay() {
    initialState.billing.paymentInfo[0].account_balance = 20000;
    putPaymentMethodToFirstPosition(WALLET_METHOD);

    const page = shallowRenderBilling();
    const walletFrame = page.dive().find(BillingFrameWallet);
    walletFrame.simulate('payButtonClick');

    return page;
}
