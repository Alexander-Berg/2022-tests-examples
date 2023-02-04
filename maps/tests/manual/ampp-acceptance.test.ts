import {generateCsv, MosReportRow} from '../../app/integration/mos-report';
import {askQuestion} from '../utils/utils';
import {amppProvider} from './providers/ampp';
import {mtsProvider, RegisterOrderResponse} from './providers/mts';

async function topUpBalanceWithUIP(phone: string, amount: number): Promise<MosReportRow> {
    const order = await mtsProvider.registerOrder(amount * 100);
    await showCreditCardDialog(order);
    const orderInfo = await mtsProvider.getOrderStatus(order.orderId);
    const transferResult = await mtsProvider.makeExternalTransfer({
        maskedPan: orderInfo.cardAuthInfo.maskedPan,
        amountInCents: orderInfo.paymentAmountInfo.approvedAmount,
        phoneNumber: phone,
        rrn: orderInfo.authRefNum
    });
    const result = await amppProvider.topUpBalance(phone, amount.toString(), transferResult.documentId);

    return {
        date: result.payment.date.split(' ')[0],
        time: result.payment.date.split(' ')[1],
        paymentId: transferResult.documentId,
        transactionId: transferResult.requestId,
        amount: result.payment.amount,
        uip: transferResult.documentId,
        phone: phone,
        paymentType: 'пополнение парковочного счета'
    };
}

describe('ampp-acceptance', () => {
    const csvData: MosReportRow[] = [];

    it('should run flow', async () => {
        const subscribers = [
            {phone: '79267776655', car_number: 'ABC111', parking_id: '4020', amount: 100, duration: '60'},
            {phone: '79267776655', car_number: 'ABC111', parking_id: '4020', amount: 200, duration: '60'},
            {phone: '79267776655', car_number: 'ABC111', parking_id: '4020', amount: 300, duration: '60'},
            {phone: '79853960743', car_number: 'ABC222', parking_id: '4020', amount: 400, duration: '30'},
            {phone: '79853960743', car_number: 'ABC222', parking_id: '4020', amount: 500, duration: '60'}
        ];

        for await (const s of subscribers) {
            // tslint:disable-next-line:no-console
            console.log(`\n\n====== Testcase ${JSON.stringify(s)} =====`);
            await amppProvider.getAccountBalance(s.phone);
            csvData.push(await topUpBalanceWithUIP(s.phone, s.amount));
            await amppProvider.startParkingSession(s.phone, s.parking_id, s.car_number, s.duration);
            await amppProvider.extendParkingSession(s.phone, s.car_number, s.duration);
            await amppProvider.stopParkingSession(s.phone, s.car_number);
        }
    });

    it('should generate report', () => {
        // tslint:disable-next-line:no-console
        console.log(generateCsv(csvData));
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
        3. Press any key after redirect to localhost\n`
    );
}
