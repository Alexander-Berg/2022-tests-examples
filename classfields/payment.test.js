/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const Payment = require('./payment');
const { PAYMENT_METHOD_IDS, PAYMENT_STATUSES } = require('./utils');
const TICKET_ID = 'my-awesome-payment';
const PAYMENT_URL = 'http://example.com';
const TOKEN = 'super-duper-secret-key';
const getResource = require('auto-core/react/lib/gateApi').getResource;

const paymentMethods = {
    new_card: { ps_id: 'YANDEXKASSA_V3', id: PAYMENT_METHOD_IDS.bank_card, paymentToken: TOKEN, save: true },
    tied_card_v1: { ps_id: 'YANDEXKASSA', id: PAYMENT_METHOD_IDS.bank_card, mask: '444444|4448' },
    tied_card_v3: { ps_id: 'YANDEXKASSA_V3', id: PAYMENT_METHOD_IDS.bank_card, mask: '555555|4444' },
    tied_card_with_cvc: {
        ps_id: 'YANDEXKASSA_V3', id: PAYMENT_METHOD_IDS.bank_card, mask: '555555|4444',
        verification_required: true, cvcToken: TOKEN,
    },
    wallet_pay: { id: 'wallet' },
    yandex_money: { ps_id: 'YANDEXKASSA_V3', id: PAYMENT_METHOD_IDS.yandex_money },
    sber: { ps_id: 'YANDEXKASSA_V3', id: PAYMENT_METHOD_IDS.sberbank, phone: '79771234567' },
};

jest.setTimeout(10000);
const { location } = global;

// сюда записываем параметры с которыми вызывается /payment/process
let paymentProcessParams;
// сюда сохраняем исходное состояние window
let originalWindowOpen;
let originalWindowClose;
beforeEach(() => {
    paymentProcessParams = {};
    originalWindowOpen = global.open;
    originalWindowClose = global.close;
    global.open = jest.fn(() => global);
    global.close = jest.fn(() => global);
    delete global.location;
    global.location = {
        replace: jest.fn(),
    };
});

afterEach(() => {
    global.open = originalWindowOpen;
    global.close = originalWindowClose;
    global.location = location;
});

it('при успешной оплате вернет зарезолвленный промис со статусом платежа', () => {
    const payment = new Payment(TICKET_ID, paymentMethods.tied_card_v3);
    // здесь и далее сначала мокается ответ от /payment/process а затем от /payment/status
    getResource
        .mockImplementationOnce(mockPaymentProcessResponse('success'))
        .mockImplementationOnce(mockPaymentStatusResponse(PAYMENT_STATUSES.closed));

    return payment.processPayment()
        .then((response) => {
            expect(response.payment_status).toBe(PAYMENT_STATUSES.closed);
        });
});

it('при не успешной оплате вернет реджекнутый промис со статусом платежа', async() => {
    const payment = new Payment(TICKET_ID, paymentMethods.tied_card_v3);
    getResource
        .mockImplementationOnce(mockPaymentProcessResponse('success'))
        .mockImplementationOnce(mockPaymentStatusResponse(PAYMENT_STATUSES.failed));

    await expect(payment.processPayment()).rejects.toMatchObject({ payment_status: PAYMENT_STATUSES.failed });
});

describe('правильно формирует параметры запроса для /payment/process', () => {
    beforeEach(() => {
        getResource
            .mockImplementationOnce(mockPaymentProcessResponse('success'))
            .mockImplementationOnce(mockPaymentStatusResponse(PAYMENT_STATUSES.closed));
    });

    const testCases = [
        { name: 'при оплате новой картой', paymentMethod: paymentMethods.new_card, options: { returnUrl: 'https://auto.ru' } },
        { name: 'при оплате привязанной картой из старого апи', paymentMethod: paymentMethods.tied_card_v1 },
        { name: 'при оплате привязанной картой из нового апи', paymentMethod: paymentMethods.tied_card_v3 },
        { name: 'при оплате привязанной картой с цвц-подтверждением', paymentMethod: paymentMethods.tied_card_with_cvc },
        { name: 'при оплате кошельком', paymentMethod: paymentMethods.wallet_pay },
        { name: 'при пополнение кошелька', paymentMethod: paymentMethods.new_card, options: { accountRefillAmount: 10000 } },
        { name: 'при оплате сбербанком на нашей форме', paymentMethod: paymentMethods.sber },
        { name: 'при оплате сбербанком на сайте сбера', paymentMethod: paymentMethods.sber, options: { hasRedirect: true } },
    ];

    testCases.forEach(({ name, options, paymentMethod }) => {
        it(`${ name }`, () => {
            const payment = new Payment(TICKET_ID, paymentMethod, options);
            return payment.processPayment()
                .then(() => {
                    expect(paymentProcessParams).toMatchSnapshot();
                });
        });
    });
});

describe('при оплате картой', () => {
    it('если карта привязана не будет открывать окно для подтверждения платежа', () => {
        const payment = new Payment(TICKET_ID, paymentMethods.tied_card_v1);
        getResource
            .mockImplementationOnce(mockPaymentProcessResponse('success'))
            .mockImplementationOnce(mockPaymentStatusResponse(PAYMENT_STATUSES.closed));
        return payment.processPayment()
            .then(() => {
                expect(global.open).not.toHaveBeenCalled();
            });
    });

    it(`если карта с 3дс завернет платеж с правильной ошибкой`, async() => {

        const payment = new Payment(TICKET_ID, paymentMethods.new_card);
        getResource
            .mockImplementationOnce(mockPaymentProcessResponse('success', { url: PAYMENT_URL }));

        await expect(payment.processPayment()).rejects.toMatchObject({ type: 'need_confirmation', url: PAYMENT_URL });
    });
});

function mockPaymentProcessResponse(type, payload) {
    return function(resource, params) {
        paymentProcessParams = JSON.parse(params.paymentParams);
        switch (type) {
            case 'success':
                return Promise.resolve({ ticket_id: TICKET_ID, ...payload });
        }
    };
}

function mockPaymentStatusResponse(type) {
    return function() {
        return Promise.resolve({ payment_status: type });
    };
}
