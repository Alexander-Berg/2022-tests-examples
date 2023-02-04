import { tvm as tvmMock } from 'server/lib/luster/tvm.mock';

import {
    prepareResult,
    prepareSiteOffers,
    prepareOffers,
} from './preparePostBlockRealtyOffers';
import {
    OFFER_MOCK,
    NEWBUILDING_OFFER_MOCK,
    PREPARATION_RESULT_MOCK_OFFER_MOCK,
    PREPARATION_RESULT_MOCK_NEWBUILDING_MOCK,
} from './preparePostBlockRealtyOffers.mock';

jest.mock('server/lib/luster/tvm', () => ({
    tvm: tvmMock,
}));

it('не возвращает офферы в результате, если итоговое количество офферов меньше двух офферов', () => {
    const initialResult = prepareResult(PREPARATION_RESULT_MOCK_OFFER_MOCK);
    const result = initialResult({
        result: {
            offers: {
                items: [ OFFER_MOCK ],
            },
        },
    });

    expect(result.realtyOffers.offers).toBe(undefined);
});

it('подготавливает блок для офферов', () => {
    const initialResult = prepareResult(PREPARATION_RESULT_MOCK_OFFER_MOCK);
    const result = initialResult({
        result: {
            offers: {
                items: [ OFFER_MOCK, OFFER_MOCK ],
            },
        },
    });

    expect(result.type).toEqual('realtyOffers');
    expect(result.realtyOffers.listingUrl).toEqual('/moskva/snyat/kvartira/yandex-arenda/?isFastlink=true');
    expect(result.realtyOffers.filterUrl).toEqual('/snyat/kvartira/?yandexRent=YES&isFastlink=true');
    expect(result.realtyOffers.offerType).toEqual('offer');
    expect(result.realtyOffers.offers?.length).toEqual(2);
    expect(result.realtyOffers.title).toBe('Снять квартиру');
    expect(result.realtyOffers.btnTitle).toBe('Смотреть больше объявлений');
});

it('подготавливает блок для офферов новостроек', () => {
    const initialResult = prepareResult(PREPARATION_RESULT_MOCK_NEWBUILDING_MOCK);

    const result = initialResult({
        result: {
            sites: {
                items: [ NEWBUILDING_OFFER_MOCK, NEWBUILDING_OFFER_MOCK ],
            },
        },
    });

    expect(result.type).toEqual('realtyOffers');
    expect(result.realtyOffers.listingUrl).toEqual('/moskva/kupit/novostrojka/tryohkomnatnaya/');
    expect(result.realtyOffers.filterUrl).toEqual('/kupit/novostrojka/tryohkomnatnaya/');
    expect(result.realtyOffers.offerType).toEqual('newbuilding');
    expect(result.realtyOffers.offers?.length).toEqual(2);
    expect(result.realtyOffers.title).toBe(undefined);
    expect(result.realtyOffers.btnTitle).toBe(undefined);
});

it('правильно подготавливает офферы', () => {
    const result = prepareOffers([ OFFER_MOCK ]);

    expect(result).toEqual([
        {
            imageUrl: '/images/url/1',
            location: {
                address: 'улица Народного Ополчения, 39к1',
                metro: {
                    latitude: 55.793587,
                    lineColors: [ 'b1179a' ],
                    longitude: 37.493324,
                    metroGeoId: 20368,
                    metroTransport: 'ON_FOOT',
                    minTimeToMetro: 4,
                    name: 'Октябрьское Поле',
                    rgbColor: 'b1179a',
                    timeToMetro: 7,
                },
                streetAddress: 'улица Народного Ополчения, 39к1',
            },
            price: {
                currency: 'RUR',
                period: 'PER_MONTH',
                unit: 'WHOLE_OFFER',
                value: 60000,
            },
            title: '?-комнатная',
            url: '/offer/423423/',
        },
    ]);
});


it('правильно подготавливает офферы новостроек', () => {
    const result = prepareSiteOffers([ NEWBUILDING_OFFER_MOCK ]);

    expect(result).toEqual([
        {
            imageUrl: '/images/url/1',
            location: {
                rgid: 274798,
                populatedRgid: 596066,
            },
            price: {
                currency: 'RUR',
                from: 3789674,
                to: 11609639,
            },
            title: 'ЖК «Люберцы»',
            url: '/lyubertsy/kupit/novostrojka/lyubercy-423423/',
        },
    ]);
});
