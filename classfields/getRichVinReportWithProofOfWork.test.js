/**
 * @jest-environment node
 */

jest.mock('@vertis/proof-of-work/build/server/validateHash', () => ({ validateHash: jest.fn() }));
jest.mock('./getRichVinReportUnsafe', () => {
    const de = require('descript');
    return de.func({
        block: () => 'MOCK_VIN_REPORT_RESPONSE',
    });
});

const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const { validateHash } = require('@vertis/proof-of-work');
const getRichVinReportWithProofOfWork = require('./getRichVinReportWithProofOfWork');

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

it('должен вернуть VIN_REPORT_POW_UNDEFINED, если не передали вычисленный pow', async() => {
    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
    };

    await expect(
        de.run(getRichVinReportWithProofOfWork, { context, params }),
    ).rejects.toMatchObject({
        error: {
            id: 'VIN_REPORT_POW_UNDEFINED',
        },
    });
});

it('должен вернуть VIN_REPORT_POW_VALIDATION_FAILED, если передали невалидный pow', async() => {
    validateHash.mockImplementation(() => false);
    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
        pow: {},
    };

    await expect(
        de.run(getRichVinReportWithProofOfWork, { context, params }),
    ).rejects.toMatchObject({
        error: {
            id: 'VIN_REPORT_POW_VALIDATION_FAILED',
        },
    });
});

it('должен вернуть отчет, если передали валидный pow', async() => {
    validateHash.mockImplementation(() => true);
    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
        pow: {},
    };

    await expect(
        de.run(getRichVinReportWithProofOfWork, { context, params }),
    ).resolves.toEqual('MOCK_VIN_REPORT_RESPONSE');
});
