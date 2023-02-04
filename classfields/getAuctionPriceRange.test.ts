import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import getAuctionPriceRange from './getAuctionPriceRange';

describe('getAuctionPriceRange: Получение диапазона оценки автомобиля для Аукциона (Выкупа)', () => {
    it('Возвращает диапазон цен', () => {
        const offer = { state: { c2b_auction_info: { price_range: { from: '1000', to: '2000' } } } } as Offer;
        expect(getAuctionPriceRange(offer)).toEqual({ from: 1000, to: 2000 });
    });

    it('Возвращает нулевой диапазон, если в оффере нет оценки', () => {
        // оффера нет
        expect(getAuctionPriceRange()).toEqual({ from: 0, to: 0 });

        // оффер пустой
        expect(getAuctionPriceRange({} as Offer)).toEqual({ from: 0, to: 0 });

        // нет инфы об аукционе
        expect(getAuctionPriceRange({ state: {} } as Offer)).toEqual({ from: 0, to: 0 });

        // нет диапазона цен
        expect(getAuctionPriceRange({ state: { c2b_auction_info: {} } } as Offer)).toEqual({ from: 0, to: 0 });

        // диапазона цен пустой
        expect(getAuctionPriceRange({ state: { c2b_auction_info: { price_range: {} } } } as Offer)).toEqual({ from: 0, to: 0 });
    });
});
