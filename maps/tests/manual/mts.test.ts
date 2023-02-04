import {strict as assert} from 'assert';
import {askQuestion} from '../utils/utils';
import {mtsProvider, OrderStatusResponse, RegisterOrderResponse} from './providers/mts';
import {amppProvider} from './providers/ampp';
const PHONE_NUMBER = '79267776655';
const AMOUNT_IN_RUBLES = 50;
const AMOUNT_IN_CENTS = AMOUNT_IN_RUBLES * 100;

describe('mts', () => {
    describe('order', () => {
        let order: RegisterOrderResponse;
        let orderInfo: OrderStatusResponse;
        let documentId: string;

        it('should create an order', async () => {
            order = await mtsProvider.registerOrder(AMOUNT_IN_CENTS);
            const status = await mtsProvider.getOrderStatus(order.orderId);
            assert.equal(status.orderStatus, 0);
        });

        it('should make a payment by credit card', async () => {
            await showCreditCardDialog(order);
            orderInfo = await mtsProvider.getOrderStatus(order.orderId);
            assert.equal(orderInfo.orderStatus, 2);
        });

        it('should generate "УИП" and send it to bank', async () => {
            const result = await mtsProvider.makeExternalTransfer({
                maskedPan: orderInfo.cardAuthInfo.maskedPan,
                amountInCents: orderInfo.paymentAmountInfo.approvedAmount,
                phoneNumber: PHONE_NUMBER,
                rrn: orderInfo.authRefNum
            });
            documentId = result.documentId;
            assert.equal(result.status, 'PROCESSED');
        });

        it('should topup balance in AMPP with the same "УИП"', async () => {
            const result = await amppProvider.topUpBalance(PHONE_NUMBER, AMOUNT_IN_RUBLES.toString(), documentId);
            assert.equal(result.payment.paymentId, documentId);
        });

        xit('should refund part of amount', async () => {
            // let's refund 50% of the whole amount for this test
            const refundResponse = await mtsProvider.refund(order.orderId, AMOUNT_IN_CENTS * 0.5);
            assert.equal(refundResponse.errorCode, '0');
        });
    });

    describe.skip('bindings', () => {
        const CLIENT_ID = 'yandex-test-binding';
        let order: RegisterOrderResponse;

        it('should return empty bindings for the client', async () => {
            const response = await mtsProvider.getBingings(CLIENT_ID);
            assert.equal(response.bindings.length, 0);
        });

        it('should create an order for specific client', async () => {
            order = await mtsProvider.registerOrder(AMOUNT_IN_CENTS, CLIENT_ID);
            const status = await mtsProvider.getOrderStatus(order.orderId);
            assert.equal(status.orderStatus, 0);
        });

        it('should make a payment by credit card', async () => {
            await showCreditCardDialog(order);
            const orderStatusResponseAfterPayment = await mtsProvider.getOrderStatus(order.orderId);
            assert.equal(orderStatusResponseAfterPayment.orderStatus, 2);
        });

        it('should return bindings for the client', async () => {
            const response = await mtsProvider.getBingings(CLIENT_ID);
            assert.equal(response.bindings.length, 1);
        });

        it('should make a payment by binding', async () => {
            const status = await mtsProvider.getOrderStatus(order.orderId);
            const mdOrder = status.attributes.find((attr) => attr.name === 'mdOrder')?.value;
            const bindingId = status.bindingInfo.bindingId;

            if (!mdOrder) {
                throw new Error('mdorder is undefined');
            }

            const response = await mtsProvider.payOrderByBinding(mdOrder, bindingId, status.ip);
            assert.equal(response.errorCode, 0);
        });

        it('should remove a binding', async () => {
            const status = await mtsProvider.getOrderStatus(order.orderId);
            const bindingId = status.bindingInfo.bindingId;
            const response = await mtsProvider.removeBinding(bindingId);
            assert.equal(response.errorCode, '0');

            const responseBinding = await mtsProvider.getBingings(CLIENT_ID);
            assert.equal(responseBinding.bindings.length, 0);
        });
    });
});

async function showCreditCardDialog(order: RegisterOrderResponse) {
    await askQuestion(`
        Please do the following steps to continue test:
        1. Open url ${order.formUrl}
        2. Enter the following info:
            Cardholder: Podrick Payne
            PAN:\t4111111111111111
            CVC:\t123
            EXP:\t2024/12

            ACS:\t12345678
        3. Press any key after redirect to localhost`
    );
}
