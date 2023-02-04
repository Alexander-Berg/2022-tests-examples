import type { Seller, Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import getSellerLocationOrRegionCoords from './getSellerLocationOrRegionCoords';

let offer: Partial<Offer>;
beforeEach(() => {
    offer = {
        seller: {
            name: 'name',
            location: {
                coord: {
                    latitude: 55.789234,
                    longitude: 37.255173,
                },
                region_info: {
                    id: '213',
                    name: 'Москва',
                    latitude: 55.753215,
                    longitude: 37.622504,
                },
            },
        } as Partial<Seller> as Seller,
    };
});

it('должен вернуть координаты места осмотра, если они есть', () => {
    expect(getSellerLocationOrRegionCoords(offer as Offer)).toEqual({
        latitude: 55.789234,
        longitude: 37.255173,
    });
});

it('должен вернуть координаты региона, если они нет места осмотра', () => {
    // Привет TS :)
    if (offer.seller) {
        delete offer.seller.location?.coord;
    }
    expect(getSellerLocationOrRegionCoords(offer as Offer)).toEqual({
        latitude: 55.753215,
        longitude: 37.622504,
    });
});

it('должен вернуть undefined, если ничего нет (битое объявление)', () => {
    delete offer.seller;
    expect(getSellerLocationOrRegionCoords(offer as Offer)).toBeUndefined();
});
