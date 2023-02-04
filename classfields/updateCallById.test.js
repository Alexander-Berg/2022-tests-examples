const updateCallById = require('./updateCallById');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const withCallsMock = require('www-cabinet/react/dataDomain/calls/mocks/withCalls.mock');

it('должен обновлять звонок переданными значениями', () => {
    const store = mockStore({
        calls: {
            ...withCallsMock,
        },
    });

    store.dispatch(
        updateCallById('1234', { result: 'foo' }),
    );

    const newCalls = store.getActions()[0].payload.calls;

    expect(newCalls[1].result).toBe('foo');
    expect(newCalls[0]).toEqual(withCallsMock.callsList.calls[0]);
});
