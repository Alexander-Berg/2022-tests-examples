/* eslint-disable max-len */
import type { SaleAttributes } from 'auto-core/lib/ads/AD2.types';

import getAsciiParamsFromSaleAttributes from './getAsciiParamsFromSaleAttributes';

it.each([
    [
        { asciiCat: 'cars', category: 'cars', 'engine-type': 'GASOLINE', image: 'https://images.mds-proxy.test.avto.ru/get-autoru-vos/4194268/6c37a9ad40878865476ca1ffc06008f3/1200x900', 'km-age': 118001, mark: 'NISSAN', markName: 'Nissan', model: 'MURANO', modelName: 'Murano', power: 249, price: 1279000, puid2: '3176', routeCategory: 'cars', segment: 'MEDIUM', state: 'used', transmission: 'VARIATOR', type: 'suv', year: 2013 },
        [ 'mark', 'puid2', 'type', 'segment' ],
        { ascii: 'NISSAN|3176|suv|MEDIUM', mark: 'NISSAN', puid2: '3176', segment: 'MEDIUM', type: 'suv' },
    ],
    [
        { asciiCat: 'cars', category: 'cars', 'engine-type': 'GASOLINE', image: 'https://images.mds-proxy.test.avto.ru/get-autoru-vos/4194268/6c37a9ad40878865476ca1ffc06008f3/1200x900', 'km-age': 118001, mark: 'NISSAN', markName: 'Nissan', model: 'MURANO', modelName: 'Murano', power: 249, price: 1279000, puid2: '3176', routeCategory: 'cars', segment: 'MEDIUM', state: 'used', transmission: 'VARIATOR', type: 'suv', year: 2013 },
        [ 'mark', 'puid2' ],
        { ascii: 'NISSAN|3176', mark: 'NISSAN', puid2: '3176' },
    ],
    [
        { asciiCat: 'trucks', category: 'trucks', 'engine-type': 'DIESEL', image: 'https://images.mds-proxy.test.avto.ru/get-autoru-vos/65714/9596e1faa7abd533fda09e8b6d142d1f/1200x900', 'km-age': 245000, mark: 'MAZ', markName: 'МАЗ', model: '6312', modelName: '6312', power: 412, price: 1890000, puid2: '11203640', routeCategory: 'truck', state: 'used', transmission: 'MECHANICAL', type: '', year: 2013 },
        [ 'mark', 'puid', 'type', 'state' ],
        { ascii: 'MAZ|USED', mark: 'MAZ', state: 'USED' },
    ],
])('%j %j -> %j', (saleAttributes: SaleAttributes, keys: Array<string>, result: Record<string, string>) => {
    expect(getAsciiParamsFromSaleAttributes(saleAttributes, keys)).toEqual(result);
});
