import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import initPaymentMock from 'auto-core/server/resources/publicApiBilling/methods/initPayment.mock';
import createContext from 'auto-core/server/descript/createContext';

import type { ArrayElement } from 'auto-core/types/ArrayElement';
import type { THttpRequest, THttpResponse } from 'auto-core/http';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { PaymentMethodIds, TWalletRefillService } from 'auto-core/types/TBilling';
import type { TPaymentMethodYouMoney, TPaymentInfo } from 'auto-core/types/TBilling';

import initTransactionPreparer from './initTransactionPreparer';

let req: THttpRequest ;
let res: THttpResponse;
let context: ReturnType<typeof createContext>;
const product = encodeURIComponent(JSON.stringify([ { name: TOfferVas.FRESH, count: 10 } ]));

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('при пополнение кошелька', () => {
    let result: TPaymentInfo;

    beforeEach(() => {
        const product = encodeURIComponent(JSON.stringify([ { name: TWalletRefillService.WALLET_USER, count: 10 } ]));
        const response = {
            initPayment: initPaymentMock.withAccountBalance('100000').value(),
        };
        result = initTransactionPreparer({
            context,
            params: { product },
            result: response,
        });
    });

    it('добавить информацию об оплачиваемом сервисе', () => {
        const serviceInfo = result.detailed_product_infos[0];
        expect(serviceInfo).toEqual({ effective_price: 10, name: 'Пополнение кошелька', service: TWalletRefillService.WALLET_USER });
    });

    it('не добавить кошелек в способы оплаты', () => {
        const walletPaymentMethod = (result.payment_methods as Array<TPaymentMethodYouMoney>).find(({ id }) => id === PaymentMethodIds.WALLET);
        expect(walletPaymentMethod).toBeUndefined();
    });

    it('добавить стоимость в ответ', () => {
        const cost = result.cost;
        expect(cost).toBe(10);
    });
});

describe('при привязке карты', () => {
    let result: TPaymentInfo;

    beforeAll(() => {
        const product = encodeURIComponent(JSON.stringify([ { name: TWalletRefillService.BIND_CARD, count: 1 } ]));
        const response = {
            initPayment: initPaymentMock.withAccountBalance('100000').value(),
        };
        result = initTransactionPreparer({
            context,
            params: { product },
            result: response,
        });
    });

    it('добавить информацию об оплачиваемом сервисе', () => {
        const serviceInfo = result.detailed_product_infos[0];
        expect(serviceInfo).toEqual({ effective_price: 1, name: 'Привязка карты', service: TWalletRefillService.BIND_CARD });
    });

    it('единственным способом оплаты будет банковская карта без привязанных карт', () => {
        const paymentMethods = result.payment_methods;
        expect(paymentMethods).toHaveLength(1);
        expect((paymentMethods[0] as TPaymentMethodYouMoney).id).toBe(PaymentMethodIds.BANK_CARD);
    });

    it('добавить стоимость в ответ', () => {
        const cost = result.cost;
        expect(cost).toBe(1);
    });
});

describe('приводит суммы к рублям', () => {
    let result: TPaymentInfo;
    const propNames: Array<keyof Pick<TPaymentInfo, 'cost' | 'account_balance' | 'base_cost'>> = [ 'cost', 'account_balance', 'base_cost' ];
    const paymentInfo = initPaymentMock.value();

    beforeEach(() => {
        const response = {
            initPayment: paymentInfo,
        };
        result = initTransactionPreparer({
            context,
            params: { product },
            result: response,
        });
    });

    propNames.forEach(propName => {
        it(`для свойства "${ propName }"`, () => {
            expect(result[propName] * 100).toBe(Number(paymentInfo[propName]));
        });
    });

    const propNamesProduct: Array<keyof Pick<ArrayElement<TPaymentInfo['detailed_product_infos']>, 'base_price' | 'effective_price' | 'auto_apply_price'>> =
        [ 'base_price', 'effective_price', 'auto_apply_price' ];

    propNamesProduct.forEach(prop => {
        it(`для свойства "${ prop }" в массиве с информацией об оплачиваемых сервисах`, () => {
            const basePriceList = result.detailed_product_infos.map(item => item[prop]);
            const responseBasePriceListMod = paymentInfo.detailed_product_infos.map(item => Number(item[prop]) / 100);

            expect(basePriceList).toEqual(responseBasePriceListMod);
        });
    });
});
