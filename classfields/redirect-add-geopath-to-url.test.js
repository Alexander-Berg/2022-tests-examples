const { baseDomain: BASE_DOMAIN } = require('auto-core/appConfig');
const RedirectError = require('auto-core/lib/handledErrors/RedirectError');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const redirectAddGeopathToUrl = require('./redirect-add-geopath-to-url');

jest.mock('auto-core/lib/core.js', () => {
    return {
        susanin: {
            findFirst: () => [ {
                getData: () => {
                    return {
                        geoUrl: 213,
                    };
                },
            } ],
        },
    };
});

const TEST_CASES = [
    {
        routeName: 'commercial-listing',
        fullUrl: `https://${ BASE_DOMAIN }/lcv/all/`,
        result: 'https://autoru_frontend.base_domain/moskva/lcv/all/',
    },
    {
        routeName: 'listing',
        fullUrl: `https://${ BASE_DOMAIN }/cars/audi/all/`,
        result: 'https://autoru_frontend.base_domain/moskva/cars/audi/all/',
    },
    {
        routeName: 'moto-listing',
        fullUrl: `https://${ BASE_DOMAIN }/atv/all/`,
        result: 'https://autoru_frontend.base_domain/moskva/atv/all/',
    },
    {
        routeName: 'card-group',
        fullUrl: `https://${ BASE_DOMAIN }/cars/new/group/audi/a3/20785010-20785541/`,
        result: 'https://autoru_frontend.base_domain/moskva/cars/new/group/audi/a3/20785010-20785541/',
    },
];

TEST_CASES.forEach(test => {
    const { fullUrl, result, routeName } = test;
    const req = createHttpReq();
    const res = createHttpRes();

    it(`должен сделать редирект страницы ${ routeName } без гео на урл с гео`, () => {
        return new Promise((done) => {
            req.isRobot = false;
            req.router = {
                route: {
                    getName: () => routeName,
                },
            };
            req.geoAlias = 'moskva';
            req.geoIds = [ 213 ];
            req.fullUrl = fullUrl;

            redirectAddGeopathToUrl(req, res, (error) => {
                expect(error).toMatchObject({
                    code: RedirectError.CODES.ADD_GEOPATH_TO_URL,
                    data: {
                        location: result,
                        status: 302,
                    },
                });
                done();
            });
        });
    });
});
