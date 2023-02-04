const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

const redirectByFirstModerated = require('./redirectByFirstModerated');

const ROUTE_NAMES = require('auto-core/router/cabinet.auto.ru/route-names');

let cancel;
let context;
let req;
beforeEach(() => {
    cancel = {
        cancel: jest.fn(),
    };

    req = createHttpReq();

    context = {
        req,
    };
});

it('должен редиректить c "/" на роут "/start", если у клиента не было первичной модерации', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.index);

    redirectByFirstModerated({
        cancel,
        context,
        result: {
            result: {
                first_moderated: '0',
            },
        },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_START',
            id: 'REDIRECTED',
            location: '/start/',
            status_code: 302,
        },
    });
});

it('должен проксировать в query client_id при редиректе', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.index);
    context.req.router.params = { client_id: 20101 };

    redirectByFirstModerated({
        cancel,
        context,
        result: {
            result: {
                first_moderated: '0',
            },
        },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_START',
            id: 'REDIRECTED',
            location: '/start/?client_id=20101',
            status_code: 302,
        },
    });
});
