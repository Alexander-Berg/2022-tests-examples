/**
 * @jest-environment node
 */
const de = require('descript');
const nock = require('nock');

const ERROR_ID = require('auto-core/server/descript/error-id').default;
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const downloadReport = require('./download-report');

const getContext = (isBad) => {
    const req = createHttpReq();
    const res = createHttpRes();

    req.headers['x-forwarded-host'] = 'auto.ru';
    req.headers['x-real-ip'] = '0:0:0:0:0:0:0:0';
    req.headers['user-agent'] = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36';

    if (!isBad) {
        req.headers['referer'] = 'https://auto.ru/history/123-456';
        req.headers.cookie = 'foo=bar';
    }

    return createContext({ req, res });
};

it('не должен запрашивать pdf, если нет offer_id или vin_or_license_plate или order_id', () => {
    return de.run(downloadReport, {
        context: getContext(),
        params: {
            history_entity_id: 'foo',
        },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'NOT_FOUND',
                    status_code: 404,
                },
            });
        },
    );
});

it('если пользователь не авторизован, должен редиректнуть на страницу аутенфикации', async() => {
    nock('https://auto.ru')
        .defaultReplyHeaders({
            'content-type': 'application/json',
        })
        .get('/printer/pdf/?content=history&id=123-456')
        .reply(401, 'Unauthorized');

    await expect(
        de.run(downloadReport, {
            context: getContext(),
            params: {
                history_entity_id: '123-456',
                offer_id: '123-456',
            },
        }),
    ).rejects.toEqual({
        error: {
            code: 'REPORT_NEED_AUTH',
            id: 'REDIRECTED',
            location: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fauto.ru%2Fhistory%2F123-456',
            status_code: 302,
        },
    });
});

describe('если пользователь не покупал отчёт', () => {
    beforeEach(() => {
        nock('https://auto.ru')
            .defaultReplyHeaders({
                'content-type': 'application/json',
            })
            .get('/printer/pdf/?content=history&id=123-456')
            .reply(403, 'Access denied');
    });

    it('должен редиректнуть на страницу отчёта', async() => {
        return de.run(downloadReport, {
            context: getContext(),
            params: {
                history_entity_id: '123-456',
                offer_id: '123-456',
            },
        }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (error) => {
                expect(error).toEqual({
                    error: {
                        code: 'REPORT_NEED_BUY',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/history/123-456/',
                        status_code: 302,
                    },
                });
            },
        );
    });

    it('не должен редиректнуть на страницу отчёта, если это заказ', async() => {
        await expect(
            de.run(downloadReport, {
                context: getContext(),
                params: {
                    history_entity_id: '123-456',
                    order_id: '123-456',
                },
            }),
        ).rejects.toEqual({
            error: {
                id: ERROR_ID.REQUIRED_BLOCK_FAILED,
                status_code: 403,
            },
        });
    });
});

it('если пользователь не авторизован и пришел с плохими хедерами, должен редиректнуть на страницу аутенфикации', async() => {
    nock('https://auto.ru')
        .defaultReplyHeaders({
            'content-type': 'application/json',
        })
        .get('/printer/pdf/?content=history&id=123-456')
        .reply(401, 'Unauthorized');

    await expect(
        de.run(downloadReport, {
            context: getContext(true),
            params: {
                history_entity_id: '123-456',
                offer_id: '123-456',
            },
        }),
    ).rejects.toEqual({
        error: {
            code: 'REPORT_NEED_AUTH',
            id: 'REDIRECTED',
            location: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fauto.ru%2Fdownload-report%2F123-456',
            status_code: 302,
        },
    });
});

it('на ошибку 302, должен редиректнуть на страницу аутенфикации', async() => {
    nock('https://auto.ru')
        .defaultReplyHeaders({
            'content-type': 'application/json',
        })
        .get('/printer/pdf/?content=history&id=123-456')
        .reply(302);

    await expect(
        de.run(downloadReport, {
            context: getContext(true),
            params: {
                history_entity_id: '123-456',
                offer_id: '123-456',
            },
        }),
    ).rejects.toEqual({
        error: {
            code: 'REPORT_NEED_AUTH',
            id: 'REDIRECTED',
            location: 'https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fauto.ru%2Fdownload-report%2F123-456',
            status_code: 302,
        },
    });
});

describe('передача timezone в принтер', () => {
    it('должен передать, если пришла', () => {
        const https = require('https');
        https.request = jest.fn((options, callback) => callback({
            headers: {},
            on: jest.fn(),
        }));

        return de.run(downloadReport, {
            context: getContext(),
            params: {
                vin_or_license_plate: 'WBA5V71060FJ83204',
                timezone: 'Europe/London',
            },
        }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                const expectedPath = '/printer/pdf/?content=history&id=WBA5V71060FJ83204&timezone=Europe/London';
                expect(https.request.mock.calls[0][0].path).toBe(expectedPath);
            },
        );
    });

    it('не должен передать в принтер, если она не пришла', () => {
        const https = require('https');
        https.request = jest.fn((options, callback) => callback({
            headers: {},
            on: jest.fn(),
        }));

        return de.run(downloadReport, {
            context: getContext(),
            params: {
                vin_or_license_plate: 'WBA5V71060FJ83204',
            },
        }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                const expectedPath = '/printer/pdf/?content=history&id=WBA5V71060FJ83204';
                expect(https.request.mock.calls[0][0].path).toBe(expectedPath);
            },
        );
    });
});
