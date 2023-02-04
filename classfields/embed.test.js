const MockAppErrorBind = jest.fn();

const MockAppError = {
    CODES: { REQUIRED_REDIRECT: 'REQUIRED_REDIRECT' },
    createError: () => ({
        bind: MockAppErrorBind,
    }),
};

jest.mock('auto-core/lib/app_error', () => MockAppError);

jest.mock('www-embed/app/lib/isEmbedAllowed');
const isEmbedAllowed = require('www-embed/app/lib/isEmbedAllowed');
// getBunkerDict.mockImplementation(() => );

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const createMockReq = (options = {}) => ({
    req: {
        ...createHttpReq(),
        hostname: 'TEST_HOSTNAME',
        router: {
            route: {
                getName: () => 'route',
            },
        },
        url: '/some/url/',
        ...options,
    },
    res: createHttpRes(),
    next: jest.fn(),
});

const embedMiddleware = require('./embed');

it('должен прокинуть дальше, если в query есть _debug_embed', async() => {
    const { next, req, res } = createMockReq({
        query: {
            _debug_embed: true,
        },
    });

    await embedMiddleware(req, res, next);

    expect(res.end).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
});

it('должен средиректить на 403, если нет _debug_embed и isEmbedAllowed вернул false', async() => {
    const { next, req, res } = createMockReq({
        headers: {
            referer: 'https://some-unknown-site.com',
        },
    });
    isEmbedAllowed.mockImplementation(() => false);

    await embedMiddleware(req, res, next);

    expect(res.end).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
    expect(MockAppErrorBind).toHaveBeenCalledWith({
        location: 'https://TEST_HOSTNAME/',
        status: 403,
    });
});

it('должен средиректить на 404, если роут не найден', async() => {
    const { next, req, res } = createMockReq({
        headers: {
            referer: 'https://approvedwebsite.com',
        },
        router: undefined,
        fullUrl: 'https://TEST_HOSTNAME/404',
    });
    isEmbedAllowed.mockImplementation(() => true);

    await embedMiddleware(req, res, next);

    expect(res.end).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
    expect(MockAppErrorBind).toHaveBeenCalledWith({
        location: 'https://TEST_HOSTNAME/',
        from: 'https://TEST_HOSTNAME/404',
        reason: 'Page not found',
        requestId: 'jest-request-id',
        status: 404,
    });
});

it('должен выставить заголовки Access-Control-Allow-*, если isEmbedAllowed вернул true', async() => {
    const { next, req, res } = createMockReq({
        headers: {
            referer: 'https://approvedwebsite.com',
        },
    });
    isEmbedAllowed.mockImplementation(() => true);

    await embedMiddleware(req, res, next);

    expect(res.setHeader).toHaveBeenCalledWith('Access-Control-Allow-Origin', 'https://approvedwebsite.com');
    expect(res.setHeader).toHaveBeenCalledWith('Access-Control-Allow-Credentials', 'true');
    expect(res.setHeader).toHaveBeenCalledWith('Access-Control-Allow-Methods', 'GET');
    expect(res.end).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
});
