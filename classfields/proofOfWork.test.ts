/**
 * @jest-environment node
 */
jest.mock('@vertis/proof-of-work');

import de from 'descript';
import { validateHash } from '@vertis/proof-of-work';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';

import { checkProofOfWork } from './proofOfWork';

const validateHashMock = validateHash as jest.MockedFunction<typeof validateHash>;

let context: TDescriptContext;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('checkProofOfWork', () => {
    it('должен выполнить блок, если PoW есть и он верный', async() => {
        const block = de.func({
            block: () => 'OK',
            options: {
                before: checkProofOfWork({
                    errorIdPoWInvalid: 'POW_VALIDATION_FAILED',
                    errorIdPoWUndefined: 'POW_UNDEFINED',
                    loggerContext: 'logger_context',
                    prometheusLabel: 'prometheus_label',
                }),
            },
        });

        validateHashMock.mockImplementation(() => true);
        const params = {
            pow: {},
        };

        await expect(
            de.run(block, { context, params }),
        ).resolves.toEqual('OK');
    });

    it('должен бросить UNDEFINED ошибку, если нет pow', async() => {
        const block = de.func({
            block: () => 'OK',
            options: {
                before: checkProofOfWork({
                    errorIdPoWInvalid: 'POW_VALIDATION_FAILED',
                    errorIdPoWUndefined: 'POW_UNDEFINED',
                    loggerContext: 'logger_context',
                    prometheusLabel: 'prometheus_label',
                }),
            },
        });

        const params = {};

        await expect(
            de.run(block, { context, params }),
        ).rejects.toEqual({
            error: { id: 'POW_UNDEFINED', status_code: 403 },
        });
    });

    it('должен бросить VALIDATION_FAILED ошибку, если pow неверный', async() => {
        const block = de.func({
            block: () => 'OK',
            options: {
                before: checkProofOfWork({
                    errorIdPoWInvalid: 'POW_VALIDATION_FAILED',
                    errorIdPoWUndefined: 'POW_UNDEFINED',
                    loggerContext: 'logger_context',
                    prometheusLabel: 'prometheus_label',
                }),
            },
        });

        validateHashMock.mockImplementation(() => false);
        const params = {
            pow: {},
        };

        await expect(
            de.run(block, { context, params }),
        ).rejects.toEqual({
            error: { id: 'POW_VALIDATION_FAILED', status_code: 403 },
        });
    });

    it('не должен бросить ошибку, если pow неверный и throwError === false', async() => {
        const block = de.func({
            block: () => 'OK',
            options: {
                before: checkProofOfWork({
                    errorIdPoWInvalid: 'POW_VALIDATION_FAILED',
                    errorIdPoWUndefined: 'POW_UNDEFINED',
                    loggerContext: 'logger_context',
                    prometheusLabel: 'prometheus_label',
                    throwError: false,
                }),
            },
        });

        validateHashMock.mockImplementation(() => false);
        const params = {
            pow: {},
        };

        await expect(
            de.run(block, { context, params }),
        ).resolves.toEqual('OK');
    });
});
