/**
 * @jest-environment node
 */

const de = require('descript');
const nock = require('nock');

const isRealUserBrowserGuard = require('auto-core/server/descript/guards/isRealUserBrowserGuard');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

jest.mock('auto-core/server/api/ajax', () => {
    return {};
});

const ajaxMethods = require('auto-core/server/api/ajax');
const middlewareFn = require('./ajax');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен ответить 404, если метод не найден', () => {
    const middleware = middlewareFn({ isEndpoint: true });
    req.params = { method: 'unknown' };

    middleware(req, res, () => {});
    expect(res.statusCode).toEqual(404);
    expect(res.end).toHaveBeenCalled();
});

it('должен ничего не сделать и пропустить дальше, если это не конечный обработчик', () => {
    expect.assertions(1);
    const middleware = middlewareFn({ isEndpoint: false });
    req.params = { method: 'unknown' };

    middleware(req, res, () => {
        expect(res.end).not.toHaveBeenCalled();
    });
});

describe('обработка ответа', () => {
    let middleware;
    beforeEach(() => {
        middleware = middlewareFn({ isEndpoint: true });
    });

    it('должен вернуть результат успешного блока', () => {
        return new Promise((resolve) => {
            ajaxMethods.myBlock = de.func({
                block: () => ({ foo: 'bar' }),
            });
            req.params = { method: 'myBlock' };

            middleware(req, res);

            process.nextTick(() => {
                expect(res.send).toHaveBeenCalledWith({ foo: 'bar' });
                resolve();
            });
        });
    });

    it('должен вернуть успешный блок вместе с внутренними ошибками', () => {
        /*
        Это специальный тест на проверку этого поведение.
        Если сам составной блок не упал, то внутри него все равно могут быть упавшие блоки (необязательные блоки, гварды и т.п.).
        Если их не обрабатывать, то они вернуться как есть и попадут в ответ.

        Это ожидаемое поведение.
        */

        return new Promise((resolve) => {
            req.isRobot = true;
            req.params = { method: 'myBlock' };

            ajaxMethods.myBlock = de.object({
                block: {
                    mySubBlock1: de.func({
                        block: () => ({ foo: 'bar' }),
                        options: {
                            before: isRealUserBrowserGuard,
                        },
                    }),
                    mySubBlock2: de.func({
                        block: () => ({ foo: 'bar' }),
                    }),
                },
            });

            middleware(req, res);

            process.nextTick(() => {
                expect(res.send).toHaveBeenCalledTimes(1);
                expect(res.send).toHaveBeenCalledWith({
                    mySubBlock1: {
                        error: { id: 'BLOCK_GUARDED' },
                    },
                    mySubBlock2: { foo: 'bar' },
                });
                resolve();
            });
        });
    });

    it('должен обработать JS-ошибку', () => {
        return new Promise((resolve) => {
            ajaxMethods.myBlock = de.func({
                block: () => {
                    throw new Error('oops');
                },
            });
            req.params = { method: 'myBlock' };

            middleware(req, res);

            process.nextTick(() => {
                expect(res.send).toHaveBeenCalledTimes(1);
                expect(res.send).toHaveBeenCalledWith({
                    error: 'JSERROR',
                    status: 'ERROR',
                });
                resolve();
            });
        });
    });

    it('должен обработать ошибку de.REQUIRED_BLOCK_FAILED', () => {
        return new Promise((resolve) => {
            ajaxMethods.myBlock = de.object({
                block: {
                    mySubBlock: de.func({
                        block: () => {
                            throw new Error('oops');
                        },
                        options: { required: true },
                    }),
                },
            });
            req.params = { method: 'myBlock' };

            middleware(req, res);

            process.nextTick(() => {
                expect(res.send).toHaveBeenCalledTimes(1);
                expect(res.send).toHaveBeenCalledWith({
                    error: 'REQUIRED_BLOCK_FAILED',
                    status: 'ERROR',
                });
                resolve();
            });
        });
    });

    it('должен обработать ошибку de.BLOCK_GUARDED', () => {
        return new Promise((resolve) => {
            req.isRobot = true;
            req.params = { method: 'myBlock' };

            ajaxMethods.myBlock = de.func({
                block: () => ({ foo: 'bar' }),
                options: {
                    before: isRealUserBrowserGuard,
                },
            });

            middleware(req, res);

            process.nextTick(() => {
                expect(res.send).toHaveBeenCalledTimes(1);
                expect(res.send).toHaveBeenCalledWith({
                    error: 'BLOCK_GUARDED',
                    status: 'ERROR',
                });
                resolve();
            });
        });
    });

    it('должен обработать ошибку OFFER_PHONES_POW_UNDEFINED', () => {
        return new Promise((resolve) => {
            ajaxMethods.getPhones = require('auto-core/server/blocks/getPhonesWithProofOfWork');
            req.params = { method: 'getPhones' };

            middleware(req, res);

            process.nextTick(() => {
                expect(res.send).toHaveBeenCalledTimes(1);
                expect(res.send).toHaveBeenCalledWith({
                    error: 'OFFER_PHONES_POW_UNDEFINED',
                    status: 'ERROR',
                });
                resolve();
            });
        });
    });

    it('должен обработать ошибку VIN_REPORT_POW_UNDEFINED', () => {
        return new Promise((resolve) => {
            ajaxMethods.getRichVinReport = require('auto-core/server/blocks/getRichVinReport/getRichVinReportWithProofOfWork');
            req.params = { method: 'getRichVinReport' };

            middleware(req, res);

            process.nextTick(() => {
                expect(res.send).toHaveBeenCalledTimes(1);
                expect(res.send).toHaveBeenCalledWith({
                    error: 'VIN_REPORT_POW_UNDEFINED',
                    status: 'ERROR',
                });
                resolve();
            });
        });
    });

    describe('http-ошибки блоков', () => {
        it('должен обработать, если бекенд ответил ошибкой', () => {
            return new Promise((resolve) => {
                req.params = { method: 'myBlock' };

                nock('http://localhost')
                    .get('/testpath')
                    .reply(
                        500,
                        { error: 'UNKNOWN_ERROR', status: 'ERROR', detailed_error: '[f7e087d0eea83288] Failed request' },
                        { 'x-secret-data': 'secret' },
                    );

                ajaxMethods.myBlock = de.http({
                    block: {
                        hostname: 'localhost',
                        pathname: '/testpath',
                    },
                });

                middleware(req, res);

                setTimeout(() => {
                    expect(res.send).toHaveBeenCalledTimes(1);
                    expect(res.send).toHaveBeenCalledWith({ error: 'UNKNOWN_ERROR', status: 'ERROR', detailed_error: '[f7e087d0eea83288] Failed request' });
                    resolve();
                }, 50);
            });
        });

        it('должен обработать, если бекенд не ответил', () => {
            return new Promise((resolve) => {
                req.params = { method: 'myBlock' };

                nock('http://localhost')
                    .get('/testpath')
                    .delayBody(1000)
                    .reply(
                        200,
                        { foo: 'bar' },
                        { 'x-secret-data': 'secret' },
                    );

                ajaxMethods.myBlock = de.http({
                    block: {
                        hostname: 'localhost',
                        pathname: '/testpath',
                        timeout: 10,
                    },
                });

                middleware(req, res);

                setTimeout(() => {
                    expect(res.send).toHaveBeenCalledTimes(1);
                    expect(res.send).toHaveBeenCalledWith({
                        error: 'REQUEST_TIMEOUT',
                        status: 'ERROR',
                    });
                    resolve();
                }, 50);
            });
        });
    });
});
