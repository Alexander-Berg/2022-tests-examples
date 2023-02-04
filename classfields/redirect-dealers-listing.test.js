const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

const redirectDealersListing = require('auto-core/lib/app/middleware/redirect-dealers-listing');

const TESTS = [
    {
        url: '/dilery/cars/all/',
        redirect: false,
    },
    {
        url: '/dilery/cars/audi/a3/111/222/333/new/',
        redirect: '/dilery/cars/audi/new/',
    },
];

let req;
beforeEach(() => {
    req = createHttpReq();
});

TESTS.forEach((test) => {
    if (test.redirect) {
        it(`Редирект листинга диллеров: ${ test.url } => ${ test.redirect }`, () => {
            req.url = test.url;

            const next = (err) => {
                expect(err).toMatchObject({
                    code: 'DEALERS_LISTING_OLD_TO_NEW',
                    data: {
                        location: test.redirect,
                        status: 301,
                    },
                });
            };

            redirectDealersListing(req, {}, next);
        });
    } else {
        it(`Редирект листинга диллеров: ${ test.url } => ${ test.redirect }`, () => {
            req.url = test.url;

            const next = (err) => {
                expect(err).toBeUndefined();
            };

            redirectDealersListing(req, {}, next);
        });
    }
});

it('должен средиректить на урл без марки, если секция не new', () => {
    req.router.route.getName.mockReturnValue('dealers-listing');
    req.router.params = { mark: 'audi', section: 'all', category: 'cars' };

    const next = (err) => {
        expect(err).toMatchObject({
            code: 'DEALERS_LISTING_REMOVE_MARK',
            data: {
                location: '/dilery/cars/all/',
                status: 301,
            },
        });
    };

    redirectDealersListing(req, {}, next);
});

it('не должен средиректить на урл без марки, если секция new', () => {
    req.router.route.getName.mockReturnValue('dealers-listing');
    req.router.params = { mark: 'audi', section: 'new', category: 'cars' };

    const next = (err) => {
        expect(err).toBeUndefined();
    };

    redirectDealersListing(req, {}, next);
});
