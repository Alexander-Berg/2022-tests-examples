/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('js-cookie', () => {
    return {
        get: jest.fn(),
        set: jest.fn(),
    };
});

const MockDate = require('mockdate');
const cookie = require('./cookie');
const jsCookie = require('js-cookie');

let dateIn7Days;
beforeEach(() => {
    MockDate.set('2020-05-20');
    jest.spyOn(cookie, '_exchangeCookieOnServer');
    dateIn7Days = new Date('2020-05-27');
});

afterEach(() => {
    jest.restoreAllMocks();
});

describe('.set()', () => {
    it('должен выставить сессионную куку с path и domain', () => {
        cookie.set('foo', 'bar');
        expect(jsCookie.set).toHaveBeenCalledWith(
            'foo',
            'bar',
            { domain: 'autoru_frontend.cookies_domain', path: '/' },
        );
    });

    it('должен выставить сессионную куку с path, domain и переданным expires', () => {
        cookie.set('foo', 'bar', { expires: 6 });
        expect(jsCookie.set).toHaveBeenCalledWith(
            'foo',
            'bar',
            { domain: 'autoru_frontend.cookies_domain', expires: 6, path: '/' },
        );
    });

    describe('обмен долгих кук', () => {
        it('не должен сделать куку для обмена, если кука сессионная', () => {
            cookie.set('foo', 'bar');
            expect(cookie._exchangeCookieOnServer).not.toHaveBeenCalled();
        });

        it('не должен сделать куку для обмена, если expires меньше 7 дней', () => {
            cookie.set('foo', 'bar', { expires: 6 });
            expect(cookie._exchangeCookieOnServer).not.toHaveBeenCalled();
        });

        it('должен сделать куку для обмена, если expires больше 7 дней', () => {
            cookie.set('foo', 'bar', { expires: 7 });
            expect(cookie._exchangeCookieOnServer).toHaveBeenCalledWith('foo', 7);
            expect(jsCookie.set).toHaveBeenCalledWith(
                '_cookie_exchange',
                `foo=${ dateIn7Days.getTime() }`,
                { domain: 'autoru_frontend.cookies_domain', path: '/' },
            );
        });

        it('должен сделать куку для обмена, если expires больше 7 дней и уже есть другая кука', () => {
            jsCookie.get.mockImplementation((name) => name === '_cookie_exchange' ? 'foo=123' : undefined);

            cookie.set('bar', 'baz', { expires: 7 });
            expect(cookie._exchangeCookieOnServer).toHaveBeenCalledWith('bar', 7);
            expect(cookie._exchangeCookieOnServer).toHaveBeenCalledWith('bar', 7);
            expect(jsCookie.set).toHaveBeenCalledWith(
                '_cookie_exchange',
                `foo=123|bar=${ dateIn7Days.getTime() }`,
                { domain: 'autoru_frontend.cookies_domain', path: '/' },
            );
        });
    });
});
