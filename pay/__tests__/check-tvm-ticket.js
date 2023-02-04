const { TVM_HEADER, TVM_CONFIG_PATH, SSL_CERT_PATH, ENV } = require('../../constants');
const HTTPStatus = require('http-status');

jest.mock('got');
jest.mock('../../fs');
jest.mock('../../ticket-parser');

describe('check-tvm-ticket middleware', () => {
    const checkTvmTicket = require('../check-tvm-ticket');
    const ticketParser = require('../../ticket-parser');

    jest.mock(TVM_CONFIG_PATH, () => ({
        clientId: 100,
        clientSecret: 'Secret'
    }), { virtual: true });

    test('Skip check in local environment', async () => {
        expect.assertions(2);

        const req = {
            app: {
                get: (key) => ({
                    'envType': ENV.LOCAL
                }[key])
            }
        };
        const res = {};
        const next = jest.fn();

        await checkTvmTicket(req, res, next);

        expect(next).toHaveBeenCalledWith();
        expect(ticketParser.checkTicket).not.toHaveBeenCalled();
    });

    test('Skip check if ticket is not required', async () => {
        expect.assertions(2);

        const req = {
            app: {
                get: (key) => ({
                    'envType': ENV.PRODUCTION,
                    'requireTicket': false
                }[key])
            },
            headers: {}
        };
        const res = {};
        const next = jest.fn();

        await checkTvmTicket(req, res, next);

        expect(next).toHaveBeenCalledWith();
        expect(ticketParser.checkTicket).not.toHaveBeenCalled();
    });

    test('Pass error if ticket is required but not provided', async () => {
        expect.assertions(2);

        const req = {
            app: {
                get: (key) => ({
                    'envType': ENV.PRODUCTION,
                    'requireTicket': true
                }[key])
            },
            headers: {}
        };
        const res = {};
        const next = jest.fn();

        await checkTvmTicket(req, res, next);

        expect(next).toHaveBeenCalledWith(expect.objectContaining({
            status: HTTPStatus.UNAUTHORIZED
        }));
        expect(ticketParser.checkTicket).not.toHaveBeenCalled();
    });

    test('Ticket is checked successfully', async () => {
        expect.assertions(2);

        const req = {
            app: {
                get: (key) => ({
                    'envType': ENV.PRODUCTION,
                    'requireTicket': true
                }[key])
            },
            headers: {
                [TVM_HEADER]: 'some valid ticket'
            }
        };
        const res = {};
        const next = jest.fn();

        await checkTvmTicket(req, res, next);

        await expect(ticketParser.checkTicket).toHaveBeenCalledWith('some valid ticket', 100, 'Secret');
        expect(next).toHaveBeenCalledWith();
    });

    test('Pass error if ticket check failed', async () => {
        expect.assertions(2);

        const req = {
            app: {
                get: (key) => ({
                    'envType': ENV.PRODUCTION,
                    'requireTicket': true
                }[key])
            },
            headers: {
                [TVM_HEADER]: 'some valid ticket'
            }
        };
        const res = {};
        const next = jest.fn();
        ticketParser.checkTicket = jest.fn(() => Promise.reject(new Error('Invalid ticket')));

        await checkTvmTicket(req, res, next);

        await expect(ticketParser.checkTicket).toHaveBeenCalledWith('some valid ticket', 100, 'Secret');
        expect(next).toHaveBeenCalledWith(expect.objectContaining({
            message: 'Invalid ticket'
        }));
    });
});
