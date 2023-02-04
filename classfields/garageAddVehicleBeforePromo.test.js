const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const block = require('./garageAddVehicleBeforePromo');

const LICENSE_PLATE = 'K145YE196';
const BASE_DOMAIN = 'https://autoru_frontend.base_domain';
const EXTERNAL_URL = 'https://ya.ru';

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('редиректит на промку с параметром vin_or_licence_plate, если не смог добавить тачку', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .post(`/1.0/garage/user/card/identifier/${ LICENSE_PLATE }`)
        .reply(401, { error: 'DAMN, BOY' });

    return de.run(block, { context, params: { vin_or_licence_plate: LICENSE_PLATE, promo: 'fitservice' } }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_LANDING_TO_PROMO',
                    id: 'REDIRECTED',
                    location: `${ BASE_DOMAIN }/promo/garage-fitservice/?redirect=false&vin_or_license_plate=${ LICENSE_PLATE }`,
                    status_code: 302,
                },
            });
        });
});

it('редиректит на на внешнюю промку', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .post(`/1.0/garage/user/card/identifier/${ LICENSE_PLATE }`)
        .reply(401, { error: 'DAMN, BOY' });

    return de.run(block, { context, params: { vin_or_licence_plate: LICENSE_PLATE, promo: 'xxx', url: EXTERNAL_URL } }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'GARAGE_LANDING_TO_EXTERNAL_PROMO',
                    id: 'REDIRECTED',
                    location: EXTERNAL_URL,
                    status_code: 302,
                },
            });
        });
});

it('если по какой-то причине забыли добавить параметр url, то 404', () => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .post(`/1.0/garage/user/card/identifier/${ LICENSE_PLATE }`)
        .reply(401, { error: 'DAMN, BOY' });

    return de.run(block, { context, params: { vin_or_licence_plate: LICENSE_PLATE, promo: 'xxx' } }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NOT_FOUND',
                },
            });
        });
});
