const actions = require('./dealersListing');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const withCallsMock = require('www-cabinet/react/dataDomain/calls/mocks/withCalls.mock');

it('Должен убрать марку из урла, если секция меняется с new на all', () => {
    expect.assertions(1);

    const store = mockStore({
        calls: {
            ...withCallsMock,
        },
        dealersListing: { searchParams: { section: 'new', mark: 'audi' } },
    });

    store.dispatch(
        actions.changeSection('all', (url) => {
            expect(url).not.toMatch('audi');
        }),
    );
});

it('Не должен убрать марку из урла, если секция меняется с new на new', () => {
    expect.assertions(1);

    const store = mockStore({
        calls: {
            ...withCallsMock,
        },
        dealersListing: { searchParams: { section: 'new', mark: 'audi' } },
    });

    store.dispatch(
        actions.changeSection('new', (url) => {
            expect(url).toMatch('audi');
        }),
    );
});
