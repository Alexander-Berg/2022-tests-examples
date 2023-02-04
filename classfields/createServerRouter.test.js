const createServerRouter = require('./createServerRouter');

const serverRouter = require('auto-core/router/auto.ru/server/router');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

it('правильно определит реферрер', () => {
    const req = createHttpReq();
    req.headers.referer = 'https://frontend2.natix.dev.avto.ru/cars/used/sale/zenvo/st1/5666724914727954818-bc57f9bb/';
    const res = createHttpRes();

    createServerRouter(serverRouter)(req, res, () => {});
    expect(req.prevRouter.url).toBe('https://frontend2.natix.dev.avto.ru/cars/used/sale/zenvo/st1/5666724914727954818-bc57f9bb/');
});
