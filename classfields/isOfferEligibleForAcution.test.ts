import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import { isOfferEligibleForAcution } from './isOfferEligibleForAcution';

const OFFER_CAN_APPLY_TAG = 'available_for_c2b_auction';

describe('isOfferEligibleForAcution: Подходит ли оффер для Аукциона (Выкупа)', () => {
    it('Оффер подходит для аукциона, если есть соответствующий тэг и диапазон цен оценки', () => {
        const offer = {
            tags: [ OFFER_CAN_APPLY_TAG ],
            state: {
                c2b_auction_info: {
                    price_range: {
                        from: '1000',
                        to: '2000',
                    },
                },
            },
        } as Offer;

        expect(isOfferEligibleForAcution(offer)).toBe(true);
    });

    it('Оффер не подходит под аукцион, если нет тэга', () => {
        const offer = {
            tags: [],
            state: {
                c2b_auction_info: {
                    price_range: {
                        from: '1000',
                        to: '2000',
                    },
                },
            },
        } as unknown as Offer;

        expect(isOfferEligibleForAcution(offer)).toBe(false);
    });

    it('Возвращает false, если оффера вообще нет', () => {
        expect(isOfferEligibleForAcution()).toBe(false);
    });

    it('Оффер не подходит под аукцион, если нет диапазона цен', () => {
        // оффер пустой
        expect(isOfferEligibleForAcution({ tags: [ OFFER_CAN_APPLY_TAG ] } as Offer)).toBe(false);

        // нет инфы об аукционе
        expect(isOfferEligibleForAcution({ tags: [ OFFER_CAN_APPLY_TAG ], state: {} } as Offer)).toBe(false);

        // нет диапазона цен
        expect(isOfferEligibleForAcution({ tags: [ OFFER_CAN_APPLY_TAG ], state: { c2b_auction_info: {} } } as Offer)).toBe(false);

        // диапазона цен пустой
        expect(isOfferEligibleForAcution({ tags: [ OFFER_CAN_APPLY_TAG ], state: { c2b_auction_info: { price_range: {} } } } as Offer)).toBe(false);
    });

    it('Оффер не подходит, если нет ни диапазона цен, ни тэга', () => {
        // оффер пустой
        expect(isOfferEligibleForAcution({ } as Offer)).toBe(false);

        // нет инфы об аукционе
        expect(isOfferEligibleForAcution({ state: {} } as Offer)).toBe(false);

        // нет диапазона цен
        expect(isOfferEligibleForAcution({ state: { c2b_auction_info: {} } } as Offer)).toBe(false);

        // диапазона цен пустой
        expect(isOfferEligibleForAcution({ state: { c2b_auction_info: { price_range: {} } } } as Offer)).toBe(false);
    });
});
