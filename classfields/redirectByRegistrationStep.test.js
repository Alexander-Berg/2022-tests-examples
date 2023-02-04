const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

const redirectByRegistrationStep = require('./redirectByRegistrationStep');

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

it('должен редиректить на роут с шага регистрации, если есть неподтвержденный шаг', () => {
    redirectByRegistrationStep({
        cancel,
        context,
        result: [
            { allowed: true, confirmed: true, step: 'poi' },
            { allowed: true, confirmed: false, step: 'requisites' },
        ],
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_REGISTRATION_STEP',
            id: 'REDIRECTED',
            location: '/card/details/',
            status_code: 302,
        },
    });
});

it('не должен редиректить, если уже находится на роуте регистрации', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.card);

    redirectByRegistrationStep({
        cancel,
        context,
        result: [
            { allowed: true, confirmed: true, step: 'poi' },
            { allowed: true, confirmed: false, step: 'requisites' },
        ],
    });

    expect(cancel.cancel).not.toHaveBeenCalled();
});
