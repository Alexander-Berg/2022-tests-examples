'use strict';

jest.mock('auto-core/appConfig', () => {
    return { envProd: true };
});

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const getMetrikaInitParams = require('./getMetrikaInitParams');

let req;
beforeEach(() => {
    req = createHttpReq();
});

describe('webvisor', () => {
    beforeEach(() => {
        req.cookies.autoruuid = '1234567842';
        req.env.cryprox = false;
    });

    it('не должен включить webvisor, если нет куки autoruuid', () => {
        req.cookies.autoruuid = undefined;
        expect(getMetrikaInitParams(req)).toMatchObject({
            webvisor: false,
        });
    });

    it('не должен включить webvisor, если кука autoruuid оканчивается не на 22, 32, 42', () => {
        req.cookies.autoruuid = '12345678';
        expect(getMetrikaInitParams(req)).toMatchObject({
            webvisor: false,
        });
    });

    it('должен включить webvisor, если кука autoruuid оканчивается на 22', () => {
        req.cookies.autoruuid = '1234567822';
        expect(getMetrikaInitParams(req)).toMatchObject({
            webvisor: true,
        });
    });

    it('должен включить webvisor, если кука autoruuid оканчивается на 42', () => {
        req.cookies.autoruuid = '1234567842';
        expect(getMetrikaInitParams(req)).toMatchObject({
            webvisor: true,
        });
    });

    it('не должен включить webvisor, если кука autoruuid оканчивается на 42, но есть cryprox', () => {
        req.env.cryprox = true;
        req.cookies.autoruuid = '1234567842';
        expect(getMetrikaInitParams(req)).toMatchObject({
            webvisor: false,
        });
    });

    describe('колдунщик', () => {
        beforeEach(() => {
            req.cookies.autoruuid = undefined;
            req.env.cryprox = false;
            req.headers.referer = 'https://yandex.ru/search/?lr=12&text=auto.ru&src=suggest_B';
            req.router.params = Object.freeze({ utm_source: 'auto_wizard' });
        });

        it('должен включить webvisor для трафика с колдунщиков', () => {
            expect(getMetrikaInitParams(req)).toMatchObject({
                webvisor: true,
            });
        });

        it('не должен включить webvisor для трафика с колдунщиков, если есть cryprox', () => {
            req.env.cryprox = true;
            expect(getMetrikaInitParams(req)).toMatchObject({
                webvisor: false,
            });
        });

        it('не должен включить webvisor для трафика с колдунщиков, если реферер не яндекс', () => {
            req.headers.referer = 'https://auto.ru/';
            expect(getMetrikaInitParams(req)).toMatchObject({
                webvisor: false,
            });
        });

        it('не должен включить webvisor, если нет referer', () => {
            req.headers.referer = undefined;
            expect(getMetrikaInitParams(req)).toMatchObject({
                webvisor: false,
            });
        });

        it('не должен включить webvisor для трафика с колдунщиков, если utm_source не auto_wizard', () => {
            req.router.params = Object.freeze({ utm_source: 'auto_wizard1' });
            expect(getMetrikaInitParams(req)).toMatchObject({
                webvisor: false,
            });
        });
    });

    it('должен включить вебвизор для страницы кредитного визарда', () => {
        req.cookies.autoruuid = undefined;
        req.router.route.getName = jest.fn();
        req.router.route.getName.mockImplementationOnce(() => 'my-credits-wizard');

        expect(getMetrikaInitParams(req)).toMatchObject({
            webvisor: true,
        });
    });
});

it('должен поставить "-" для dealer_id, если нет айдишника дилера', () => {
    req.cookies.autoruuid = undefined;
    expect(getMetrikaInitParams(req)).toMatchObject({
        params: {
            dealer_id: '-',
        },
    });
});
