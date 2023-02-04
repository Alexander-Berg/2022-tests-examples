const redirects = require('./redirects');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('не должен ничего делать для урла /sales/trucks/', () => {
    req.url = '/sales/trukcs/';
    redirects(req, res, (error) => {
        expect(error).toBeUndefined();
    });
});

it('должен средиректить для урла /sales/commercial/?from=1', () => {
    req.url = '/sales/commercial/?from=1';
    redirects(req, res, (error) => {
        expect(error).toMatchObject({
            code: 'REQUIRED_REDIRECT',
            data: {
                location: '/sales/trucks/?from=1',
                status: 301,
            },
        });
    });
});
