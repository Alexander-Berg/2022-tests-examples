const RedirectError = require('auto-core/lib/handledErrors/RedirectError');
const redirectOldModelUrl = require('./redirect-old-model-url');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен средиректить со старой модели на новую', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'PORSCHE', model: 'CAYENNE_COUPE' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/porsche/cayenne_coupe/all/';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.MODEL_OLD_TO_NEW,
                data: {
                    location: '/cars/porsche/cayenne/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить со старой модели и шильда на новые', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'PORSCHE', model: 'CAYENNE_COUPE', nameplate_name: 'gts' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/porsche/cayenne_coupe-gts/all/';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.MODEL_OLD_TO_NEW,
                data: {
                    location: '/cars/porsche/cayenne-gts_coup/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить со старой модели с шильдом на новую модель без шильда', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'MERCEDES', model: 'VITO', nameplate_name: 'marco_polo' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/mercedes/vito-marco_polo/all/';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.MODEL_OLD_TO_NEW,
                data: {
                    location: '/cars/mercedes/marco_polo/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить, когда в catalog_filter несколько старых моделей', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [
                { mark: 'MERCEDES', model: 'VITO', nameplate_name: 'marco_polo' },
                { mark: 'PORSCHE', model: 'CAYENNE_COUPE' },
            ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/all/?catalog_filter=mark%3DMERCEDES%2Cmodel%3DVITO%3Dnameplate_name=marco_polo&catalog_filter=mark%3DPORSCHE%2Cmodel%3DCAYENNE_COUPE';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.MODEL_OLD_TO_NEW,
                data: {
                    location: '/cars/all/?catalog_filter=mark%3DMERCEDES%2Cmodel%3DMARCO_POLO&catalog_filter=mark%3DPORSCHE%2Cmodel%3DCAYENNE',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен средиректить, когда есть хотя бы одна старая модель', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [
                { mark: 'MERCEDES', model: 'VITO', nameplate_name: 'marco_polo' },
                { mark: 'PORSCHE', model: 'CAYENNE-TURBO_COUP' },
                { mark: 'PORSCHE', model: '924' },
            ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        // eslint-disable-next-line max-len
        req.url = '/cars/all/?catalog_filter=mark%3DMERCEDES%2Cmodel%3DVITO%3Dnameplate_name=marco_polo&catalog_filter=mark%3DPORSCHE%2Cmodel%3DCAYENNE-TURBO_COUP&catalog_filter=mark%3DPORSCHE%2Cmodel%3D924';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toMatchObject({
                code: RedirectError.CODES.MODEL_OLD_TO_NEW,
                data: {
                    // eslint-disable-next-line max-len
                    location: '/cars/all/?catalog_filter=mark%3DMERCEDES%2Cmodel%3DMARCO_POLO&catalog_filter=mark%3DPORSCHE%2Cmodel%3DCAYENNE-TURBO_COUP&catalog_filter=mark%3DPORSCHE%2Cmodel%3D924',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('ничего не должен делать с новой моделью', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'PORSCHE', model: 'CAYENNE-TURBO_COUP' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/porsche/cayenne-turbo_coup/all/';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('ничего не должен делать с обычной моделью', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'PORSCHE', model: '924' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/porsche/924/all/';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('ничего не должен делать, если не совпадает шильд модели, а редирект для модель+шильд', () => {
    return new Promise((done) => {
        req.router.params = {
            category: 'cars',
            catalog_filter: [ { mark: 'MERCEDES', model: 'VITO' } ],
            section: 'all',
        };
        req.router.route.getName = () => 'listing';
        req.url = '/cars/mercedes/vito/all/';

        redirectOldModelUrl(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});
