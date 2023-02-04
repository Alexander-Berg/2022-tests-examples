const redirectOldNameplateUrl = require('./redirect-old-nameplate-url');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен средиректить со старого шильда на новый', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'BUICK', model: 'GS', nameplate_name: 'gsx' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/buick/gs-gsx/all/';

        redirectOldNameplateUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'NAMEPLATE_OLD_TO_NEW',
                data: {
                    location: '/cars/buick/gs-x/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('ничего не должен делать с новым шильдом', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'BUICK', model: 'GS', nameplate_name: 'x' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/buick/gs-x/all/';

        redirectOldNameplateUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('ничего не должен делать с несуществующим вендором', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'BUICK', model: 'GS', nameplate_name: 'gsy' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/buick/gs-gsy/all/';

        redirectOldNameplateUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
