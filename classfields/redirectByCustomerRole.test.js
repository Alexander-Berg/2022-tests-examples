const querystring = require('querystring');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

const redirectByCustomerRole = require('./redirectByCustomerRole');

const CUSTOMER_ROLES = require('www-cabinet/data/client/customer-roles');
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

it('должен редиректить с / на dashboard для агенств', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.index);
    context.req.headers['x-forwarded-host'] = 'agency.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.agency },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_DASHBOARD_AGENCY',
            id: 'REDIRECTED',
            location: '/dashboard/',
            status_code: 302,
        },
    });
});

it('должен редиректить с / на dashboard для групп компаний', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.index);
    context.req.headers['x-forwarded-host'] = 'company.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.company },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_DASHBOARD_AGENCY',
            id: 'REDIRECTED',
            location: '/dashboard/',
            status_code: 302,
        },
    });
});

it('должен редиректить клиентов с /dashboard на /', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.dashboardAgency);
    context.req.headers['x-forwarded-host'] = 'cabinet.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.client },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_DASHBOARD',
            id: 'REDIRECTED',
            location: '/',
            status_code: 302,
        },
    });
});

it('должен редиректить менеджеров с /dashboard на /', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.dashboardAgency);
    context.req.headers['x-forwarded-host'] = 'manager.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.manager },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_DASHBOARD',
            id: 'REDIRECTED',
            location: '/',
            status_code: 302,
        },
    });
});

it('должен редиректить клиентов с /clients на /', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.clients);
    context.req.headers['x-forwarded-host'] = 'cabinet.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.client },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_DASHBOARD',
            id: 'REDIRECTED',
            location: '/',
            status_code: 302,
        },
    });
});

it('должен редиректить менеджеров с /clients на /', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.clients);
    context.req.headers['x-forwarded-host'] = 'manager.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.manager },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_DASHBOARD',
            id: 'REDIRECTED',
            location: '/',
            status_code: 302,
        },
    });
});

it('должен редиректить на промку дилеров, если нет валидной customerRole', () => {
    redirectByCustomerRole({
        cancel,
        context,
        result: { result: '' },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_DEALER_PROMO',
            id: 'REDIRECTED',
            location: 'https://autoru_frontend.base_domain/dealer/',
            status_code: 302,
        },
    });
});

it('должен редиректить на правильный домен, если customerRole не совпадает с текущим доменом', () => {
    context.req.headers['x-forwarded-host'] = 'agency.auto.ru';
    context.req.router.params = { foo: 123 };
    context.req.router.route.build.mockImplementation((params) => `/?${ querystring.stringify(params) }`);

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.client },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            code: 'CABINET_TO_CUSTOMER_PROJECT',
            id: 'REDIRECTED',
            location: 'https://cabinet.autoru_frontend.base_domain/?foo=123',
            status_code: 302,
        },
    });
});

it('должен бросать 404 для агенств, если роута нет в агентском кабинете', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.calls);
    context.req.headers['x-forwarded-host'] = 'agency.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.agency },
    });

    expect(cancel.cancel).toHaveBeenCalledWith({
        error: {
            id: 'PAGE_NOT_FOUND',
            status_code: 404,
        },
    });
});

it('не должен обрабатывать кейс с редиректами для агенств, если передан client_id', () => {
    context.req.router.route.getName.mockImplementation(() => ROUTE_NAMES.index);
    context.req.router.params = { client_id: 123 };
    context.req.headers['x-forwarded-host'] = 'agency.auto.ru';

    redirectByCustomerRole({
        cancel,
        context,
        result: { result: CUSTOMER_ROLES.agency },
    });

    expect(cancel.cancel).not.toHaveBeenCalled();
});
