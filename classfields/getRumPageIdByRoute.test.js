const getRumPageIdByRoute = require('./getRumPageIdByRoute');

it('должен вернуть правильный pageId', () => {
    const route = {
        getData: () => ({
            controller: 'listing',
        }),
    };

    expect(getRumPageIdByRoute(route)).toBe('jest.listing');
});
