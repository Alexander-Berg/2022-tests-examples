const getDesktopUrl = require('./getDesktopUrl');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

let req;
beforeEach(() => {
    req = createHttpReq();
});

it('должен сохранить параметры из урла', () => {
    req.urlWithoutRegion = '/cars/all/?from=yandex';
    req.router.params = {
        from: 'yandex',
    };

    expect(
        getDesktopUrl(req),
    ).toEqual('https://autoru_frontend.base_domain/cars/all/?from=yandex');
});

it('должен сохранить параметры из урла и добавить параметры из optParams', () => {
    req.urlWithoutRegion = '/cars/all/?from=yandex';
    req.router.params = {
        from: 'yandex',
    };

    expect(
        getDesktopUrl(req, { foo: 'bar' }),
    ).toEqual('https://autoru_frontend.base_domain/cars/all/?from=yandex&foo=bar');
});

it('должен удалить geo_id, если req.geoSource != "query"', () => {
    req.urlWithoutRegion = '/cars/all/?from=yandex';
    req.router.params = {
        geo_id: 213,
        from: 'yandex',
    };

    expect(
        getDesktopUrl(req, { foo: 'bar' }),
    ).toEqual('https://autoru_frontend.base_domain/cars/all/?from=yandex&foo=bar');
});

it('должен сохранить geo_id, если req.geoSource = "query"', () => {
    req.geoSource = 'query';
    req.urlWithoutRegion = '/cars/all/?from=yandex';
    req.router.params = {
        geo_id: 213,
        from: 'yandex',
    };

    expect(
        getDesktopUrl(req, { foo: 'bar' }),
    ).toEqual('https://autoru_frontend.base_domain/moskva/cars/all/?from=yandex&foo=bar');
});

it('должен преобразовать урл card/cars в десктопный', () => {
    req.urlWithoutRegion = '/cars/used/sale/nissan/patrol/1102906822-004d5b2a/';
    req.router.params = {
        category: 'cars',
        section: 'used',
        mark: 'nissan',
        model: 'patrol',
        sale_id: '1102906822',
        sale_hash: '004d5b2a',
    };

    expect(
        getDesktopUrl(req),
    ).toEqual('https://autoru_frontend.base_domain/cars/used/sale/nissan/patrol/1102906822-004d5b2a/');
});

it('должен преобразовать урл card/moto в десктопный', () => {
    req.urlWithoutRegion = '/atv/used/sale/stels/atv_850/3702916-12c036d8/';
    req.router.params = {
        category: 'cars',
        section: 'used',
        mark: 'nissan',
        model: 'patrol',
        sale_id: '1102906822',
        sale_hash: '004d5b2a',
    };

    expect(
        getDesktopUrl(req),
    ).toEqual('https://autoru_frontend.base_domain/atv/used/sale/stels/atv_850/3702916-12c036d8/');
});

describe('dealers-listing', () => {
    it('должен преобразовать урл "/moskva/dilery/cars/alfa_romeo/new/" в десктопный', () => {
        req.geoAlias = 'moskva';
        req.geoIds = [ 213 ];
        req.router.params = {
            category: 'cars',
            mark: 'alfa_romeo',
            section: 'new',
            geo_id: 213,
        };
        req.urlWithoutRegion = '/dilery/cars/alfa_romeo/new/';

        expect(
            getDesktopUrl(req, { nomobile: true }),
        ).toEqual('https://autoru_frontend.base_domain/moskva/dilery/cars/alfa_romeo/new/?nomobile=true');
    });
});
