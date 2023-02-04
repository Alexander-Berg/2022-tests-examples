const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const getExteriorPanoramaFixtures = require('auto-core/server/resources/publicApiPanorama/methods/getExteriorPanorama.fixtures');
const getInteriorPanoramaFixtures = require('auto-core/server/resources/publicApiPanorama/methods/getInteriorPanorama.fixtures');

const controller = require('./panorama-admin');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('вызовет правильный ресурс для типа exterior', () => {
    publicApi
        .get('/1.0/panorama/panorama_id')
        .reply(200, getExteriorPanoramaFixtures.success());

    const params = {
        panorama_type: 'exterior',
        panorama_id: 'panorama_id',
    };

    return de.run(controller, { context, params })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('вызовет правильный ресурс для типа interior', () => {
    publicApi
        .get('/1.0/panorama/interior/panorama_id')
        .reply(200, getInteriorPanoramaFixtures.success());

    const params = {
        panorama_type: 'interior',
        panorama_id: 'panorama_id',
    };

    return de.run(controller, { context, params })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('для неизвестного типа вернет ошибку', async() => {
    const params = {
        panorama_type: 'foo',
        panorama_id: 'panorama_id',
    };

    await expect(
        de.run(controller, { context, params }),
    ).rejects.toMatchObject({ error: { id: 'PAGE_NOT_FOUND', status_code: 404 } });
});

it('если панорама не готова, вернет ошибку', async() => {
    publicApi
        .get('/1.0/panorama/panorama_id')
        .reply(200, getExteriorPanoramaFixtures.in_processing());

    const params = {
        panorama_type: 'exterior',
        panorama_id: 'panorama_id',
    };

    await expect(
        de.run(controller, { context, params }),
    ).rejects.toMatchObject({ error: { id: 'PAGE_NOT_FOUND', status_code: 404 } });
});
