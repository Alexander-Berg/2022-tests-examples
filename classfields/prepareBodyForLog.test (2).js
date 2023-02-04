const prepareBodyForLog = require('./prepareBodyForLog');

const TESTS = [
    {
        body: { password: '123', username: '456' },
        result: JSON.stringify({ password: '********', username: '456' }),
    },
    {
        body: Buffer.from(JSON.stringify({ password: '123', username: '456' })),
        result: JSON.stringify({ password: '********', username: '456' }),
    },
    {
        body: JSON.stringify({ password: '123', username: '456' }),
        result: JSON.stringify({ password: '********', username: '456' }),
    },
    {
        body: JSON.stringify({ new_password: '123', username: '456' }),
        result: JSON.stringify({ new_password: '********', username: '456' }),
    },
    {
        body: JSON.stringify({ current_password: '123', username: '456' }),
        result: JSON.stringify({ current_password: '********', username: '456' }),
    },
    {
        body: JSON.stringify({ user_pass: '123', username: '456' }),
        result: JSON.stringify({ user_pass: '********', username: '456' }),
    },
    {
        body: 'password=123&username=456',
        result: 'password=********&username=456',
    },
    {
        body: 'new_password=123&username=456',
        result: 'new_password=********&username=456',
    },
    {
        body: 'current_password=123&username=456',
        result: 'current_password=********&username=456',
    },
    {
        body: 'user_pass=123&username=456',
        result: 'user_pass=********&username=456',
    },
];

it('не должен обработать тело, если оно Buffer и больше 10 кб', () => {
    const body = Buffer.from('0'.repeat(10 * 1024 + 1), 'utf8');

    const req = {
        body,
        headers: {
            'content-length': body.length,
        },
    };

    expect(prepareBodyForLog(req)).toEqual('[body too long]');
});

it('не должен обработать тело, если оно string и больше 10 кб', () => {
    const body = '0'.repeat(10 * 1024 + 1);

    const req = {
        body,
        headers: {
            'content-length': body.length,
        },
    };

    expect(prepareBodyForLog(req)).toEqual('[body too long]');
});

it('не должен обработать непонятный тип', () => {
    const body = 1;

    const req = {
        body,
        headers: {
            'content-length': 1,
        },
    };

    expect(prepareBodyForLog(req)).toEqual('[unknown body type "number"]');
});

it('не должен обработать пустой body', () => {
    expect(prepareBodyForLog({
        body: null,
    })).toEqual('-');
});

TESTS.forEach((testCase) => {
    it(`должен преобразовать ${ testCase.body } в ${ testCase.result }`, () => {
        const body = testCase.body;

        const req = {
            body,
            headers: {
                'content-length': body.length || Buffer.from(JSON.stringify(body)),
            },
        };

        expect(prepareBodyForLog(req)).toEqual(testCase.result);
    });
});
