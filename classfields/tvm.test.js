jest.mock('@vertis/pino', () => {
    const mock = {
        child: () => mock,
        error: jest.fn(() => {}),
    };
    return mock;
});

const TVM = require('./tvm');
const nock = require('nock');
const { logger } = require('./util');

let bodyGood;
let dstlist;
let scope;
let tvm;
beforeEach(() => {
    bodyGood = {
        dst1: { ticket: 'ticker-for-dst1' },
        dst2: { ticket: 'ticker-for-dst1' },
    };
    dstlist = { dst1: 'dst1', dst2: 'dst2' };
    scope = nock('http://tvm-api.yandex.net');
    tvm = new TVM({ hostname: 'tvm-api.yandex.net' }, 'src', 'secret');
});

describe('успешные ответы', () => {
    let result;
    beforeEach(() => {
        result = { dst1: 'ticker-for-dst1', dst2: 'ticker-for-dst1' };
    });

    it('должен отдавать tvm-тикеты, если запрос завершился успешно', () => {
        scope.post('/2/ticket').reply(200, bodyGood);

        return tvm.loadTickets(dstlist)
            .then((response) => {
                expect(response).toEqual(result);
            });
    });

    it('должен отдавать tvm-тикеты, если 9 запросов ответили 500, а 10й завершился успешно', () => {
        scope.post('/2/ticket').times(9).reply(500);
        scope.post('/2/ticket').times(1).reply(200, bodyGood);

        return tvm.loadTickets(dstlist)
            .then((response) => {
                expect(response).toEqual(result);
            });
    });
});

describe('обработка плохого тела ответа', () => {
    it('должен бросить исключение, если получили тикеты не для всех источников', async() => {
        const body = { dst1: { ticket: 'ticker-for-dst1' } };
        scope.post('/2/ticket').reply(200, body);

        await expect(
            tvm.loadTickets(dstlist),
        ).rejects.toThrowError('[luster-tvm] Can\'t find ticket for dst2');

        expect(logger.error).toHaveBeenCalledTimes(1);
        expect(logger.error).toHaveBeenCalledWith({ response: body, destination: 'dst2' }, 'CANT_FIND_TICKET');
    });

    it('должен бросить исключение, если ответ с ошибкой', async() => {
        const body = { error: 'SOME_ERROR' };
        scope.post('/2/ticket').reply(200, body);

        await expect(
            tvm.loadTickets(dstlist),
        ).rejects.toThrowError('[luster-tvm] Error from TVM API: SOME_ERROR');

        expect(logger.error).toHaveBeenCalledTimes(1);
        expect(logger.error).toHaveBeenCalledWith({ response: body }, 'RESPONSE_WITH_ERROR');
    });

    it('должен бросить исключение, если ответ не JSON', async() => {
        const body = 'foo';
        scope.post('/2/ticket').reply(200, 'foo');

        await expect(
            tvm.loadTickets(dstlist),
        ).rejects.toThrowError('Unexpected token o in JSON at position 1');

        expect(logger.error).toHaveBeenCalledTimes(1);
        expect(logger.error).toHaveBeenCalledWith({ response: body }, 'BAD_JSON');
    });
});

describe('обработка плохих ответов', () => {
    it('должен сделать 10 ретраев и reject, если tvm-api 500ит', async() => {
        scope
            .post('/2/ticket')
            .times(10)
            .reply(500);

        await expect(
            tvm.loadTickets(dstlist),
        ).rejects.toThrowError(/Retries limit \{LIMIT:10\} exceeded for request luster-tvm/);

        expect(logger.error).toHaveBeenCalledTimes(1);
        expect(logger.error.mock.calls[0][0]).toMatchObject({
            error: /\[AskerError: Retries limit \{LIMIT:10\} exceeded for request luster-tvm/,
        });
        expect(logger.error.mock.calls[0][1]).toEqual('BAD_RESPONSE');
    });

    it('должен сделать 10 ретраев и reject, если tvm-api не отвечает', async() => {
        scope
            .post('/2/ticket')
            .delay(600)
            .times(10)
            .reply(200, bodyGood);

        await expect(
            tvm.loadTickets(dstlist),
        ).rejects.toThrowError(/Retries limit \{LIMIT:10\} exceeded for request luster-tvm/);

        expect(logger.error).toHaveBeenCalledTimes(1);
        expect(logger.error.mock.calls[0][0]).toMatchObject({
            error: /\[AskerError: Retries limit \{LIMIT:10\} exceeded for request luster-tvm/,
        });
        expect(logger.error.mock.calls[0][1]).toEqual('BAD_RESPONSE');
    }, 10000);
});
