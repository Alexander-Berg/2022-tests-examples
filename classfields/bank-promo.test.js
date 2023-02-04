const de = require('descript');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const createContext = require('auto-core/server/descript/createContext');
const redirectWithType = require('auto-core/server/descript/redirectWithType');
const bankPromo = require('./bank-promo');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const RedirectError = require('auto-core/lib/handledErrors/RedirectError');

jest.mock('auto-core/server/descript/redirectWithType');
redirectWithType.mockReturnValue({ cancel: () => {} });

describe('bank-promo', () => {
    const res = createHttpRes();

    publicApi
        .get('/1.0/search/cars')
        .reply(200, {});

    it('не вызывает редирект на /my/credits/ если запрос идет с аппки в вебвью', () => {
        const req = createHttpReq();
        req.webview = true;
        const context = createContext({ req, res });

        return de.run(bankPromo, { context, params: {} })
            .finally(() => {
                expect(
                    redirectWithType,
                ).not.toHaveBeenCalled();
            })
        ;
    });

    it('вызывает редирект на /my/credits/ для тача', () => {
        const req = createHttpReq();
        const context = createContext({ req, res });

        return de.run(bankPromo, { context, params: {} })
            .catch(() => {})
            .finally(() => {
                expect(
                    redirectWithType,
                ).toHaveBeenCalledWith(expect.anything(), {
                    code: RedirectError.CODES.CREDITS_FORCE_APP_UPDATE_TO_LK,
                    location: '/my/credits/',
                    status: 302,
                });
            });
    });
});
