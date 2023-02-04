jest.mock('auto-core/lib/core/isMobileApp', () => {
    return () => false;
});

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const redirectsDealers = require('auto-core/lib/app/middleware/redirect-dealer-page');

const TESTS = [
    {
        url: '/diler/cars/all/avtomir_pererva_moskva/gaz/31029/',
        redirect: false,
    },
    {
        url: '/diler/cars/all/avtomir_pererva_moskva/VOLGA_SIBER/31029/',
        redirect: '/diler/cars/all/avtomir_pererva_moskva/volga_siber/31029/',
    },
    {
        url: '/diler-oficialniy/cars/all/avtomir_moskva_hyundai_dmitrovka/1302/',
        redirect: false,
    },
];

let req;
beforeEach(() => {
    req = createHttpReq();
});

TESTS.forEach((test) => {
    if (test.redirect) {
        it(`Редирект диллеров: ${ test.url } => ${ test.redirect }`, () => {
            req.url = test.url;

            const next = (err) => {
                expect(err).toMatchObject({
                    code: 'DEALER_PAGE_BAD_PARAMS',
                    data: {
                        location: test.redirect,
                        status: 301,
                    },
                });
            };

            redirectsDealers(req, {}, next);
        });
    } else {
        it(`Редирект диллеров: ${ test.url } => ${ test.redirect }`, () => {
            req.url = test.url;

            const next = (err) => {
                expect(err).toBeUndefined();
            };

            redirectsDealers(req, {}, next);
        });
    }
});
