/**
 * @jest-environment node
 */

jest.mock('@vertis/proof-of-work/build/server/validateHash', () => ({ validateHash: jest.fn() }));
jest.mock('../resources/publicApiCard/methods/getPhonesUnsafe', () => {
    const de = require('descript');
    return de.func({
        block: () => 'MOCK_OFFER_PHONES_RESPONSE',
    });
});

const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const { validateHash } = require('@vertis/proof-of-work');
const getPhonesWithProofOfWork = require('./getPhonesWithProofOfWork');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вернуть OFFER_PHONES_POW_UNDEFINED, если не передали вычисленный pow', async() => {
    const params = {
        category: 'CARS',
        offerIdHash: '1101822270-990473b6',
    };

    await expect(
        de.run(getPhonesWithProofOfWork, { context, params }),
    ).rejects.toMatchObject({
        error: {
            id: 'OFFER_PHONES_POW_UNDEFINED',
        },
    });
});

it('должен вернуть OFFER_PHONES_POW_VALIDATION_FAILED, если передали невалидный pow', async() => {
    validateHash.mockImplementation(() => false);
    const params = {
        category: 'CARS',
        offerIdHash: '1101822270-990473b6',
        pow: {},
    };

    await expect(
        de.run(getPhonesWithProofOfWork, { context, params }),
    ).rejects.toMatchObject({
        error: {
            id: 'OFFER_PHONES_POW_VALIDATION_FAILED',
        },
    });
});

it('должен вернуть телефоны, если передали валидный pow', async() => {
    validateHash.mockImplementation(() => true);
    const params = {
        category: 'CARS',
        offerIdHash: '1101822270-990473b6',
        pow: {},
    };

    await expect(
        de.run(getPhonesWithProofOfWork, { context, params }),
    ).resolves.toEqual('MOCK_OFFER_PHONES_RESPONSE');
});
