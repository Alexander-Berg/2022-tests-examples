import type { PaymentMethod } from '@vertis/schema-registry/ts-types-snake/vertis/banker/api_model';

import initPaymentMock, { YouMoneyPaymentMethods } from 'auto-core/server/resources/publicApiBilling/methods/initPayment.mock';

import { PaymentMethodIds } from 'auto-core/types/TBilling';
import type { TTiedCard, TPaymentMethodYouMoney } from 'auto-core/types/TBilling';

import getPaymentMethodsYouMoney, { PAYMENT_METHOD_WHITE_LIST } from './getPaymentMethodsYouMoney';

describe('на мобиле', () => {
    it('если есть привязанные карты, поставит новую карту в конец и переименует ее', () => {
        const response = {
            initPayment: initPaymentMock.withPaymentMethods([
                YouMoneyPaymentMethods.new_card,
                YouMoneyPaymentMethods.new_api_tied_card_1,
                YouMoneyPaymentMethods.new_api_tied_card_2,
            ]).value(),
        };
        const result = getPaymentMethodsYouMoney(response, undefined, true);

        expect(result).toHaveLength(3);
        expect(result[2]).toEqual({
            id: 'bank_card',
            name: 'Новая карта',
            ps_id: 'YANDEXKASSA_V3',
        });
    });
});

describe('на десктопе', () => {
    it('оставляет только указанные методы оплаты', () => {
        const response = {
            initPayment: initPaymentMock.withPaymentMethods([
                YouMoneyPaymentMethods.new_card,
                YouMoneyPaymentMethods.new_api_tied_card_1,
                YouMoneyPaymentMethods.new_api_tied_card_2,
            ]).value(),
        };
        const result = getPaymentMethodsYouMoney(response, undefined, false);
        const methods = (result as Array<TPaymentMethodYouMoney>).map(({ id }) => id);

        expect(PAYMENT_METHOD_WHITE_LIST).toEqual(expect.arrayContaining(methods));
    });

    describe('если есть привязанные карты', () => {
        const tiedCards = [
            YouMoneyPaymentMethods.old_api_tied_card,
            YouMoneyPaymentMethods.new_api_tied_card_1,
            YouMoneyPaymentMethods.new_api_tied_card_2,
        ];
        let result: Array<TTiedCard>;

        beforeEach(() => {
            const response = {
                initPayment: initPaymentMock.withPaymentMethods([
                    YouMoneyPaymentMethods.old_api_tied_card,
                    YouMoneyPaymentMethods.new_api_tied_card_1,
                    YouMoneyPaymentMethods.new_api_tied_card_2,
                ]).value(),
            };
            result = getPaymentMethodsYouMoney(response, undefined, false) as Array<TTiedCard>;
        });

        it('оставит только один метод оплаты картой', () => {
            expect(result.filter(isBankCardMethod)).toHaveLength(4);
        });

        it('правильно сформирует список карт', () => {
            const expectedMasks = tiedCards.map(formatTiedCardInfo).map(getCardMaskOrId);
            const receivedMasks = result.map(getCardMaskOrId);
            expect(receivedMasks).toEqual(expect.arrayContaining(expectedMasks));
        });

        it('поставит основную карту на первое место', () => {
            expect(result[1].preferred).toBe(true);
        });
    });

    it('если нет привязанных карт то, в массиве будет только "новая карта"', () => {
        const response = {
            initPayment: initPaymentMock.withPaymentMethods([]).value(),
        };
        const result = getPaymentMethodsYouMoney(response, undefined, false);
        const firstMethod = result[0] as TPaymentMethodYouMoney;

        expect(result).toHaveLength(1);
        expect(firstMethod.id).toEqual(PaymentMethodIds.BANK_CARD);
        expect('mask' in firstMethod).toBe(false);
    });

    it('если достаточно баланса добавит "кошелек" в методы оплаты', () => {
        const response = {
            initPayment: initPaymentMock.withAccountBalance('100000').withPaymentMethods([]).value(),
        };
        const result = getPaymentMethodsYouMoney(response, undefined, false);

        expect(result[0].id).toBe(PaymentMethodIds.WALLET);
    });

    it('отсортирует способы оплаты в заданном порядке', () => {
        const response = {
            initPayment: initPaymentMock.value(),
        };
        const result = getPaymentMethodsYouMoney(response, undefined, false).map(({ id }) => id);
        expect(result).toEqual(PAYMENT_METHOD_WHITE_LIST.filter(id => result.includes(id)));
    });

    it('при оплате бесплатного сервиса единственным методом будет "кошелек"', () => {
        const response = {
            initPayment: initPaymentMock.withCost('0').value(),
        };
        const result = getPaymentMethodsYouMoney(response, undefined, false);

        expect(result).toHaveLength(1);
        expect(result[0].id).toEqual(PaymentMethodIds.WALLET);
    });

    it('исключит методы у которых порог ниже чем сумма транзакции', () => {
        const response = {
            initPayment: initPaymentMock.withCost('1200000').value(),
        };
        const result = getPaymentMethodsYouMoney(response, undefined, false);

        expect(result).not.toEqual(expect.arrayContaining([ 'sberbank' ]));
    });
});

function formatTiedCardInfo(card: PaymentMethod) {
    return {
        id: 'bank_card',
        ps_id: card.ps_id,
        mask: card.properties?.card?.cdd_pan_mask,
        brand: card.properties?.card?.brand,
        verification_required: card.properties?.card?.verification_required,
        preferred: card.preferred,
    };
}

function isBankCardMethod({ id }: { id: string }) {
    return id === 'bank_card';
}

function getCardMaskOrId({ id, mask }: { id: string; mask: string | undefined }) {
    return mask || id;
}
