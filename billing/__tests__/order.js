import { getTransferTargetItems } from '../order';

describe('data processor for order page', () => {
    it('getTransferTargetItems', () => {
        const respData = [
            {
                clientId: 109497694,
                clientName: 'balance_test 2019-09-23 15:40:03.778074',
                lastTouchDt: '2019-09-25',
                orderClientId: 109497694,
                orderDt: '2019-09-23',
                orderEid: '7-49050676',
                orderId: 1526991252,
                price: '0.000000',
                priceWoNds: '0.000000',
                productName: 'Рекламная кампания РРС',
                serviceCc: 'PPC',
                serviceId: 7,
                serviceOrderId: 49050676,
                text: 'Py_Test order 7-1475',
                type_rate: 1,
                unit: null
            },
            {
                clientId: 109497694,
                clientName: 'balance_test 2019-09-23 15:40:03.778074',
                lastTouchDt: '2019-09-25',
                orderClientId: 109497694,
                orderDt: '2019-09-23',
                orderEid: '7-49050678',
                orderId: 1526991254,
                price: '0.000000',
                priceWoNds: '0.000000',
                productName: 'Рекламная кампания РРС',
                serviceCc: 'PPC',
                serviceId: 7,
                serviceOrderId: 49050678,
                text: 'Py_Test order 7-1475',
                typeRate: 1,
                unit: null
            }
        ];

        expect(getTransferTargetItems(respData)).toEqual([
            {
                value: '7-49050676',
                content:
                    '7-49050676 (2019-09-25): Py_Test order 7-1475, Клиент: 109497694-balance_test 2019-09-23 15:40:03.778074'
            },
            {
                value: '7-49050678',
                content:
                    '7-49050678 (2019-09-25): Py_Test order 7-1475, Клиент: 109497694-balance_test 2019-09-23 15:40:03.778074'
            }
        ]);

        expect(getTransferTargetItems(respData, 1526991254)).toEqual([
            {
                value: '7-49050676',
                content:
                    '7-49050676 (2019-09-25): Py_Test order 7-1475, Клиент: 109497694-balance_test 2019-09-23 15:40:03.778074'
            }
        ]);
    });
});
