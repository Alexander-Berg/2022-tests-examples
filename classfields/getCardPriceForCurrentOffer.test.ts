import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/search/search_model';
import type { PriceRange } from '@vertis/schema-registry/ts-types-snake/auto/api/stats_model';
import type { PricePredict } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import { SellerType } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import garageCardMock from 'auto-core/react/dataDomain/garageCard/mocks';

import getCardPriceForCurrentOffer from './getCardPriceForCurrentOffer';

const PRICE = 123000;

describe('есть цены', () => {
    it('должен вернуть рыночную цену для оффера от частника', () => {
        const predict = {
            predict: {
                market: {
                    price: PRICE,
                    currency: Currency.RUR,
                },
            },
        } as unknown as PricePredict;

        const card = garageCardMock.withPricePredict(predict).value();
        const offer = { ...cardMock, seller_type: SellerType.PRIVATE };
        const price = getCardPriceForCurrentOffer(card, offer);
        expect(price).toBe(PRICE);
    });

    it('должен вернуть трейдин цену для оффера от дилера', () => {
        const predict = {
            predict: {
                tradein_dealer_matrix_buyout: {
                    from: PRICE - 100,
                    to: PRICE + 100,
                    currency: Currency.RUR,
                } as unknown as PriceRange,
            },
        } as unknown as PricePredict;

        const card = garageCardMock.withPricePredict(predict).value();
        const offer = { ...cardMock, seller_type: SellerType.COMMERCIAL };
        const price = getCardPriceForCurrentOffer(card, offer);
        expect(price).toBe(PRICE);
    });

    it('не должен вернуть трейдин цену для оффера от дилера если нет верхней цены', () => {
        const predict = {
            predict: {
                tradein_dealer_matrix_buyout: {
                    from: PRICE - 100,
                    currency: Currency.RUR,
                } as unknown as PriceRange,
            },
        } as unknown as PricePredict;

        const offer = { ...cardMock, seller_type: SellerType.COMMERCIAL };
        const card = garageCardMock.withPricePredict(predict).value();
        const price = getCardPriceForCurrentOffer(card, offer);
        expect(price).toBe(undefined);
    });

    it('не должен вернуть трейдин цену для оффера от дилера если нет нижней цены', () => {
        const predict = {
            predict: {
                tradein_dealer_matrix_buyout: {
                    to: PRICE - 100,
                    currency: Currency.RUR,
                } as unknown as PriceRange,
            },
        } as unknown as PricePredict;

        const offer = { ...cardMock, seller_type: SellerType.COMMERCIAL };
        const card = garageCardMock.withPricePredict(predict).value();
        const price = getCardPriceForCurrentOffer(card, offer);
        expect(price).toBe(undefined);
    });
});

describe('нет цены', () => {
    it('не должен вернуть рыночную цену для оффера от частника', () => {
        const offer = { ...cardMock, seller_type: SellerType.PRIVATE };
        const card = garageCardMock.withPricePredict({} as unknown as PricePredict).value();
        const price = getCardPriceForCurrentOffer(card, offer);
        expect(price).toBe(undefined);
    });

    it('не должен вернуть трейдин цену для оффера от дилера', () => {
        const offer = { ...cardMock, seller_type: SellerType.COMMERCIAL };
        const card = garageCardMock.withPricePredict({} as unknown as PricePredict).value();
        const price = getCardPriceForCurrentOffer(card, offer);
        expect(price).toBe(undefined);
    });
});
