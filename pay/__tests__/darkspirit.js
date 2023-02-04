const { TVM_HEADER, TVM_CONFIG_PATH, SSL_CERT_PATH } = require('../../constants');
const DARKSPIRIT_URL = 'http://ds.net';

jest.mock('got');
jest.mock('../../fs');
jest.mock('../../ticket-parser');

describe('darkspirit middleware', () => {
    jest.mock(TVM_CONFIG_PATH, () => ({
        clientId: 100,
        clientSecret: 'Secret',
        services: {
            darkspirit: { clientId: 200 }
        }
    }), { virtual: true });

    const incomingReq = {
        app: {
            get: (key) => ({
                'envType': 'production',
                'config': {
                    'darkspirit': {
                        "api": DARKSPIRIT_URL
                    }
                }
            }[key])
        },
        query: {
            fn: 'ABCDEF1234567890',
            n: '123',
            fpd: '4567'
        }
    };

    const got = require('got');
    const fs = require('../../fs');

    const darkspirit = require('../darkspirit');
    const ticketParser = require('../../ticket-parser');

    test('Requests darkspirit with TVM ticket header set and sets the result to req.body', async () => {
        expect.assertions(4);

        const req = Object.assign({}, incomingReq);
        const res = {};
        const next = jest.fn();
        const ticket = 'ticket';
        const receipt = { id: 100 };
        const cert = 'cert';

        ticketParser.getTicket.mockResolvedValue(ticket);
        fs.readFile.mockResolvedValueOnce(cert);
        got.mockResolvedValueOnce({ body: receipt });
        const options = {
            ca: cert,
            headers: {
                [TVM_HEADER]: ticket
            },
            json: true
        };

        await darkspirit(req, res, next);

        expect(fs.readFile).toHaveBeenCalledWith(SSL_CERT_PATH);
        expect(got).toHaveBeenCalledWith(`${DARKSPIRIT_URL}/fiscal_storages/${req.query.fn}/documents/${req.query.n}/${req.query.fpd}`, options);
        expect(next).toHaveBeenCalledWith();
        expect(req.body).toEqual(receipt);
    });

    test('Darkspirit error happened. Call next with error', async () => {
        expect.assertions(4);

        const req = Object.assign({}, incomingReq);
        const res = {};
        const next = jest.fn();
        const ticket = 'ticket';
        const cert = 'cert';
        const err = new Error('darkspirit error');
        err.statusCode = 400;

        ticketParser.getTicket.mockResolvedValue(ticket);
        fs.readFile.mockResolvedValueOnce(cert);
        got.mockRejectedValueOnce(err);
        const options = {
            ca: cert,
            headers: {
                [TVM_HEADER]: ticket
            },
            json: true
        };

        await darkspirit(req, res, next);

        expect(fs.readFile).toHaveBeenCalledWith(SSL_CERT_PATH);
        expect(got).toHaveBeenCalledWith(`${DARKSPIRIT_URL}/fiscal_storages/${req.query.fn}/documents/${req.query.n}/${req.query.fpd}`, options);
        expect(next).toHaveBeenCalledWith(err);
        expect(err.status).toEqual(400);
    });
});
