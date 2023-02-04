const filterErrorResources = require('./filterErrorResources');

it('должен вырезать ресурсы с ошибками', () => {
    const resources = {
        ok: { result: {} },
        fail: { error: {} },
    };

    const expectedResources = {
        ok: { result: {} },
    };

    expect(filterErrorResources(resources)).toEqual(expectedResources);
});
