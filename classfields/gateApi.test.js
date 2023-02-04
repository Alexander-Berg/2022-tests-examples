/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/cookie', () => ({
    getCsrfToken: jest.fn(() => '_csrf_token_from_cookie'),
}));

const MockDate = require('mockdate');

const GateApiClass = require('auto-core/react/lib/gateApiClass');

const testDate = 1515110400000;

let gateApi;
beforeEach(() => {
    fetch.resetMocks();
    gateApi = new GateApiClass();
    MockDate.set(testDate);
});

afterEach(() => {
    MockDate.reset();
});

it('не должен мутировать переданные параметры', () => {
    gateApi.setConfig({ gids: [ 213 ] });
    fetch.mockResponseOnce(JSON.stringify({}));

    const params = { foo: 'bar' };
    gateApi.getResource('test', params);
    expect(params).toEqual({ foo: 'bar' });
});

describe('_prepareRequestBody', function() {
    beforeEach(() => {
        fetch.mockResponseOnce(JSON.stringify({}));
    });

    it('{param1 : 1, param2 : 2} => param1=1&param2=2', function() {
        gateApi.getResource('test', { a: 1, b: 2 }, { method: 'GET' });

        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/?a=1&b=2', {
            credentials: 'same-origin',
            headers: {
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'GET',
        });
    });

    it('{array : [1, 2]} => array=1&array=2', function() {
        gateApi.getResource('test', { array: [ 1, 2 ] }, { method: 'GET' });

        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/?array=1&array=2', {
            credentials: 'same-origin',
            headers: {
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'GET',
        });
    });

    it('{null_value: null, foo: bar} => foo=bar', function() {
        gateApi.getResource('test', {
            null_value: null,
            foo: 'bar',
        }, { method: 'GET' });

        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/?foo=bar', {
            credentials: 'same-origin',
            headers: {
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'GET',
        });
    });

    it('{undefined_value: undefined, foo: bar} => foo=bar', function() {
        gateApi.getResource('test', {
            undefined_value: undefined,
            foo: 'bar',
        }, { method: 'GET' });

        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/?foo=bar', {
            credentials: 'same-origin',
            headers: {
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'GET',
        });
    });

    it('{empty_value: "", foo: bar} => empty_value=&foo=bar', function() {
        gateApi.getResource('test', {
            empty_value: '',
            foo: 'bar',
        }, { method: 'GET' });

        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/?empty_value=&foo=bar', {
            credentials: 'same-origin',
            headers: {
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'GET',
        });
    });
});

describe('_getError', function() {
    it('error in data', function() {
        expect(gateApi._getError({ error: {} })).toEqual({});
    });

    it('error in response', function() {
        expect(gateApi._getError({ response: { error: {} } })).toEqual({});
    });

    it('valid data', function() {
        expect(gateApi._getError({})).toBeUndefined();
    });
});

describe('добавление geo_id', () => {
    it('должен добавить geo_id, если он есть в конфиге', () => {
        gateApi.setConfig({ gids: [ 213 ] });
        fetch.mockResponseOnce(JSON.stringify({}));
        gateApi.getResource('test', { foo: 'bar' });

        expect(fetch.mock.calls).toHaveLength(1);
        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/', {
            body: JSON.stringify({ foo: 'bar', geo_id: [ 213 ] }),
            credentials: 'same-origin',
            headers: {
                'content-type': 'application/json',
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'POST',
        });
    });

    it('не должен добавить geo_id, если его нет в конфиге', () => {
        fetch.mockResponseOnce(JSON.stringify({}));
        gateApi.getResource('test', { foo: 'bar' });

        expect(fetch.mock.calls).toHaveLength(1);
        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/', {
            body: JSON.stringify({ foo: 'bar' }),
            credentials: 'same-origin',
            headers: {
                'content-type': 'application/json',
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'POST',
        });
    });

    it('не должен добавить geo_id, если он есть в переданных параметрах', () => {
        gateApi.setConfig({ gids: [ 213 ] });
        fetch.mockResponseOnce(JSON.stringify({}));
        gateApi.getResource('test', { foo: 'bar', geo_id: 2 });

        expect(fetch.mock.calls).toHaveLength(1);
        expect(fetch).toHaveBeenCalledWith('/-/ajax/jest/test/', {
            body: JSON.stringify({ foo: 'bar', geo_id: 2 }),
            credentials: 'same-origin',
            headers: {
                'content-type': 'application/json',
                'x-client-app-version': 'development',
                'x-client-date': testDate,
                'x-csrf-token': '_csrf_token_from_cookie',
                'x-requested-with': 'XMLHttpRequest',
                'x-retpath-y': 'http://localhost/',
            },
            mode: 'same-origin',
            method: 'POST',
        });
    });
});

describe('.getResourcePublicApi', () => {
    it('должен зарезолвить промис, если ответ 200 и status === "SUCCESS"', async() => {
        fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS' }), { status: 200 });

        await expect(
            gateApi.getResourcePublicApi('test'),
        ).resolves.toEqual({ status: 'SUCCESS' });
    });

    it('не должен зарезолвить промис, если ответ не 200 и status === "SUCCESS"', async() => {
        fetch.mockResponseOnce(JSON.stringify({ status: 'SUCCESS' }), { status: 404 });

        await expect(
            gateApi.getResourcePublicApi('test'),
        ).rejects.toMatchObject({ status: 404 });
    });

    it('не должен зарезолвить промис, если ответ 200 и status !== "SUCCESS"', async() => {
        fetch.mockResponseOnce(JSON.stringify({ status: 'ERROR' }), { status: 200 });

        await expect(
            gateApi.getResourcePublicApi('test'),
        ).rejects.toMatchObject({ status: 'ERROR' });
    });
});

describe('.geoRedirect()', () => {
    let _originalLocation;
    beforeEach(() => {
        _originalLocation = global.location;
        delete global.location;

        global.location = {
            href: 'https://auto.ru/?foo=bar&bar=baz',
        };
    });

    afterEach(() => {
        global.location = _originalLocation;
    });

    it('сделает редирект и возмет текущий url, если не передали options', () => {
        gateApi.geoRedirect(213);

        expect(location.href).toEqual(
            'https://autoru_frontend.base_domain/georedir/?_csrf_token=_csrf_token_from_cookie&geo=213&url=https%3A%2F%2Fauto.ru%2F%3Ffoo%3Dbar%26bar%3Dbaz',
        );
    });

    it('сделает редирект и возмет url из options', () => {
        gateApi.geoRedirect(213, {
            url: 'https://auto.ru/cars/all/?from=test',
        });

        expect(location.href).toEqual(
            'https://autoru_frontend.base_domain/georedir/?_csrf_token=_csrf_token_from_cookie&geo=213&url=https%3A%2F%2Fauto.ru%2Fcars%2Fall%2F%3Ffrom%3Dtest',
        );
    });
});

describe('captcha', () => {
    let _location;
    beforeEach(() => {
        _location = global.location;
        global.location = {
            origin: 'http://localhost',
            href: 'http://localhost/',
        };
    });

    afterEach(() => {
        global.location = _location;
    });

    it('должен обработать ответ getResource с капчей', () => {
        const response = {
            type: 'captcha',
            captcha: { 'captcha-page': 'http://captcha.com' },
        };
        fetch.mockResponseOnce(JSON.stringify(response), { status: 200 });

        return gateApi.getResource('test')
            .finally(() => expect(location.href).toEqual('http://captcha.com'));
    });

    it('должен обработать ответ getResourcePublicApi с капчей', () => {
        const response = {
            type: 'captcha',
            captcha: { 'captcha-page': 'http://captcha.com' },
        };
        fetch.mockResponseOnce(JSON.stringify(response), { status: 200 });

        return gateApi.getResourcePublicApi('test')
            .catch(jest.fn())
            .finally(() => expect(location.href).toEqual('http://captcha.com'));
    });
});
