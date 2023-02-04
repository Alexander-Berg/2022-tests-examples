import { prepareBodyForLog } from './prepareBodyForLog';

it('не должен обработать тело, если оно Buffer и больше 10 кб', () => {
    const body = Buffer.from('0'.repeat(10 * 1024 + 1), 'utf8');

    const req = {
        body,
        headers: {
            'content-length': body.length,
        },
    };

    expect(prepareBodyForLog(req.body, req.headers)).toBe('[body too long]');
});

it('не должен обработать тело, если оно string и больше 10 кб', () => {
    const body = '0'.repeat(10 * 1024 + 1);

    const req = {
        body,
        headers: {
            'content-length': body.length,
        },
    };

    expect(prepareBodyForLog(req.body, req.headers)).toBe('[body too long]');
});

it('не должен обработать непонятный тип', () => {
    const body = 1;

    const req = {
        body,
        headers: {
            'content-length': 1,
        },
    };

    expect(prepareBodyForLog(req.body, req.headers)).toBe('[unknown body type "number"]');
});


it('должен обработать body в виде plain object', () => {
    const body = { data: 'foo' };

    const req = {
        body,
        headers: {
            'content-length': 1,
        },
    };

    expect(prepareBodyForLog(req.body, req.headers)).toBe('{"data":"foo"}');
});

it('должен обработать body в виде строки', () => {
    const body = '{ data: \'foo\' }';

    const req = {
        body,
        headers: {
            'content-length': 1,
        },
    };

    expect(prepareBodyForLog(req.body, req.headers)).toBe('{ data: \'foo\' }');
});

it('не должен обрабатывать пустой body', () => {
    expect(prepareBodyForLog(null, {})).toBe('-');
});

describe('fields', () => {
    it.each([
        [
            { password: '123', username: '456' },
            JSON.stringify({ password: 'XXXXXXXX', username: '456' }),
        ],
        [
            Buffer.from(JSON.stringify({ password: '123', username: '456' })),
            JSON.stringify({ password: 'XXXXXXXX', username: '456' }),
        ],
        [
            JSON.stringify({ password: '123', username: '456' }),
            JSON.stringify({ password: 'XXXXXXXX', username: '456' }),
        ],
        [
            JSON.stringify({ new_password: '123', username: '456' }),
            JSON.stringify({ new_password: 'XXXXXXXX', username: '456' }),
        ],
        [
            JSON.stringify({ current_password: '123', username: '456' }),
            JSON.stringify({ current_password: 'XXXXXXXX', username: '456' }),
        ],
        [
            JSON.stringify({ user_pass: '123', username: '456' }),
            JSON.stringify({ user_pass: 'XXXXXXXX', username: '456' }),
        ],
        [
            'password=123&username=456',
            'password=XXXXXXXX&username=456',
        ],
        [
            'new_password=123&username=456',
            'new_password=XXXXXXXX&username=456',
        ],
        [
            'current_password=123&username=456',
            'current_password=XXXXXXXX&username=456',
        ],
        [
            'user_pass=123&username=456',
            'user_pass=XXXXXXXX&username=456',
        ],
    ])('должен преобразовать %s в %s', (body: Record<string, unknown> | Buffer | string, result) => {
        const headers = {
            'content-length': String(
                typeof body === 'string' || Buffer.isBuffer(body) ? body.length : Buffer.from(JSON.stringify(body)),
            ),
        };

        expect(prepareBodyForLog(body, headers)).toEqual(result);
    });
});
