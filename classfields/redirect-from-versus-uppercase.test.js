jest.mock('auto-core/appConfig', () => ({
    baseDomain: 'auto.ru',
}));
jest.mock('auto-core/lib/handledErrors/RedirectError', () => ({
    createErrorFromReq: (req, code, params) => params,
    CODES: {},
}));

const redirect = require('./redirect-from-versus-uppercase');

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

it('должен средиректить, если это адрес в апперкейсе', () => {
    reqMock.url = 'https://auto.ru/compare-cars/FIAT-ALBEA-vs-VOLKSWAGEN-EOS/';
    redirect(reqMock, resMock, nextMock);

    expect(nextMock).toHaveBeenCalledWith({
        location: 'https://auto.ru/compare-cars/fiat-albea-vs-volkswagen-eos/',
        status: 301,
    });
    expect(nextMock).toHaveBeenCalledTimes(1);
});
