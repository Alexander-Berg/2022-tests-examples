const getClientId = require('./getClientId');

it('должен получить client_id из req.query', () => {
    const expectedClientId = 123;

    const params = {};

    const context = {
        req: {
            query: {
                client_id: expectedClientId,
            },
        },
    };

    expect(getClientId({ params, context })).toBe(expectedClientId);
});

it('должен получить client_id из params', () => {
    const expectedClientId = 123;

    const params = { client_id: expectedClientId };

    const context = {};

    expect(getClientId({ params, context })).toBe(expectedClientId);
});

it('должен получить client_id из params с другой структурой', () => {
    const expectedClientId = 123;

    const params = { client_id: { value: expectedClientId } };

    const context = {};

    expect(getClientId({ params, context })).toBe(expectedClientId);
});

it('должен получить client_id из оригинальных параметров роутера', () => {
    const expectedClientId = 123;

    const params = {};

    const context = {
        req: {
            router: {
                params: {
                    client_id: 123,
                },
            },
        },
    };

    expect(getClientId({ params, context })).toBe(expectedClientId);
});

it('client_id из query должен быть самым приоритетным', () => {
    const expectedClientId = 123;
    const wrongClientId = 234;

    const params = { client_id: wrongClientId };

    const context = {
        req: {
            query: {
                client_id: expectedClientId,
            },
        },
    };

    expect(getClientId({ params, context })).toBe(expectedClientId);
});
