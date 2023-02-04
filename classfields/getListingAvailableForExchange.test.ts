import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import type { StateGarage } from 'auto-core/react/dataDomain/garage/types';

import getListingAvailableForExchange from './getListingAvailableForExchange';

const TRADE_IN_PRICE = { from: 10, to: 20 };
const MARKET_PRICE = { price: 100 };

const CARD_CURRENT = {
    card_type_info: {
        card_type: CardTypeInfo_CardType.CURRENT_CAR,
    },
    price_predict: {
        predict: {
            market: MARKET_PRICE,
            tradein_dealer_matrix_buyout: TRADE_IN_PRICE,
        },
    },
};

const CARD_CURRENT_NO_PRICE_PREDICT = {
    card_type_info: {
        card_type: CardTypeInfo_CardType.CURRENT_CAR,
    },
};

const CARD_CURRENT_INVALID_TRADEIN = {
    card_type_info: {
        card_type: CardTypeInfo_CardType.CURRENT_CAR,
    },
    price_predict: {
        predict: {
            market: MARKET_PRICE,
            tradein_dealer_matrix_buyout: { from: 10 },
        },
    },
};

const CARD_DREAM = {
    card_type_info: {
        card_type: CardTypeInfo_CardType.DREAM_CAR,
    },
    price_predict: {
        predict: {
            market: MARKET_PRICE,
            tradein_dealer_matrix_buyout: TRADE_IN_PRICE,
        },
    },
};

// Написал тест, чтобы закрепить знание о том, какие карточки,
// должны вернуться из селектора
it('вернет только карточки текущих тачек с рыночной price predict', () => {
    const state = {
        garage: {
            data: {
                listing: [
                    CARD_CURRENT,
                    CARD_CURRENT_NO_PRICE_PREDICT,
                    CARD_CURRENT_INVALID_TRADEIN,
                    CARD_DREAM,
                ],
            },
        } as unknown as StateGarage,
    };
    const result = getListingAvailableForExchange(state);
    expect(result).toEqual([ CARD_CURRENT ]);
});
