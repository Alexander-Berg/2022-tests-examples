const http = require('http');
const got = require('got');
const getPort = require('get-port');

const HTTPStatus = require('http-status');

describe('Test express server', () => {
    jest.setTimeout(30000);

    let server;

    //will be 8001 or other available port
    let mockPort;

    jest.mock('command-line-args', () => () => ({
        port: mockPort,
        requireTicket: false,
        envType: 'development',
    }));

    const receipt = require('./fixtures/single');
    const receiptWithContext = require('./fixtures/single-with-context');
    const receiptsWithContext = require('./fixtures/multiple-with-context');
    const receipts = require('./fixtures/multiple');
    const uberReceipt = require('./fixtures/uber/single');
    const uberReceipts = require('./fixtures/uber/multiple');
    const marketReceipt = require('./fixtures/single-market');
    const receiptWithoutKktSn = require('./fixtures/single_no_kkt_sn');

    let url;

    const headers = {
        'content-type': 'application/json',
        accept: '*/*',
    };

    beforeAll(async () => {
        mockPort = await getPort({port: 8001});
        url = `http://localhost:${mockPort}`;

        const app = require('../app');
        server = http.createServer(app);

        return new Promise(resolve => {
            server
                .on('listening', () => {
                    console.log(`Listening on  ${mockPort}`);
                    resolve();
                })
                .on('err', err => {
                    console.log(err);
                    server.close(() => process.exit(1));
                })
                .listen(mockPort);
        });
    });

    test('HTML - single receipt', async () => {
        expect.assertions(1);
        const res = await got.post(url, {
            body: JSON.stringify(receipt),
            headers,
        });

        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('HTML - single receipt with context', async () => {
        expect.assertions(1);
        const res = await got.post(url, {
            body: JSON.stringify(receiptWithContext),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('HTML - single Uber receipt', async () => {
        expect.assertions(1);
        const res = await got.post(`http://localhost:${mockPort}`, {
            body: JSON.stringify(uberReceipt),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('HTML - single Market receipt', async () => {
        expect.assertions(1);
        const res = await got.post(`http://localhost:${mockPort}`, {
            body: JSON.stringify(marketReceipt),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('Mobile HTML - single receipt', async () => {
        expect.assertions(1);
        const res = await got.post(`${url}/mobile`, {
            body: JSON.stringify(receipt),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('Mobile HTML - single receipt with context', async () => {
        expect.assertions(1);
        const res = await got.post(`${url}/mobile`, {
            body: JSON.stringify(receiptWithContext),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('Mobile HTML - single Uber receipt', async () => {
        expect.assertions(1);
        const res = await got.post(`${url}/mobile`, {
            body: JSON.stringify(uberReceipt),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('Mobile HTML - multiple receipts', async () => {
        expect.assertions(1);
        const res = await got.post(`${url}/mobile/mult`, {
            body: JSON.stringify(receipts),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('Mobile HTML - multiple Uber receipts', async () => {
        expect.assertions(1);
        const res = await got.post(`${url}/mobile/mult`, {
            body: JSON.stringify(uberReceipts),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('Mobile HTML - multiple receipts with empty receipts list', async () => {
        expect.assertions(1);
        try {
            const res = await got.post(`${url}/mobile/mult`, {
                body: JSON.stringify([]),
                headers,
            });
        } catch (err) {
            // expect(res.body).toMatchSnapshot();
            expect(err.statusCode).toBe(HTTPStatus.BAD_REQUEST);
        }
    });

    test('Mobile HTML - single Market receipt', async () => {
        expect.assertions(1);
        const res = await got.post(`${url}/mobile`, {
            body: JSON.stringify(marketReceipt),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    test('HTML - single receipt without kkt.sn', async () => {
        expect.assertions(1);
        const res = await got.post(`${url}`, {
            body: JSON.stringify(receiptWithoutKktSn),
            headers,
        });
        // expect(res.body).toMatchSnapshot();
        expect(HTTPStatus.OK).toBe(res.statusCode);
    });

    afterAll(() => {
        server.close(() => console.log('Server closed'));
    });
});
