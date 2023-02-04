import initPaymentMock, { TrustPaymentMethods } from 'auto-core/server/resources/publicApiBilling/methods/initPayment.mock';

import { TWalletRefillService } from 'auto-core/types/TBilling';

import getPaymentMethodsTrust from './getPaymentMethodsTrust';
import getCustomProduct from './getCustomProduct';

it('для привязки карты вернет пустой массив', () => {
    const response = {
        initPayment: initPaymentMock.withPaymentMethods([
            TrustPaymentMethods.tied_card_1,
            TrustPaymentMethods.new_card,
        ]).value(),
    };
    const result = getPaymentMethodsTrust(response, getCustomProduct([ { name: TWalletRefillService.BIND_CARD, count: 10 } ]));

    expect(result).toEqual([]);
});

it('правильно форматирует данные карт', () => {
    const response = {
        initPayment: initPaymentMock
            .withPaymentMethods([
                TrustPaymentMethods.tied_card_1,
                TrustPaymentMethods.tied_card_2,
            ])
            .value(),
    };
    const result = getPaymentMethodsTrust(response);

    expect(result).toMatchSnapshot();
});

describe('оставит в методах только привязанные карты', () => {
    it('при пополнении кошелька', () => {
        const response = {
            initPayment: initPaymentMock.withPaymentMethods([
                TrustPaymentMethods.tied_card_1,
                TrustPaymentMethods.tied_card_2,
                TrustPaymentMethods.new_card,
            ]).value(),
        };
        const result = getPaymentMethodsTrust(response, getCustomProduct([ { name: TWalletRefillService.WALLET_USER, count: 10 } ]));

        expect(result).toHaveLength(2);
        expect('number' in result[0] && result[0].number).toBe('510000|5755');
        expect('number' in result[1] && result[1].number).toBe('510000|1554');
    });

    it('если сумма на кошельке меньше суммы платежа', () => {
        const response = {
            initPayment: initPaymentMock
                .withPaymentMethods([
                    TrustPaymentMethods.tied_card_1,
                    TrustPaymentMethods.tied_card_2,
                    TrustPaymentMethods.new_card,
                ])
                .withAccountBalance('10000')
                .withCost('10100')
                .value(),
        };
        const result = getPaymentMethodsTrust(response);

        expect(result).toHaveLength(2);
        expect('number' in result[0] && result[0].number).toBe('510000|5755');
        expect('number' in result[1] && result[1].number).toBe('510000|1554');
    });
});

it('добавит кошелек в способы оплаты, если суммы на балансе достаточно', () => {
    const response = {
        initPayment: initPaymentMock
            .withPaymentMethods([
                TrustPaymentMethods.tied_card_1,
                TrustPaymentMethods.tied_card_2,
                TrustPaymentMethods.new_card,
            ])
            .withAccountBalance('20000')
            .withCost('10000')
            .value(),
    };
    const result = getPaymentMethodsTrust(response);

    expect(result[0]).toMatchSnapshot();
});
