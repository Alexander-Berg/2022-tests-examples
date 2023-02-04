import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import getFormattedPrice from './getFormattedPrice';

let offer: Offer;
beforeEach(() => {
    offer = {
        price_info: {
            RUR: 4800000,
            USD: 76190,
        },
    } as Partial<Offer> as Offer;
});

it('должен вернуть отформатированную цену оффера с указанием валюты (рубли)', () => {
    const divider = /\u00a0/g;
    expect(
        getFormattedPrice(offer, Currency.RUR).replace(divider, ' '),
    ).toEqual('4 800 000 ₽');
});

it('должен вернуть отформатированную цену оффера с указанием валюты (не рубли)', () => {
    const divider = /\u00a0/g;
    expect(
        getFormattedPrice(offer, Currency.USD).replace(divider, ' '),
    ).toEqual('76 190 $');
});

it('должен вернуть пустую строку, если нет цены в данной валюте', () => {
    expect(
        getFormattedPrice(offer, Currency.EUR),
    ).toEqual('');
});
