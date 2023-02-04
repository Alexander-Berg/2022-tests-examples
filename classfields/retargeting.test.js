jest.mock('auto-core/lib/marketing', () => ({
    getPage: () => 'IFRAME CONTENT',
}));

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const retargetingMiddleware = require('./retargeting');

const createMockReq = (options = {}) => ({
    req: {
        ...createHttpReq(),
        url: '/retargeting/',
        hostname: 'TEST_HOSTNAME',
        ...options,
    },
    res: createHttpRes(),
    next: jest.fn(),
});

describe('middleware для маркетингового iframe', () => {

    it('не должен матчится на урл', () => {
        const { next, req, res } = createMockReq({
            url: '/any/but/retargeting/',
        });

        retargetingMiddleware(req, res, next);

        expect(res.end).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    it('должен отдать пустую страницу', () => {
        const { next, req, res } = createMockReq({
            query: { noads: '1' },
            isInternalNetwork: true,
        });

        retargetingMiddleware(req, res, next);

        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.end).toHaveBeenCalledWith();
        expect(next).not.toHaveBeenCalled();
    });

    it('должен отдать iframe при запросе с сайтов, относящихся к auto.ru', () => {
        const { next, req, res } = createMockReq({
            headers: {
                referer: 'https://test.avto.ru',
            },
        });

        retargetingMiddleware(req, res, next);

        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.end).toHaveBeenCalledWith('IFRAME CONTENT');
        expect(next).not.toHaveBeenCalled();
    });

    it('должен отдать iframe при запросе с флагом дебага', () => {
        const { next, req, res } = createMockReq({
            query: { _debug: '1' },
        });

        retargetingMiddleware(req, res, next);

        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.end).toHaveBeenCalledWith('IFRAME CONTENT');
        expect(next).not.toHaveBeenCalled();
    });

    it('должен отдать 403 во всех остальных случаях', () => {
        const { next, req, res } = createMockReq({
        });

        retargetingMiddleware(req, res, next);

        expect(res.statusCode).toEqual(403);
        expect(res.end).toHaveBeenCalled();
        expect(next).not.toHaveBeenCalled();
    });

});
