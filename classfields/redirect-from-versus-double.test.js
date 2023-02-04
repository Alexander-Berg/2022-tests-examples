jest.mock('auto-core/appConfig', () => ({
    baseDomain: 'auto.ru',
}));
jest.mock('auto-core/lib/handledErrors/RedirectError', () => ({
    createErrorFromReq: (req, code, params) => params,
    CODES: {},
}));
jest.mock('auto-core/lib/core/isMobileApp', () => jest.fn());

const AppError = require('auto-core/lib/app_error');
const isMobileApp = require('auto-core/lib/core/isMobileApp');

const redirect = require('./redirect-from-versus-double');

const nextMock = jest.fn();
const resMock = {};
let reqMock;

beforeEach(() => {
    reqMock = {
        router: {
            route: {
                getData: () => ({ controller: 'versus' }),
            },
        },
    };
});

it('не должен средиректить, если урл не относится к сравнению', () => {
    reqMock.router.route.getData = () => ({ controller: 'other' });
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith();
    expect(nextMock).toHaveBeenCalledTimes(1);
});

it('должен средиректить, если это поисковый дубль версуса', () => {
    reqMock.url = 'https://auto.ru/compare-cars/kia-rio-x_line-vs-hyundai-solaris/';
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith({
        location: 'https://auto.ru/compare-cars/hyundai-solaris-vs-kia-rio-x_line/',
        status: 302,
    });
    expect(nextMock).toHaveBeenCalledTimes(1);
});

it('должен средиректить, если это поисковый дубль версуса, и сохранить query-параметры', () => {
    reqMock.url = 'https://auto.ru/compare-cars/kia-rio-x_line-vs-hyundai-solaris/?foo=1&bar=2';
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith({
        location: 'https://auto.ru/compare-cars/hyundai-solaris-vs-kia-rio-x_line/?foo=1&bar=2',
        status: 302,
    });
    expect(nextMock).toHaveBeenCalledTimes(1);
});

it('не должен средиректить, если это правильный урл версуса (МММ в алфавитном порядке)', () => {
    reqMock.url = 'https://auto.ru/compare-cars/hyundai-solaris-vs-kia-rio-x_line/?foo=1&bar=2';
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith();
    expect(nextMock).toHaveBeenCalledTimes(1);
});

it('должен отдать 404, если кто-то решил сравнить одну и ту же тачку', () => {
    reqMock.url = 'https://auto.ru/compare-cars/hyundai-solaris-vs-hyundai-solaris/?foo=1&bar=2';
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith(AppError.createError(AppError.CODES.PAGE_NOT_FOUND));
    expect(nextMock).toHaveBeenCalledTimes(1);
});

it('должен средиректить, если это поисковый дубль версуса, и подставить правильный домен для тача', () => {
    isMobileApp.mockImplementationOnce(() => true);
    reqMock.url = 'https://m.auto.ru/compare-cars/kia-rio-x_line-vs-hyundai-solaris/';
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith({
        location: 'https://auto.ru/compare-cars/hyundai-solaris-vs-kia-rio-x_line/',
        status: 302,
    });
    expect(nextMock).toHaveBeenCalledTimes(1);
});
