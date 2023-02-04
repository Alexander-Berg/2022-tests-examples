const redirect = require('./redirect-from-front-wheel');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;

beforeEach(() => {
    res = createHttpRes();
    req = createHttpReq();
});

it('не должен ничего сделать для обычой ссылки', () => {
    req.url = '/';

    redirect(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('Должен редиректить /cars/all/drive-front_wheel/ на /cars/all/drive-forward_wheel/', () => {
    req.url = '/cars/all/drive-front_wheel/';
    req.router.params = {
        category: 'cars',
        section: 'all',
    };

    redirect(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'FORWARD_WHEEL_FROM_FRONT_WHEEL',
            data: {
                location: '/cars/all/drive-forward_wheel/',
                status: 301,
            },
        });
    });
});
