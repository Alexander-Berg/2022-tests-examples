const RedirectError = require('auto-core/lib/handledErrors/RedirectError');
const redirect = require('./redirect-from-old-bodytype-to-new');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;

beforeEach(() => {
    res = createHttpRes();
    req = createHttpReq();
});

it('не должен ничего сделать для валидного бодитайпа /cars/all/drive-wagon/', () => {
    req.url = '/cars/all/drive-wagon/';
    req.router.params = {
        category: 'cars',
        section: 'all',
        body_type_group: [ 'WAGON' ],
    };

    redirect(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('Должен редиректить /cars/all/body-universal/ на /cars/all/drive-wagon/', () => {
    req.url = '/cars/all/body-universal/';
    req.router.params = {
        category: 'cars',
        section: 'all',
        body_type_group: [ 'UNIVERSAL' ],
    };

    redirect(req, res, (error) => {
        expect(error).toMatchObject({
            code: RedirectError.CODES.BODY_TYPE_OLD_TO_NEW,
            data: {
                location: '/cars/all/body-wagon/',
                status: 301,
            },
        });
    });
});
